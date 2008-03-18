package edu.stanford.smi.protegex.storage.rdf.configurable;

import java.io.*;
import java.util.*;

import javax.swing.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.plugin.*;
import edu.stanford.smi.protege.resource.*;
import edu.stanford.smi.protege.storage.clips.*;
import edu.stanford.smi.protege.ui.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;

/**
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class RDFExportProjectPlugin extends AbstractBackendExportPlugin implements RDFFilesPlugin {
    private String clsesFileName;
    private String instancesFileName;
    private String namespace;

    public RDFExportProjectPlugin() {
        super(RDFCBackend.DESCRIPTION);
    }

    public WizardPage createExportWizardPage(ExportWizard wizard, Project project) {
        return new RDFFilesWizardPage(wizard, this);
    }

    public WizardPage createExportToNewFormatWizardPage(ExportWizard wizard, Project project) {
        return new RDFExportToNewFormatWizardPage(wizard, project, this);
    }

    protected void overwriteDomainInformation(Project project, Collection errors) {
        KnowledgeBase kb = project.getKnowledgeBase();
        new RDFCBackend().saveWalk(kb, null, clsesFileName, instancesFileName, null, errors);
    }

    protected void initializeSources(Project project, Collection errors) {
        PropertyList sources = project.getSources();
        String projectName = project.getName();
        project.setKnowledgeBaseFactory(new RDFCBackend());
        RDFCBackend.setSourceFiles(sources, clsesFileName, instancesFileName,
                RDFFilePanel.DEFAULT_NAMESPACE);
    }

    public void setFiles(String clsesFileName, String instancesFileName) {
        this.clsesFileName = clsesFileName;
        this.instancesFileName = instancesFileName;
    }    
    
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void exportProject(Project project) {
        Collection errors = new ArrayList();
        KnowledgeBase kb = project.getKnowledgeBase();
        PropertyList sources = project.getSources();
        RDFCBackend.saveConfiguration(sources, false);
        Namespaces namespaces = new Namespaces(namespace, sources);
        RDFCBackend factory = new RDFCBackend();
        factory.saveWalk(kb, sources, clsesFileName, instancesFileName, namespaces, errors);
        handleErrors(errors);
    }
}