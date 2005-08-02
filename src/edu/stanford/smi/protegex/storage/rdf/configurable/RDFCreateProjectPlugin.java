package edu.stanford.smi.protegex.storage.rdf.configurable;

import java.io.*;
import java.net.*;
import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.plugin.*;
import edu.stanford.smi.protege.util.*;

/**
 * TODO Class Comment
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class RDFCreateProjectPlugin extends AbstractCreateProjectPlugin implements RDFFilesPlugin {
    private String clsesFileName;
    private String instancesFileName;
    private String namespace;

    public RDFCreateProjectPlugin() {
        super(RDFCBackend.DESCRIPTION);
    }

    protected void importIntoProject(Project project, Collection errors) {
        KnowledgeBase kb = project.getKnowledgeBase();
        RDFCBackend backend = (RDFCBackend) kb.getKnowledgeBaseFactory();
        // This backend will only work if the values are sent in the project
        PropertyList sources = project.getSources();
        backend.setSourceFiles(sources, clsesFileName, instancesFileName, namespace);
        backend.loadKnowledgeBase(kb, sources, errors);
    }

    public boolean canCreateProject(KnowledgeBaseFactory factory, boolean useExistingSources) {
        return factory.getClass() == RDFCBackend.class;
    }

    public WizardPage createCreateProjectWizardPage(CreateProjectWizard wizard, boolean useExistingSources) {
        WizardPage page = null;
        if (useExistingSources) {
            page = new RDFFilesWizardPage(wizard, this);
        }
        return page;
    }

    protected URI getBuildProjectURI() {
        String name = clsesFileName.substring(0, clsesFileName.length() - 5);
        name += ".pprj";
        File file = new File(name);
        return file.toURI();
    }
    
    protected void initializeSources(PropertyList sources) {
        RDFCBackend.setSourceFiles(sources, FileUtilities.getName(clsesFileName), FileUtilities
                .getName(instancesFileName), namespace);
    }

    public void setFiles(String clsesFileName, String instancesFileName) {
        this.clsesFileName = clsesFileName;
        this.instancesFileName = instancesFileName;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

}