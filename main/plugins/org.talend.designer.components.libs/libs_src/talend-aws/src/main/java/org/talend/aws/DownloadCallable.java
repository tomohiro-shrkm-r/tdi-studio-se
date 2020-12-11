package org.talend.aws;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.net.ssl.SSLProtocolException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.annotation.SdkInternalApi;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.internal.FileLocks;
import com.amazonaws.services.s3.internal.ServiceUtils;
import com.amazonaws.services.s3.internal.ServiceUtils.RetryableS3DownloadTask;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;
import com.amazonaws.services.s3.transfer.exception.FileLockException;
import com.amazonaws.util.IOUtils;

@SdkInternalApi
final class DownloadCallable implements Callable<File> {
    private static final Log LOG = LogFactory.getLog(DownloadCallable.class);

    private final AmazonS3 s3;
    private final CountDownLatch latch;
    private final GetObjectRequest req;
    private final boolean resumeExistingDownload;
    private final DownloadImpl download;
    private final File dstfile;
    private final long origStartingByte;
    private final long timeout;
    private final ScheduledExecutorService timedExecutor;
    /** The thread pool in which parts are downloaded downloaded. */
    private final ExecutorService executor;
    private final List<Future<File>> futureFiles;
    private final boolean isDownloadParallel;
    private Integer lastFullyMergedPartNumber;
    private final boolean resumeOnRetry;

    private long expectedFileLength;

    DownloadCallable(AmazonS3 s3, CountDownLatch latch,
            GetObjectRequest req, boolean resumeExistingDownload,
            DownloadImpl download, File dstfile, long origStartingByte,
            long expectedFileLength, long timeout,
            ScheduledExecutorService timedExecutor,
            ExecutorService executor,
            Integer lastFullyDownloadedPartNumber, boolean isDownloadParallel, boolean resumeOnRetry)
    {
        if (s3 == null || latch == null || req == null || dstfile == null || download == null)
            throw new IllegalArgumentException();
        this.s3 = s3;
        this.latch = latch;
        this.req = req;
        this.resumeExistingDownload = resumeExistingDownload;
        this.download = download;
        this.dstfile = dstfile;
        this.origStartingByte = origStartingByte;
        this.expectedFileLength = expectedFileLength;
        this.timeout = timeout;
        this.timedExecutor = timedExecutor;
        this.executor = executor;
        this.futureFiles = new ArrayList<Future<File>>();
        this.lastFullyMergedPartNumber = lastFullyDownloadedPartNumber;
        this.isDownloadParallel = isDownloadParallel;
        this.resumeOnRetry = resumeOnRetry;
    }

    /**
     * This method must return a non-null object, or else the existing
     * implementation in {@link AbstractTransfer#waitForCompletion()}
     * would block forever.
     *
     * @return the downloaded file
     */
    @Override
    public File call() throws Exception {
        try {
            latch.await();

            if (isTimeoutEnabled()) {
                timedExecutor.schedule(new Runnable() {
                    public void run() {
                        try {
                            if (download.getState() != TransferState.Completed) {
                                download.abort();
                            }
                        } catch(Exception e) {
                            throw new SdkClientException(
                                    "Unable to abort download after timeout", e);
                        }
                    }
                }, timeout, TimeUnit.MILLISECONDS);
            }

            download.setState(TransferState.InProgress);
            ServiceUtils.createParentDirectoryIfNecessary(dstfile);

            if (isDownloadParallel) {
                downloadInParallel(ServiceUtils.getPartCount(req, s3));
            } else {
                S3Object s3Object = retryableDownloadS3ObjectToFile(dstfile,
                        new DownloadTaskImpl(s3, download, req));
                updateDownloadStatus(s3Object);
            }
            return dstfile;
        } catch (Throwable t) {
            // Cancel all the futures
            for (Future<File> f : futureFiles) {
                f.cancel(true);
            }
            // Downloads aren't allowed to move from canceled to failed
            if (download.getState() != TransferState.Canceled) {
                download.setState(TransferState.Failed);
            }
            if (t instanceof Exception)
                throw (Exception) t;
            else
                throw (Error) t;
        }
    }

    /**
     * Takes the result from serial download,
     * updates the transfer state and monitor in downloadImpl object
     * based on the result.
     */
    private void updateDownloadStatus(S3Object result) {
        if (result == null) {
            download.setState(TransferState.Canceled);
            download.setMonitor(new DownloadMonitor(download, null));
        } else {
            download.setState(TransferState.Completed);
        }
    }

    /**
     * Downloads each part of the object into a separate file synchronously and
     * combines all the files into a single file.
     */
    private void downloadInParallel(int partCount) throws Exception {
        if (lastFullyMergedPartNumber == null) {
            lastFullyMergedPartNumber = 0;
        }

        for (int i = lastFullyMergedPartNumber + 1; i <= partCount; i++) {
            GetObjectRequest getPartRequest = new GetObjectRequest(req.getBucketName(), req.getKey(),
                    req.getVersionId()).withUnmodifiedSinceConstraint(req.getUnmodifiedSinceConstraint())
                            .withModifiedSinceConstraint(req.getModifiedSinceConstraint())
                            .withResponseHeaders(req.getResponseHeaders()).withSSECustomerKey(req.getSSECustomerKey())
                            .withGeneralProgressListener(req.getGeneralProgressListener());

            getPartRequest.setMatchingETagConstraints(req.getMatchingETagConstraints());
            getPartRequest.setNonmatchingETagConstraints(req.getNonmatchingETagConstraints());
            getPartRequest.setRequesterPays(req.isRequesterPays());

            futureFiles.add(
                    executor.submit(new DownloadPartCallable(s3, getPartRequest.withPartNumber(i), dstfile)));
        }

        truncateDestinationFileIfNecessary();
        Future<File> future = executor.submit(new CompleteMultipartDownload(futureFiles, dstfile, download, ++lastFullyMergedPartNumber));
        ((DownloadMonitor) download.getMonitor()).setFuture(future);
    }

