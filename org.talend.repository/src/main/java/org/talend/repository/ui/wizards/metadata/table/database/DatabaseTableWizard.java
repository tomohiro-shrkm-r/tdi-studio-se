// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.repository.ui.wizards.metadata.table.database;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.swt.dialogs.ErrorDialogWidthDetailArea;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.repository.RepositoryElementDelta;
import org.talend.repository.RepositoryPlugin;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.ui.wizards.RepositoryWizard;

/**
 * TableWizard present the TableForm width the MetaDataTable. Use to create a new table (need a connection to a DB).
 */

public class DatabaseTableWizard extends RepositoryWizard implements INewWizard {

    private static Logger log = Logger.getLogger(DatabaseTableWizard.class);

    private SelectorTableWizardPage selectorWizardPage;

    private DatabaseTableWizardPage tableWizardpage;

    private ConnectionItem connectionItem;

    private MetadataTable metadataTable;

    /**
     * DOC ocarbone DatabaseTableWizard constructor comment.
     * 
     * @param workbench
     * @param idNodeDbConnection
     * @param metadataTable
     * @param existingNames
     */
    @SuppressWarnings("unchecked")
    public DatabaseTableWizard(IWorkbench workbench, boolean creation, ConnectionItem connectionItem, MetadataTable metadataTable,
            String[] existingNames) {
        super(workbench, creation);
        this.connectionItem = connectionItem;
        this.metadataTable = metadataTable;
        this.existingNames = existingNames;
        setNeedsProgressMonitor(true);

        // set the repositoryObject, lock and set isRepositoryObjectEditable
        isRepositoryObjectEditable();
        initLockStrategy();
    }

    /**
     * Adding the page to the wizard.
     */

    public void addPages() {
        setWindowTitle(Messages.getString("TableWizard.windowTitle"));

        selectorWizardPage = new SelectorTableWizardPage(connectionItem, metadataTable, isRepositoryObjectEditable());

        tableWizardpage = new DatabaseTableWizardPage(connectionItem, metadataTable, isRepositoryObjectEditable());

        if (creation) {
            selectorWizardPage.setTitle(Messages.getString("TableWizardPage.titleCreate") + " \"" + connectionItem.getProperty().getLabel()
                    + "\"");
            selectorWizardPage.setDescription(Messages.getString("TableWizardPage.descriptionCreate"));
            selectorWizardPage.setPageComplete(true);

            tableWizardpage.setTitle(Messages.getString("TableWizardPage.titleCreate") + " \"" + connectionItem.getProperty().getLabel()
                    + "\"");
            tableWizardpage.setDescription(Messages.getString("TableWizardPage.descriptionCreate"));
            tableWizardpage.setPageComplete(false);

            addPage(selectorWizardPage);
            addPage(tableWizardpage);

        } else {
            tableWizardpage.setTitle(Messages.getString("TableWizardPage.titleUpdate") + " \"" + connectionItem.getProperty().getLabel()
                    + "\"");
            tableWizardpage.setDescription(Messages.getString("TableWizardPage.descriptionUpdate"));
            tableWizardpage.setPageComplete(false);
            addPage(tableWizardpage);
        }

    }

    /**
     * This method determine if the 'Finish' button is enable This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it using wizard as execution context.
     */
    public boolean performFinish() {
        if (tableWizardpage.isPageComplete()) {
            saveMetaData();
            closeLockStrategy();
            RepositoryPlugin.getDefault().getRepositoryService().repositoryChanged(new RepositoryElementDelta(repositoryObject));
            return true;
        } else {
            return false;
        }
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize from it.
     * 
     * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
     */
    public void init(final IWorkbench workbench, final IStructuredSelection selection2) {
        this.selection = selection2;
    }

    /**
     * execute saveMetaData() on TableForm.
     */
    private void saveMetaData() {
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        try {
            factory.save(repositoryObject.getProperty().getItem());
        } catch (PersistenceException e) {
            String detailError = e.toString();
            new ErrorDialogWidthDetailArea(getShell(), PID, Messages.getString("CommonWizard.persistenceException"), detailError);
            log.error(Messages.getString("CommonWizard.persistenceException") + "\n" + detailError);
        }
    }

}
