// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.views.statsandlogs.migration;

import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.emf.common.util.EList;
import org.talend.core.model.migration.AbstractJobMigrationTask;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.properties.ProcessItem;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType;

/**
 * 
 * DOC xye class global comment. Detailled comment
 */
public class UseOracleSIDAsDefaultOracleTypeMigrationTask extends AbstractJobMigrationTask {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.migration.IWorkspaceMigrationTask#getOrder()
     */
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2008, 10, 8, 16, 0, 0);
        return gc.getTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.migration.AbstractJobMigrationTask#executeOnProcess(org.talend.core.model.properties.ProcessItem)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ExecutionResult executeOnProcess(ProcessItem item) {
        EList parameters = item.getProcess().getParameters().getElementParameter();
        for (int i = 0; i < parameters.size(); i++) {
            ElementParameterType parameter = (ElementParameterType) parameters.get(i);
            if ((parameter.getName().equals(EParameterName.DB_TYPE.getName()) || parameter.getName().equals(
                    "DB_TYPE_IMPLICIT_CONTEXT"))
                    && parameter.getField().equals(EParameterFieldType.CLOSED_LIST.getName())) {
                String value = (String) parameter.getValue();
                if (value.equalsIgnoreCase("tOracleOutput")) {
                    parameter.setValue("tOracleOutput_sid"); // Default set to Orace SID(see bug 5315)
                }
            }
        }
        return ExecutionResult.SUCCESS_WITH_ALERT;

    }
}
