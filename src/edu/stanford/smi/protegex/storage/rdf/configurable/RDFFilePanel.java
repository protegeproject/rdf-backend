package edu.stanford.smi.protegex.storage.rdf.configurable;

import java.awt.*;

import javax.swing.*;

import edu.stanford.smi.protege.util.*;

/**
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class RDFFilePanel extends JComponent {
    private static final long serialVersionUID = -1414681288737331218L;

    public static final String DEFAULT_NAMESPACE = "http://protege.stanford.edu/rdf";

    private FileField clsesFileField;
    private FileField instancesFileField;
    private JTextField namespaceField;

    public RDFFilePanel() {
        createComponents();
        layoutComponents();
    }
    private void createComponents() {
        clsesFileField = new FileField("RDF Schema (classes) File", null, ".rdfs", "RDFS File");
        instancesFileField = new FileField("RDF (instances) File", null, ".rdf", "RDF File");
        namespaceField = ComponentFactory.createTextField();
        namespaceField.setText(DEFAULT_NAMESPACE);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(3, 0));
        panel.add(clsesFileField);
        panel.add(instancesFileField);
        panel.add(new LabeledComponent("Namespace", namespaceField));
        add(panel, BorderLayout.NORTH);
    }

    public String getClsesFileName() {
        return clsesFileField.getPath();
    }

    public String getInstancesFileName() {
        return instancesFileField.getPath();
    }

    public String getNamespace() {
        return namespaceField.getText();
    }
}
