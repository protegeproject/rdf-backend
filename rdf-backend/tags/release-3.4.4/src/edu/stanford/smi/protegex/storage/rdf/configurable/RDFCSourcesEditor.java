package edu.stanford.smi.protegex.storage.rdf.configurable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import edu.stanford.smi.protege.resource.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.rdf.*;

public class RDFCSourcesEditor extends RDFSourcesEditor {

    // JRadioButton _useRoundtripBackendRB; // old version
    JCheckBox _useSimpleBackendCB;

    public RDFCSourcesEditor(String projectURIString, PropertyList sources) {
        super(projectURIString, sources);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, 10, 5));
        panel.add(createClsesField());
        panel.add(createInstancesField());
        panel.add(createNamespaceField());
        panel.add(createConfigurationPanel());
        add(panel);
    }

    JComponent createConfigurationPanel() {

        JPanel configPanel = new JPanel();

        PropertyList sources = getSources();

        // roundtrip vs. plain RDFS
        boolean useRoundtrip = RDFCBackend.getUseRoundtripBackend(sources);

        _useSimpleBackendCB = new JCheckBox("plain RDFS (may loose Protege-specific facets)");
        _useSimpleBackendCB.setSelected(!useRoundtrip);
        configPanel.add(_useSimpleBackendCB);

        Collection includedProjects = RDFKnowledgeBase.getIncludedProjects(sources);
        if (!includedProjects.isEmpty()) { // ... other conditions
            final JButton advancedButton = new JButton("Advanced ...");
            advancedButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    Component root = SwingUtilities.getRoot(advancedButton);
                    if (root instanceof Dialog) // should always be the case
                        advancedDialog((Dialog) root);
                }
            });
            configPanel.add(advancedButton);
        }

        // additional configuration goes here ...

        return configPanel;

    }

    boolean useRoundtripBackend() {
        return !_useSimpleBackendCB.isSelected();
        // return _useRoundtripBackendRB.isSelected(); // old version
    }

    ArrayList getIncludedProjectsNamespaces() {
        return null;
    }

    public void saveContents() {
        super.saveContents();
        RDFCBackend.saveConfiguration(getSources(), useRoundtripBackend());
        // add here ...
    }

    void advancedDialog(Dialog dialog) {

        final JDialog advancedFrame = new JDialog(dialog, "Advanced", true);

        JPanel panel = new JPanel(new BorderLayout());

        panel.add(new JLabel("Rename namespaces of included frames:"), BorderLayout.NORTH);

        Vector header = new Vector();
        header.add("project");
        header.add("namespace");
        final Vector rows = new Vector();

        final PropertyList sources = getSources();

        Collection includedProjects = RDFKnowledgeBase.getIncludedProjects(sources);
        final Collection includedProjectsNamespaces = RDFKnowledgeBase.getIncludedProjectsNamespaces(sources);

        Iterator projectIterator = includedProjects.iterator();
        Iterator namespaceIterator = includedProjectsNamespaces.iterator();
        while (projectIterator.hasNext()) {
            String projectName = (String) projectIterator.next();
            String namespace = (String) namespaceIterator.next();
            Vector row = new Vector();
            row.add(projectName);
            row.add(namespace);
            rows.add(row);
        }

        DefaultTableModel model = new DefaultTableModel(rows, header) {
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };
        final JTable table = new JTable(model);
        table.getColumn(header.elementAt(0)).setPreferredWidth(150);
        table.getColumn(header.elementAt(1)).setPreferredWidth(350);
        JScrollPane scroller = new JScrollPane(table);
        scroller.setPreferredSize(new Dimension(500, 150));
        panel.add(scroller, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton okButton = new JButton("Ok", Icons.getOkIcon());
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                // stop editing
                TableCellEditor cellEditor = table.getCellEditor();
                if (cellEditor != null && cellEditor instanceof DefaultCellEditor)
                     ((DefaultCellEditor) cellEditor).stopCellEditing();
                advancedFrame.setVisible(false);
                advancedFrame.dispose();
                // save new namespaces
                String newNamespaces = "";
                Iterator rowIterator = rows.iterator();
                Iterator oldIterator = includedProjectsNamespaces.iterator();
                boolean renamed = false;
                while (rowIterator.hasNext()) {
                    Vector row = (Vector) rowIterator.next();
                    String newNamespace = ((String) row.elementAt(1)).trim().replace(' ', '_');
                    String oldNamespace = (String) oldIterator.next();
                    newNamespaces += newNamespace + " ";
                    if (!renamed && !oldNamespace.equals(newNamespace))
                        renamed = true;
                }
                if (!renamed)
                    newNamespaces = ""; // nothing was changed
                RDFKnowledgeBase.setRenamingNamespaces(sources, newNamespaces);
            }
        });
        buttons.add(okButton);
        JButton cancelButton = new JButton("Cancel", Icons.getCancelIcon());
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                advancedFrame.setVisible(false);
                advancedFrame.dispose();
            }
        });
        buttons.add(cancelButton);
        panel.add(buttons, BorderLayout.SOUTH);

        advancedFrame.getContentPane().add(panel);
        advancedFrame.pack();
        advancedFrame.setVisible(true);

    }

}
