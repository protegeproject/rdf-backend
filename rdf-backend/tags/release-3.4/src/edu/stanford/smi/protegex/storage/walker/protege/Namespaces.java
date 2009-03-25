package edu.stanford.smi.protegex.storage.walker.protege;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

// import org.w3c.rdf.vocabulary.rdf_schema_19990303.*;
import org.w3c.rdf.vocabulary.rdf_schema_200001.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.*;

import edu.stanford.smi.protege.resource.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.walker.*;

public class Namespaces extends BaseNamespaces {

    static final String NAMESPACES_PROPERTY = "namespaces";
    static final String HIDE_NAMESPACES_PROPERTY = "hide_namespaces";

    PropertyList _sources;

    public Namespaces(String namespace, PropertyList sources) {
        super(namespace);
        _sources = sources;
        init();
    }

    public String getFrameName(WalkerFrame walkerFrame) {
        String namespace = walkerFrame.getNamespace();
        String localName = walkerFrame.getLocalName();
        if (namespace == null || namespace.equals(_defaultNamespace))
            return localName;
        else { // abbreviation + local name
            String abbrev = (String) _uris.get(namespace);
            if (abbrev == null) {
                abbrev = inventAbbrev(namespace);
                add(abbrev, namespace);
            }
            return abbrev + ":" + localName;
        }
    }

    void init() { // load values from sources (and add default/rdf/rdfs)
        String encodedString = _sources.getString(NAMESPACES_PROPERTY);
        if (encodedString != null)
            decode(encodedString);
        if (getURI("rdf") == null)
            add("rdf", RDF._Namespace);
        if (getURI("rdfs") == null)
            add("rdfs", RDFS._Namespace);
    }

    public void save() { // save to sources
        _sources.setString(NAMESPACES_PROPERTY, toEncodedString());
    }

    public void show() {

        // check if user wants to see the namespace again
        Boolean hide = _sources.getBoolean(HIDE_NAMESPACES_PROPERTY);
        if (hide != null && hide.booleanValue())
            return;

        final JFrame namespacesFrame = new JFrame("Namespaces");

        JPanel panel = new JPanel(new BorderLayout());

        panel.add(new JLabel("The following namespace abbreviations are used:"), BorderLayout.NORTH);

        Vector header = new Vector();
        header.add("abbreviation");
        header.add("URI");
        Vector rows = new Vector();
        Vector defaultRow = new Vector();
        defaultRow.add("");
        defaultRow.add(_defaultNamespace);
        rows.add(defaultRow);
        // use entrySet here ... !!!
        for (Iterator abbrevIterator = _abbrevs.keySet().iterator(); abbrevIterator.hasNext();) {
            String abbrev = (String) abbrevIterator.next();
            String uri = (String) _abbrevs.get(abbrev);
            Vector row = new Vector();
            row.add(abbrev);
            row.add(uri);
            rows.add(row);
        }
        Collections.sort(rows, new Comparator() {
            public int compare(Object o1, Object o2) {
                Vector row1 = (Vector) o1;
                Vector row2 = (Vector) o2;
                return ((String) row1.elementAt(0)).compareTo(((String) row2.elementAt(0)));
            }
        });
        DefaultTableModel model = new DefaultTableModel(rows, header) {
            public boolean isCellEditable(int row, int column) {
                return false; // DefaultTableModel returns true!
            }
        };
        JTable table = new JTable(model);
        table.getColumn(header.elementAt(0)).setPreferredWidth(150);
        table.getColumn(header.elementAt(1)).setPreferredWidth(350);
        JScrollPane scroller = new JScrollPane(table);
        scroller.setPreferredSize(new Dimension(500, 200));
        panel.add(scroller, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        final JCheckBox hideCB = new JCheckBox("don't show this window again");
        buttons.add(hideCB);
        JButton closeButton = new JButton("Close", Icons.getCloseIcon());
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                namespacesFrame.setVisible(false);
                namespacesFrame.dispose();
                if (hideCB.isSelected())
                    _sources.setBoolean(HIDE_NAMESPACES_PROPERTY, true);
            }
        });
        buttons.add(closeButton);
        panel.add(buttons, BorderLayout.SOUTH);

        namespacesFrame.getContentPane().add(panel);
        namespacesFrame.pack();
        namespacesFrame.setVisible(true);
    }

}
