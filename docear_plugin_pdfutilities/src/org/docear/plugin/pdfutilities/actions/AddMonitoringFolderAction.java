package org.docear.plugin.pdfutilities.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;

import org.docear.plugin.pdfutilities.features.AnnotationID;
import org.docear.plugin.pdfutilities.features.AnnotationNodeModel;
import org.docear.plugin.pdfutilities.pdf.PdfAnnotationImporter;
import org.docear.plugin.pdfutilities.pdf.PdfFileFilter;
import org.docear.plugin.pdfutilities.util.NodeUtils;
import org.docear.plugin.pdfutilities.util.Tools;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.url.UrlManager;
import org.freeplane.features.url.mindmapmode.MFileManager;
import org.freeplane.n3.nanoxml.XMLParseException;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.attribute.AttributePanelManager;
import org.freeplane.view.swing.map.attribute.AttributeView;
import org.jdesktop.swingworker.SwingWorker;

@EnabledAction( checkOnNodeChange = true )
public class AddMonitoringFolderAction extends AFreeplaneAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AddMonitoringFolderAction(String key) {
		super(key);		
	}

	public void actionPerformed(ActionEvent e) {
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle(TextUtils.getText("AddMonitoringFolderAction_dialog_title")); //$NON-NLS-1$
		int result = fileChooser.showOpenDialog(Controller.getCurrentController().getViewController().getJFrame());
        if(result == JFileChooser.APPROVE_OPTION){
        	URI pdfDir = MLinkController.toLinkTypeDependantURI(Controller.getCurrentController().getMap().getFile(), fileChooser.getSelectedFile());
        	fileChooser.setDialogTitle(TextUtils.getText("AddMonitoringFolderAction_dialog_title_mindmaps")); //$NON-NLS-1$
        	result = fileChooser.showOpenDialog(Controller.getCurrentController().getViewController().getJFrame());
        	if(result == JFileChooser.APPROVE_OPTION){
        		URI mindmapDir = MLinkController.toLinkTypeDependantURI(Controller.getCurrentController().getMap().getFile(), fileChooser.getSelectedFile());
        		NodeModel selected = Controller.getCurrentController().getSelection().getSelected();
        		NodeAttributeTableModel attributes = AttributeController.getController().createAttributeTableModel(selected);
        		AttributeController.getController().performInsertRow(attributes, attributes.getRowCount(), TextUtils.getText("mon_incoming_folder"), pdfDir); //$NON-NLS-1$
        		AttributeController.getController().performInsertRow(attributes, attributes.getRowCount(), "mon_mindmap_folder", mindmapDir);
        		AttributeView attributeView = (((MapView) Controller.getCurrentController().getViewController().getMapView()).getSelected()).getAttributeView();
        		attributeView.setOptimalColumnWidths();
        		
        		this.updateNodesAgainstMonitoringDir(selected, pdfDir, mindmapDir);
        	}   		
        }	
		
	}
	
	private void updateNodesAgainstMonitoringDir(NodeModel target, URI monitoringDir, URI mindmapDir) {
			PdfAnnotationImporter importer = new PdfAnnotationImporter();
			monitoringDir = Tools.getAbsoluteUri(monitoringDir);
			Collection<URI> monitorFiles = Tools.getFilteredFileList(monitoringDir, new PdfFileFilter(), true);
			Map<AnnotationID, Collection<AnnotationNodeModel>> oldAnnotations = new NodeUtils().getOldAnnotationsFromCurrentMap();
			
			System.out.println("");
	}

	private void printNodes(NodeModel node) {
		System.out.println("Node Text:" + node.getText());
		URI link = Tools.getAbsoluteUri(node, node.getMap());
		System.out.println("Node Link:" + link);
		for(NodeModel child : node.getChildren()){
			this.printNodes(child);
		}		
	}

	public void setEnabled(){
		NodeModel selected = Controller.getCurrentController().getSelection().getSelected();
		if(selected == null){
			this.setEnabled(false);
		}
		else{
			this.setEnabled(!NodeUtils.isMonitoringNode(selected));
		}
	}

}
