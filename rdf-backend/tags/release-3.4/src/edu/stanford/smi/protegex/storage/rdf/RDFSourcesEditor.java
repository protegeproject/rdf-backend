package edu.stanford.smi.protegex.storage.rdf;

import java.awt.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;

public class RDFSourcesEditor extends KnowledgeBaseSourcesEditor {

    static final String PROTEGEURI = "http://protege.stanford.edu/";

    FileField _clsesField;
    FileField _instancesField;
    JTextField _namespaceField;

    String _setNamespace;
    boolean _updateNamespaceField;

    public RDFSourcesEditor(String projectURIString, PropertyList sources) {
        super(projectURIString, sources);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, 10, 10));
        panel.add(createClsesField());
        panel.add(createInstancesField());
        panel.add(createNamespaceField());
        add(panel);
    }

    // overwritten methods from the KnowledgeBaseSourcesEditor class ----------

    public void onProjectPathChange(String oldPath, String newPath) {
        if (newPath != null) {
            updatePath(_clsesField, newPath, ".rdfs");
            updatePath(_instancesField, newPath, ".rdf");
            if (_updateNamespaceField)
                updateNamespace(_namespaceField);
        }
    }

    // methods implementing the Validatable interface ------------------------

    public void saveContents() {
        String clses = getBaseFile(_clsesField);
        String instances = getBaseFile(_instancesField);
        String namespace = _namespaceField.getText();
        RDFBackend.setSourceFiles(getSources(), clses, instances, namespace);
    }

    public boolean validateContents() {
        return true;
    }

    // GUI stuff ------------------------------------------------------------

    public JComponent createClsesField() {
        String name = RDFBackend.getClsesFileName(getSources());
        _clsesField = new FileField("Classes file name", name, ".rdfs", "Ontology");
        return _clsesField;
    }

    public JComponent createInstancesField() {
        String name = RDFBackend.getInstancesFileName(getSources());
        _instancesField = new FileField("Instances file name", name, ".rdf", "Instances");
        return _instancesField;
    }

    public JComponent createNamespaceField() {
        String namespace = RDFBackend.getNamespace(getSources());
        if (namespace == null || namespace.trim().length() == 0) {
            String projectName = getProjectName();
            if (projectName == null)
                projectName = "kb";
            namespace = getNamespace(projectName);
            _updateNamespaceField = true;
        } else { // namespace already existed, so don't change it automatically
            _updateNamespaceField = false;
        }
        _namespaceField = ComponentFactory.createTextField();
        setNamespaceField(namespace);
        _namespaceField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent de) {
                check();
            }
            public void insertUpdate(DocumentEvent de) {
                check();
            }
            public void removeUpdate(DocumentEvent de) {
                check();
            }
            void check() {
                String namespaceTF = _namespaceField.getText();
                if (_updateNamespaceField && !namespaceTF.equals("") && !namespaceTF.equals(_setNamespace))
                    // user has changed the field, so never automatically change it:
                    _updateNamespaceField = false;
            }
        });
        return new LabeledComponent("Namespace", _namespaceField);
    }

    void setNamespaceField(String namespace) {
        _setNamespace = namespace; // remember namespace set automatically
        _namespaceField.setText(namespace);
    }

    // auxiliaries -------------------------------------------------------

    void updateNamespace(JTextField field) {
        String projectName = getProjectName();
        if (projectName != null) {
            String newNamespace = getNamespace(projectName);
            setNamespaceField(newNamespace);
        }
    }

    String getNamespace(String projectName) {
        String namespace = PROTEGEURI + projectName + "#";
        // remove spaces, ' and "
        if (namespace.indexOf(' ') != -1)
            namespace = namespace.replace(' ', '_');
        if (namespace.indexOf('\'') != -1)
            namespace = namespace.replace('\'', '_');
        if (namespace.indexOf('\"') != -1)
            namespace = namespace.replace('\"', '_');
        // shorten if too long ... !!!
        return namespace;
    }

    public URI getProjectURI() {
        return null;
        // return getProjectURI(_clsesField);
    }

    private String getProjectName() {
        return URIUtilities.getName(getProjectURI());
    }

}
