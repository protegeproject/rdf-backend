package edu.stanford.smi.protegex.storage.rdf.configurable;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.plugin.*;
import edu.stanford.smi.protege.ui.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;

/**
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class RDFExportPlugin implements ExportPlugin {

    public String getName() {
        return "RDF Schema";
    }

    public void handleExportRequest(Project project) {
        RDFFilePanel panel = new RDFFilePanel();
        int rval = ModalDialog.showDialog(null, panel, "RDF Files to Export", ModalDialog.MODE_OK_CANCEL);
        if (rval == ModalDialog.OPTION_OK) {
            String classesFileName = panel.getClsesFileName();
            String instancesFileName = panel.getInstancesFileName();
            String namespace = panel.getNamespace();
            WaitCursor cursor = new WaitCursor(ProjectManager.getProjectManager().getMainPanel());
            try {
                exportProject(project, classesFileName, instancesFileName, namespace);
            } finally {
                cursor.hide();
            }
        }
    }

    private void exportProject(Project project, String clsesFileName, String instancesFileName, String namespace) {
        Collection errors = new ArrayList();
        KnowledgeBase kb = project.getKnowledgeBase();
        PropertyList sources = project.getSources();
        RDFCBackend.saveConfiguration(sources, false);
        Namespaces namespaces = new Namespaces(namespace, sources);
        RDFCBackend factory = new RDFCBackend();
        factory.saveWalk(kb, sources, clsesFileName, instancesFileName, namespaces, errors);
        handleErrors(errors);
    }

    private void handleErrors(Collection errors) {
        if (!errors.isEmpty()) {
            Log.error("Errors!", this, "handleErrors", errors);
        }
    }

    public static boolean isSuitable(Project prj) {
    	if (prj == null)
    		return false;
    	KnowledgeBaseFactory factory = prj.getKnowledgeBaseFactory();
    	
    	//This is the case of a multi-client
    	if (factory == null) {
    		return false;
    	}
    	
        String factoryName = factory.getClass().getName();
        return factoryName.indexOf(".owl.") == -1;
      // return (prj != null && prj.getKnowledgeBaseFactory() instanceof ClipsKnowledgeBaseFactory);
    }
    
    public void dispose() {
        // do nothing
    }

}
