// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.xmlmap;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.talend.core.model.process.IExternalNode;
import org.talend.core.model.process.INode;
import org.talend.core.service.IXmlMapService;
import org.talend.designer.core.model.utils.emf.talendfile.AbstractExternalData;
import org.talend.designer.xmlmap.model.emf.xmlmap.AbstractInOutTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.AbstractNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.Connection;
import org.talend.designer.xmlmap.model.emf.xmlmap.FilterConnection;
import org.talend.designer.xmlmap.model.emf.xmlmap.GlobalMapNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.IConnection;
import org.talend.designer.xmlmap.model.emf.xmlmap.INodeConnection;
import org.talend.designer.xmlmap.model.emf.xmlmap.InputXmlTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.LookupConnection;
import org.talend.designer.xmlmap.model.emf.xmlmap.OutputTreeNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.OutputXmlTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.TreeNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.VarNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.VarTable;
import org.talend.designer.xmlmap.model.emf.xmlmap.XmlMapData;
import org.talend.designer.xmlmap.model.emf.xmlmap.XmlmapFactory;
import org.talend.designer.xmlmap.util.XmlMapConnectionBuilder;

/**
 * DOC talend class global comment. Detailled comment
 */
public class XmlMapService implements IXmlMapService {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.service.IXmlMapService#isXmlMapComponent(org.talend.core.model.process.IExternalNode)
     */
    @Override
    public boolean isXmlMapComponent(IExternalNode node) {
        if (node instanceof XmlMapComponent) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.service.IXmlMapService#checkXMLMapDifferents(org.talend.core.model.process.INode,
     * org.talend.core.model.process.INode)
     */
    @Override
    public boolean checkXMLMapDifferents(INode testNode, INode originalNode) {
        AbstractExternalData oriExternalData = originalNode.getExternalNode().getExternalEmfData();
        AbstractExternalData testExternalData = testNode.getExternalNode().getExternalEmfData();

        if (oriExternalData == null || testExternalData == null) {
            if (oriExternalData != testExternalData) {
                return true;
            }
        }

        if (oriExternalData == null && testExternalData == null) {
            return false;
        }
        if (!(oriExternalData instanceof XmlMapData) || !(testExternalData instanceof XmlMapData)) {
            return false;
        }
        XmlMapData oriXmlData = (XmlMapData) oriExternalData;
        XmlMapData testXmlData = (XmlMapData) testExternalData;

        EList<InputXmlTree> oriInputs = oriXmlData.getInputTrees();
        EList<OutputXmlTree> oriOutputs = oriXmlData.getOutputTrees();
        EList<VarTable> oriVars = oriXmlData.getVarTables();

        EList<InputXmlTree> testInputs = testXmlData.getInputTrees();
        EList<OutputXmlTree> testOutputs = testXmlData.getOutputTrees();
        EList<VarTable> testVars = testXmlData.getVarTables();

        if (oriInputs == null || testInputs == null) {
            if (oriInputs != testInputs) {
                return true;
            }
        }

        if (oriInputs == null && testInputs == null) {
            return false;
        }

        if (oriOutputs == null || testOutputs == null) {
            if (oriOutputs != testOutputs) {
                return true;
            }
        }

        if (oriOutputs == null && testOutputs == null) {
            return false;
        }

        if (oriVars == null || testVars == null) {
            if (oriVars != testVars) {
                return true;
            }
        }

        if (oriVars == null && testVars == null) {
            return false;
        }

        if (oriInputs.size() != testInputs.size()) {
            return true;
        }
        if (oriOutputs.size() != testOutputs.size()) {
            return true;
        }
        if (oriVars.size() != testVars.size()) {
            return true;
        }

        for (InputXmlTree oriInput : oriInputs) {
            String oriName = oriInput.getName();
            InputXmlTree testInput = null;
            for (InputXmlTree input : testInputs) {
                if (input.getName().equals(oriName)) {
                    testInput = input;
                    break;
                }
            }
            if (testInput == null) {
                return true;
            }
            if (oriInput.isActivateExpressionFilter() != testInput.isActivateExpressionFilter()) {
                return true;
            }
            if (oriInput.getExpressionFilter() != testInput.getExpressionFilter()) {
                return true;
            }
            if (oriInput.isMinimized() != testInput.isMinimized()) {
                return true;
            }
            if (oriInput.isActivateCondensedTool() != testInput.isActivateCondensedTool()) {
                return true;
            }
            EList<TreeNode> oriEntrys = oriInput.getNodes();
            EList<TreeNode> testEntrys = testInput.getNodes();
            if (oriEntrys == null && testEntrys != null) {
                if (testEntrys.isEmpty()) {
                    return true;
                }
            }
            if (oriEntrys != null && testEntrys == null) {
                if (oriEntrys.isEmpty()) {
                    return true;
                }
            }
            if (oriEntrys == null || testEntrys == null) {
                continue;
            }
            if (oriEntrys.size() != testEntrys.size()) {
                return true;
            }
            for (TreeNode oriEntry : oriEntrys) {
                String oriEntryName = oriEntry.getName();
                boolean found = false;
                for (TreeNode testEntry : testEntrys) {
                    if (oriEntryName.equals(testEntry.getName())) {
                        found = true;
                        if (oriEntry.getExpression() == null || testEntry.getExpression() == null) {
                            if (oriEntry.getExpression() != testEntry.getExpression()) {
                                return true;
                            }
                        }
                        if (oriEntry.getExpression() == null && testEntry.getExpression() == null) {
                            continue;
                        }
                        if (!oriEntry.getExpression().equals(testEntry.getExpression())) {
                            return true;
                        }
                    }
                }
                if (!found) {
                    return true;
                }
            }
        }

        for (OutputXmlTree oriOutput : oriOutputs) {
            String oriName = oriOutput.getName();
            OutputXmlTree testOutput = null;
            for (OutputXmlTree output : testOutputs) {
                if (output.getName().equals(oriName)) {
                    testOutput = output;
                    break;
                }
            }
            if (testOutput == null) {
                return true;
            }
            if (oriOutput.isActivateExpressionFilter() != testOutput.isActivateExpressionFilter()) {
                return true;
            }
            if (oriOutput.getExpressionFilter() != testOutput.getExpressionFilter()) {
                return true;
            }
            if (oriOutput.isMinimized() != testOutput.isMinimized()) {
                return true;
            }
            if (oriOutput.isActivateCondensedTool() != testOutput.isActivateCondensedTool()) {
                return true;
            }

            EList<OutputTreeNode> oriEntrys = oriOutput.getNodes();
            EList<OutputTreeNode> testEntrys = testOutput.getNodes();
            if (oriEntrys == null && testEntrys != null) {
                if (!testEntrys.isEmpty()) {
                    return true;
                }
            }
            if (oriEntrys != null && testEntrys == null) {
                if (!oriEntrys.isEmpty()) {
                    return true;
                }
            }
            if (oriEntrys == null || testEntrys == null) {
                continue;
            }
            if (oriEntrys.size() != testEntrys.size()) {
                return true;
            }
            for (OutputTreeNode oriEntry : oriEntrys) {
                String oriEntryName = oriEntry.getName();
                boolean found = false;
                for (OutputTreeNode testEntry : testEntrys) {
                    if (oriEntryName.equals(testEntry.getName())) {
                        found = true;
                        if (oriEntry.getExpression() == null || testEntry.getExpression() == null) {
                            if (oriEntry.getExpression() != testEntry.getExpression()) {
                                return true;
                            }
                        }
                        if (oriEntry.getExpression() == null && testEntry.getExpression() == null) {
                            continue;
                        }
                        if (!oriEntry.getExpression().equals(testEntry.getExpression())) {
                            return true;
                        }
                    }
                }
                if (!found) {
                    return true;
                }
            }
        }

        for (VarTable oriVar : oriVars) {
            String oriName = oriVar.getName();
            VarTable testVar = null;
            for (VarTable var : testVars) {
                if (var.getName().equals(oriName)) {
                    testVar = var;
                    break;
                }
            }
            if (testVar == null) {
                return true;
            }
            if (oriVar.isMinimized() != testVar.isMinimized()) {
                return true;
            }
            EList<VarNode> oriEntrys = oriVar.getNodes();
            EList<VarNode> testEntrys = testVar.getNodes();
            if (oriEntrys == null && testEntrys != null) {
                if (!testEntrys.isEmpty()) {
                    return true;
                }
            }
            if (oriEntrys != null && testEntrys == null) {
                if (!oriEntrys.isEmpty()) {
                    return true;
                }
            }
            if (oriEntrys == null || testEntrys == null) {
                continue;
            }
            if (oriEntrys.size() != testEntrys.size()) {
                return true;
            }
            for (VarNode oriEntry : oriEntrys) {
                String oriEntryName = oriEntry.getName();
                boolean found = false;
                for (VarNode testEntry : testEntrys) {
                    if (oriEntryName.equals(testEntry.getName())) {
                        found = true;
                        if (oriEntry.getExpression() == null || testEntry.getExpression() == null) {
                            if (oriEntry.getExpression() != testEntry.getExpression()) {
                                return true;
                            }
                        }
                        if (oriEntry.getExpression() == null && testEntry.getExpression() == null) {
                            continue;
                        }
                        if (!oriEntry.getExpression().equals(testEntry.getExpression())) {
                            return true;
                        }
                    }
                }
                if (!found) {
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.core.service.IXmlMapService#externalEmfDataClone(org.talend.designer.core.model.utils.emf.talendfile
     * .AbstractExternalData)
     */
    @Override
    public AbstractExternalData externalEmfDataClone(AbstractExternalData externalEmfData) {
        if (!(externalEmfData instanceof XmlMapData)) {
            return externalEmfData;
        }
        Map<EObject, EObject> nodeMaps = new HashMap<EObject, EObject>();
        XmlMapData newXmlMapData = XmlmapFactory.eINSTANCE.createXmlMapData();
        XmlMapData xmlMapData = (XmlMapData) externalEmfData;
        EList<InputXmlTree> oriInputs = xmlMapData.getInputTrees();
        EList<OutputXmlTree> oriOutputs = xmlMapData.getOutputTrees();
        EList<VarTable> oriVars = xmlMapData.getVarTables();
        EList<IConnection> oriConns = xmlMapData.getConnections();

        for (IConnection oriConn : oriConns) {
            if (oriConn instanceof INodeConnection) {
                AbstractNode sourceNode = ((INodeConnection) oriConn).getSource();
                AbstractNode targetNode = ((INodeConnection) oriConn).getTarget();
                EObject source = null;
                if (nodeMaps.get(sourceNode) != null) {
                    source = nodeMaps.get(sourceNode);
                } else {
                    source = cloneTreeNode(sourceNode);
                    nodeMaps.put(sourceNode, source);
                }

                EObject target = null;
                if (nodeMaps.get(targetNode) != null) {
                    target = nodeMaps.get(targetNode);
                } else {
                    target = cloneTreeNode(targetNode);
                    nodeMaps.put(targetNode, target);
                }

                if (oriConn instanceof Connection) {
                    XmlMapConnectionBuilder.createConnection((AbstractNode) source, (AbstractNode) target, newXmlMapData);
                } else if (oriConn instanceof LookupConnection) {
                    XmlMapConnectionBuilder.createLookupConnection((TreeNode) source, (TreeNode) target, newXmlMapData);
                }
            } else if (oriConn instanceof FilterConnection) {
                AbstractNode sourceNode = ((FilterConnection) oriConn).getSource();
                AbstractInOutTree targetNode = ((FilterConnection) oriConn).getTarget();

                EObject source = null;
                if (nodeMaps.get(sourceNode) != null) {
                    source = nodeMaps.get(sourceNode);
                } else {
                    source = cloneTreeNode(sourceNode);
                    nodeMaps.put(sourceNode, source);
                }
                EObject target = null;
                if (nodeMaps.get(targetNode) != null) {
                    target = nodeMaps.get(targetNode);
                } else {
                    target = cloneTreeNode(targetNode);
                    nodeMaps.put(targetNode, target);
                }

                XmlMapConnectionBuilder.createFilterConnection((AbstractNode) source, (AbstractInOutTree) target, newXmlMapData);
            }
        }

        for (InputXmlTree inputXml : oriInputs) {
            InputXmlTree newInputXml = null;
            if (nodeMaps.get(inputXml) == null) {
                newInputXml = XmlmapFactory.eINSTANCE.createInputXmlTree();
                newInputXml.setActivateCondensedTool(inputXml.isActivateCondensedTool());
                newInputXml.setActivateExpressionFilter(inputXml.isActivateExpressionFilter());
                newInputXml.setActivateGlobalMap(inputXml.isActivateGlobalMap());
                newInputXml.setExpressionFilter(inputXml.getExpressionFilter());
                newInputXml.setInnerJoin(inputXml.isInnerJoin());
                newInputXml.setLookup(inputXml.isLookup());
                newInputXml.setLookupMode(inputXml.getLookupMode());
                newInputXml.setMatchingMode(inputXml.getMatchingMode());
                newInputXml.setMinimized(inputXml.isMinimized());
                newInputXml.setMultiLoops(inputXml.isMultiLoops());
                newInputXml.setName(inputXml.getName());
                newInputXml.setPersistent(inputXml.isPersistent());
                if (inputXml.getNodes() != null) {
                    for (TreeNode treeNode : inputXml.getNodes()) {
                        EObject obj = nodeMaps.get(treeNode);
                        if (obj != null) {
                            newInputXml.getNodes().add((TreeNode) obj);
                        }
                    }
                }
                if ((inputXml.getFilterIncomingConnections() != null) && !inputXml.getFilterIncomingConnections().isEmpty()) {
                    newInputXml.getFilterIncomingConnections().addAll(inputXml.getFilterIncomingConnections());
                }
                if ((inputXml.getGlobalMapKeysValues() != null) && !inputXml.getGlobalMapKeysValues().isEmpty()) {
                    newInputXml.getGlobalMapKeysValues().addAll(inputXml.getGlobalMapKeysValues());
                }
                newXmlMapData.getInputTrees().add(newInputXml);
                nodeMaps.put(inputXml, newInputXml);
            }
        }

        for (OutputXmlTree outputXml : oriOutputs) {
            OutputXmlTree newOutputXml = null;
            if (nodeMaps.get(outputXml) == null) {
                newOutputXml = XmlmapFactory.eINSTANCE.createOutputXmlTree();
                newOutputXml.setActivateCondensedTool(outputXml.isActivateCondensedTool());
                newOutputXml.setActivateExpressionFilter(outputXml.isActivateExpressionFilter());
                newOutputXml.setAllInOne(outputXml.isAllInOne());
                newOutputXml.setEnableEmptyElement(outputXml.isEnableEmptyElement());
                newOutputXml.setErrorReject(outputXml.isErrorReject());
                newOutputXml.setExpressionFilter(outputXml.getExpressionFilter());
                newOutputXml.setMinimized(outputXml.isMinimized());
                newOutputXml.setMultiLoops(outputXml.isMultiLoops());
                newOutputXml.setName(outputXml.getName());
                newOutputXml.setReject(outputXml.isReject());
                newOutputXml.setRejectInnerJoin(outputXml.isRejectInnerJoin());

                if (outputXml.getNodes() != null) {
                    for (OutputTreeNode treeNode : outputXml.getNodes()) {
                        EObject obj = nodeMaps.get(treeNode);
                        if (obj != null) {
                            newOutputXml.getNodes().add((OutputTreeNode) obj);
                        }
                    }
                }
                if ((outputXml.getFilterIncomingConnections() != null) && !outputXml.getFilterIncomingConnections().isEmpty()) {
                    newOutputXml.getFilterIncomingConnections().addAll(outputXml.getFilterIncomingConnections());
                }
                if ((outputXml.getInputLoopNodesTables() != null) && !outputXml.getInputLoopNodesTables().isEmpty()) {
                    newOutputXml.getInputLoopNodesTables().addAll(outputXml.getInputLoopNodesTables());
                }
                newXmlMapData.getOutputTrees().add(newOutputXml);
                nodeMaps.put(outputXml, newOutputXml);
            }
        }

        for (VarTable varXml : oriVars) {
            VarTable newVarXml = null;
            if (nodeMaps.get(varXml) == null) {
                newVarXml = XmlmapFactory.eINSTANCE.createVarTable();
                newVarXml.setMinimized(varXml.isMinimized());
                newVarXml.setName(varXml.getName());

                if (varXml.getNodes() != null) {
                    for (VarNode treeNode : varXml.getNodes()) {
                        EObject obj = nodeMaps.get(treeNode);
                        if (obj != null) {
                            newVarXml.getNodes().add((VarNode) obj);
                        }
                    }
                }

                newXmlMapData.getVarTables().add(newVarXml);
                nodeMaps.put(varXml, newVarXml);
            }
        }

        return newXmlMapData;
    }

    private EObject cloneTreeNode(EObject node) {
        if (node instanceof VarNode) {
            VarNode oriSource = (VarNode) node;
            VarNode source = XmlmapFactory.eINSTANCE.createVarNode();
            source.setExpression(oriSource.getExpression());
            source.setName(oriSource.getName());
            source.setNullable(oriSource.isNullable());
            source.setType(oriSource.getType());
            return source;
        } else if (node instanceof TreeNode) {
            TreeNode source = null;
            TreeNode oriSource = null;
            if (node instanceof OutputTreeNode) {
                oriSource = (OutputTreeNode) node;
                source = XmlmapFactory.eINSTANCE.createOutputTreeNode();
                ((OutputTreeNode) source).setAggregate(((OutputTreeNode) oriSource).isAggregate());
                ((OutputTreeNode) source).setInputLoopNodesTable(((OutputTreeNode) oriSource).getInputLoopNodesTable());
            } else if (node instanceof GlobalMapNode) {
                oriSource = (GlobalMapNode) node;
                source = XmlmapFactory.eINSTANCE.createGlobalMapNode();
                return source;
            } else {
                oriSource = (TreeNode) node;
                source = XmlmapFactory.eINSTANCE.createTreeNode();
            }
            source.setChoice(oriSource.isChoice());
            source.setDefaultValue(oriSource.getDefaultValue());
            source.setExpression(oriSource.getExpression());
            source.setGroup(oriSource.isGroup());
            source.setKey(oriSource.isKey());
            source.setLoop(oriSource.isLoop());
            source.setMain(oriSource.isMain());
            source.setName(oriSource.getName());
            source.setNodeType(oriSource.getNodeType());
            source.setNullable(oriSource.isNullable());
            source.setOptional(oriSource.isOptional());
            source.setPattern(oriSource.getPattern());
            source.setSubstitution(oriSource.isSubstitution());
            source.setType(oriSource.getType());
            source.setXpath(oriSource.getXpath());
            return source;
        }

        return null;
    }

}
