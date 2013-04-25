package edu.stanford.smi.protegex.storage.rdf.configurable;

import java.awt.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;

/**
 * TODO Class Comment
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class RDFExportToNewFormatWizardPage extends WizardPage {
    private static final long serialVersionUID = 4458927044894069943L;
    private FileField projectFileField;
    private JTextField clsesFileField;
    private JTextField instancesFileField;
    private JTextField namespaceField;
    private RDFExportProjectPlugin plugin;

    public RDFExportToNewFormatWizardPage(Wizard wizard, Project project, RDFExportProjectPlugin plugin) {
        super("rdf export", wizard);
        this.plugin = plugin;
        createComponents(project.getProjectFile());
        layoutComponents();
        updateSetPageComplete();
    }

    private void createComponents(File projectPath) {
        projectFileField = new FileField("Project (.pprj) File", null, ".pprj", "Project File");
        clsesFileField = ComponentFactory.createTextField();
        clsesFileField.setEnabled(false);
        instancesFileField = ComponentFactory.createTextField();
        instancesFileField.setEnabled(false);
        namespaceField = ComponentFactory.createTextField("http://protege.stanford.edu/rdf");
        
        projectFileField.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                onProjectFieldChanged();
                updateSetPageComplete();
            }
        });
    }
    
    private void onProjectFieldChanged() {
        String name = getProjectFileName();
        replaceName(instancesFileField, ".rdf", name);
        replaceName(clsesFileField, ".rdfs", name);
    }
    
    private void replaceNamespaceName() {
        String s = namespaceField.getText();
    }
    
    private void replaceName(JTextField field, String extension, String baseName) {
        String name = FileUtilities.replaceExtension(baseName, extension);
        name = new File(name).getName();
        field.setText(name);
    }
    
    private void updateSetPageComplete() {
        setPageComplete(getProjectFileName() != null);
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        Box panel = Box.createVerticalBox();
        panel.add(projectFileField);
        panel.add(new LabeledComponent("Classes", clsesFileField));
        panel.add(new LabeledComponent("Instances", instancesFileField));
        panel.add(new LabeledComponent("Namespace", namespaceField));
        add(panel, BorderLayout.NORTH);
    }
    
    public void onFinish() {
        String projectName = getProjectFileName();
        String clsesFileName = clsesFileField.getText();
        String instancesFileName = instancesFileField.getText();
        String namespace = namespaceField.getText();
        plugin.setNewProjectPath(projectName);
        plugin.setFiles(clsesFileName, instancesFileName);
        plugin.setNamespace(namespace);
    }

    private String getProjectFileName() {
        return getPath(projectFileField, ".pprj");
    }

    private String getPath(FileField field, String extension) {
        String path = field.getPath();
        return FileUtilities.ensureExtension(path, extension);
    }
}