    /**
     * If only partial part object is merged into the dstFile(due to pause
     * operation), adjust the file length so that the part starts writing from
     * the correct position.
     */
    private void truncateDestinationFileIfNecessary() {
        RandomAccessFile raf = null;
        if (!FileLocks.lock(dstfile)) {
            throw new FileLockException("Fail to lock " + dstfile);
        }

        try {
            raf = new RandomAccessFile(dstfile, "rw");
            if (lastFullyMergedPartNumber == 0) {
                raf.setLength(0);
            } else {
                long lastByte = ServiceUtils.getLastByteInPart(s3, req, lastFullyMergedPartNumber);
                if (dstfile.length() < lastByte) {
                    throw new SdkClientException(
                            "File " + dstfile.getAbsolutePath() + " has been modified since last pause.");
                }
                raf.setLength(lastByte + 1);
                download.getProgress().updateProgress(lastByte + 1);
            }
        } catch (Exception e) {
            throw new SdkClientException("Unable to append part file to dstfile " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(raf, LOG);
            FileLocks.unlock(dstfile);
        }
    }

    /**
     * This method is called only if it is a resumed download.
     *
     * Adjust the range of the get request, and the expected (ie current) file
     * length of the destination file to append to.
     */
    private void adjustRequest(GetObjectRequest req) {
        long[] range = req.getRange();
        long lastByte = range[1];
        long totalBytesToDownload = lastByte - this.origStartingByte + 1;

        if (dstfile.exists()) {
            if (!FileLocks.lock(dstfile)) {
                throw new FileLockException("Fail to lock " + dstfile
                        + " for range adjustment");
            }
            try {
                expectedFileLength = dstfile.length();
                long startingByte = this.origStartingByte + expectedFileLength;
                LOG.info("Adjusting request range from " + Arrays.toString(range)
                        + " to "
                        + Arrays.toString(new long[] { startingByte, lastByte })
                        + " for file " + dstfile);
                req.setRange(startingByte, lastByte);
                totalBytesToDownload = lastByte - startingByte + 1;
            } finally {
                FileLocks.unlock(dstfile);
            }
        }

        if (totalBytesToDownload < 0) {
            throw new IllegalArgumentException(
                "Unable to determine the range for download operation. lastByte="
                        + lastByte + ", origStartingByte=" + origStartingByte
                        + ", expectedFileLength=" + expectedFileLength
                        + ", totalBytesToDownload=" + totalBytesToDownload);
        }
    }


    private S3Object retryableDownloadS3ObjectToFile(File file,
            RetryableS3DownloadTask retryableS3DownloadTask) {
        boolean hasRetried = false;
        S3Object s3Object;
        for (;;) {
            final boolean appendData = resumeExistingDownload || (resumeOnRetry && hasRetried);
            if (appendData && hasRetried) {
                // Need to adjust the get range or else we risk corrupting the downloaded file
                adjustRequest(req);
            }
            s3Object = retryableS3DownloadTask.getS3ObjectStream();
            if (s3Object == null)
                return null;
            try {
                if (testing && resumeExistingDownload && !hasRetried) {
                    throw new SdkClientException("testing");
                }
                ServiceUtils.downloadToFile(s3Object, file,
                        retryableS3DownloadTask.needIntegrityCheck(),
                        appendData, expectedFileLength);
                return s3Object;
            } catch (AmazonClientException ace) {
                if (!ace.isRetryable())
                    throw ace;
                // Determine whether an immediate retry is needed according to the captured SdkClientException.
                // (There are three cases when downloadObjectToFile() throws SdkClientException:
                //      1) SocketException or SSLProtocolException when writing to disk (e.g. when user aborts the download)
                //      2) Other IOException when writing to disk
                //      3) MD5 hashes don't match
                // For 1) If SocketException is the result of the client side resetting the connection, this is retried
                // Cases 2) and 3) will always be retried
                final Throwable cause = ace.getCause();
                if ((cause instanceof SocketException && !cause.getMessage().equals("Connection reset"))
                    || (cause instanceof SSLProtocolException)) {
                    throw ace;
                } else {
                    if (hasRetried)
                        throw ace;
                    else {
                        LOG.info("Retry the download of object " + s3Object.getKey() + " (bucket " + s3Object.getBucketName() + ")", ace);
                        hasRetried = true;
                    }
                }
            } finally {
                s3Object.getObjectContent().abort();
            }
        }
    }

    private boolean isTimeoutEnabled() {
        return timeout > 0;
    }

    private static boolean testing;
    /**
     * Used for testing purpose only.
     */
    static void setTesting(boolean b) {
        testing = b;
    }
}
