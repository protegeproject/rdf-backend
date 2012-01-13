package edu.stanford.smi.protegex.widget.oil;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import edu.stanford.smi.protege.ui.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protege.widget.*;

/** This is a modifiable AbstractListWidget, i.e., some swing
    components can be added (to the itsBorderLayoutPanel) */
public abstract class ModifiableAbstractListWidget extends AbstractSlotWidget {
    private static final long serialVersionUID = -7887382006986579074L;
    private JList itsList;
    private JPanel itsBorderLayoutPanel; // [MS]
    private LabeledComponent itsLabeledComponent;
    private SwitchableListSelectionListener itsListListener = new ListSelectionListenerAdapter(this);

    public ModifiableAbstractListWidget() {
        setPreferredColumns(2);
        setPreferredRows(5); // [MS]
    }

    public JPanel getBorderLayoutPanel() { // [MS]
      // NORTH contains the list, all other parts can be added to
      return itsBorderLayoutPanel;
    }

    public void addButton(Action action) {
        addButton(action, true);
    }

    public void addButton(Action action, boolean defaultState) {
        addButtonConfiguration(action, defaultState);
        if (displayButton(action)) {
            itsLabeledComponent.addHeaderButton(action);
        }
    }

    public void addItem(Object o) {
        ComponentUtilities.addListValue(itsList, o);
    }

    public void addItems(Collection items) {
        ComponentUtilities.addListValues(itsList, items);
    }

    public boolean contains(Object o) {
        return ComponentUtilities.listValuesContain(itsList, o);
    }

    private JComponent createLabeledComponent(Action action) {
        itsList = ComponentFactory.createList(action, true);
        itsList.getModel().addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent event) {
                valueChanged();
            }

            public void intervalAdded(ListDataEvent event) {
                valueChanged();
            }

            public void intervalRemoved(ListDataEvent event) {
                valueChanged();
            }
        });
        itsList.setCellRenderer(new FrameRenderer());
        itsList.addListSelectionListener(itsListListener);
	// --- this is changed: [MS]
	JPanel listPanel = new JPanel(new BorderLayout());
	listPanel.add(new JScrollPane(itsList));
	listPanel.setBorder(
	  BorderFactory.createEmptyBorder(5,5,0,5)); // t l b r insets
	itsBorderLayoutPanel = new JPanel(new BorderLayout());
	itsBorderLayoutPanel.add(listPanel, BorderLayout.NORTH);
	itsBorderLayoutPanel.setBorder(BorderFactory.createEtchedBorder());
        itsLabeledComponent = 
	  new LabeledComponent(getLabel(), itsBorderLayoutPanel, true);
	  // true: allow stretching
	// --- end changes [MS]
        return itsLabeledComponent;
    }

    public Collection getSelection() {
        return ComponentUtilities.getSelection(itsList);
    }

    public Collection getValues() {
        return ComponentUtilities.getListValues(itsList);
    }

    public void initialize() {
        initialize(null);
    }

    public void initialize(Action action) {
        add(createLabeledComponent(action));
    }

    public void removeAllItems() {
        ComponentUtilities.clearListValues(itsList);
    }

    public void removeItem(Object o) {
        ComponentUtilities.removeListValue(itsList, o);
        // notifySelectionListeners();        // workaround for JDK1.2 ListSelectionModel bug
    }

    public void removeItems(Collection items) {
        ComponentUtilities.removeListValues(itsList, items);
        // notifySelectionListeners();     // workaround for JDK1.2 ListSelectionModel bug
    }

    public void replaceItem(Object oldItem, Object newItem) {
        ComponentUtilities.replaceListValue(itsList, oldItem, newItem);
    }

    public void setEditable(boolean b) {
        Iterator i = itsLabeledComponent.getHeaderButtonActions().iterator();
        while (i.hasNext()) {
            Action action = (Action) i.next();
            if (action instanceof CreateAction
                || action instanceof AddAction
                || action instanceof RemoveAction
                || action instanceof DeleteAction) {
                ((AllowableAction) action).setAllowed(b);
            }

        }
    }

    public void setRenderer(ListCellRenderer renderer) {
        itsList.setCellRenderer(renderer);
    }

    public void setSelection(Object o) {
        itsList.setSelectedValue(o, true);
    }

    public void setValues(Collection values) {
        ComponentUtilities.setListValues(itsList, values);
    }
}


