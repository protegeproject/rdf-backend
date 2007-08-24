package edu.stanford.smi.protegex.storage.rdf.configurable;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;

/**
 * TODO Class Comment
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class RDFFilesWizardPage extends WizardPage {
    private FileField clsesFileField;
    private FileField instancesFileField;
    private JTextField namespaceField;
    private RDFFilesPlugin plugin;
    private Project project;

    public RDFFilesWizardPage(Wizard wizard, RDFFilesPlugin plugin) {
        super("rdf files", wizard);
        this.plugin = plugin;
        createComponents();
        layoutComponents();
        updateSetPageComplete();
    }

    private void createComponents() {
        clsesFileField = new FileField("Classes (.rdfs) File", null, ".rdfs", "Classes File");
        instancesFileField = new FileField("Instances (.rdf) File", null, ".rdf", "Instances File");
        namespaceField = ComponentFactory.createTextField("http://protege.stanford.edu/rdf/");
        
        clsesFileField.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                onClsFieldChanged();
                updateSetPageComplete();
            }
        });
    }
    
    private void onClsFieldChanged() {
        String name = getClsesFileName();
        String instancesName = FileUtilities.replaceExtension(name, ".rdf");
        instancesFileField.setPath(instancesName);
        updateNamespace(name);
    }
    
    private void updateNamespace(String name) {
        String namespace;
        String text = namespaceField.getText();
        int index = text.lastIndexOf('/');
        if (index == -1) {
            namespace = text;
        } else {
            String baseName = FileUtilities.getBaseName(name);
            namespace = text.substring(0, index + 1) + baseName;
        }
        
        namespaceField.setText(namespace);
    }
    
    private void updateSetPageComplete() {
        setPageComplete(getClsesFileName() != null);
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        Box panel = Box.createVerticalBox();
        panel.add(clsesFileField);
        panel.add(instancesFileField);
        panel.add(new LabeledComponent("Namespace", namespaceField));
        add(panel, BorderLayout.NORTH);
    }
    
    public void onFinish() {
        plugin.setFiles(getClsesFileName(), getInstancesFileName());
        plugin.setNamespace(getNamespace());
    }

    private String getClsesFileName() {
        return getPath(clsesFileField, ".rdfs");
    }

    private String getInstancesFileName() {
        return getPath(instancesFileField, ".rdf");
    }
    
    private String getNamespace() {
        return namespaceField.getText();
    }
    
    private String getPath(FileField field, String extension) {
        String path = field.getPath();
        return FileUtilities.ensureExtension(path, extension);
    }
}
