package edu.stanford.smi.protegex.widget.oil;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import edu.stanford.smi.protege.action.*;
import edu.stanford.smi.protege.event.*;
import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.ui.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protege.widget.*;

/** This is a modified InstanceFieldWidget for showing expressions. */
public class ExpressionFieldWidget extends AbstractSlotWidget {

    private static final long serialVersionUID = 1824973146816376230L;
    private JList itsList;
    private Instance itsInstance;
    private AllowableAction createAction;
    private AllowableAction addAction;
    private AllowableAction itsRemoveAction;
    private AllowableAction itsDeleteAction;

    private ExpressionWidgetHelper itsExpressionWidgetHelper;

    public ExpressionFieldWidget() {
        super();
        itsExpressionWidgetHelper = new ExpressionWidgetHelper(this);
    }

    private FrameListener itsInstanceListener = new FrameAdapter() {
        public void browserTextChanged(FrameEvent event) {
            itsList.repaint();
        }
    };

    public static boolean isSuitable(Cls cls, Slot slot, Facet facet) {
        return ExpressionWidgetHelper.isSuitable(cls, slot, facet, false);
        // false -> single value mode

    }

    private void addButton(LabeledComponent c, Action action, boolean defaultState) {
        addButtonConfiguration(action, defaultState);
        if (displayButton(action)) {
            c.addHeaderButton(action);
        }
    }

    public JList createList() {
        JList list = ComponentFactory.createSingleItemList(getViewInstanceAction());
        list.setCellRenderer(new FrameRenderer());
        return list;
    }

    public void dispose() {
        super.dispose();
        if (itsInstance != null) {
            itsInstance.removeFrameListener(itsInstanceListener);
        }
        itsExpressionWidgetHelper.removeClassesAndAllInstancesListeners();
    }

    protected Action getCreateInstanceAction() {
        createAction = new CreateAction("Create Instance") {
            private static final long serialVersionUID = -610960903582699721L;

            public void onCreate() {
                Collection clses = getCls().getTemplateSlotAllowedClses(getSlot());
                Cls cls = DisplayUtilities.pickConcreteCls(ExpressionFieldWidget.this, getKnowledgeBase(), clses);
                if (cls != null) {
                    Instance instance = getKnowledgeBase().createInstance(null, cls);
                    if (instance instanceof Cls) {
                        ((Cls) instance).addDirectSuperclass(getKnowledgeBase().getRootCls());
                    }
                    showInstance(instance);
                    setDisplayedInstance(instance);
                }
            }
        };
        return createAction;
    }

    protected Action getDeleteInstancesAction() {
        itsDeleteAction = new DeleteInstancesAction("Delete Instance", this);
        return itsDeleteAction;
    }

    protected Action getRemoveInstanceAction() {
        itsRemoveAction = new RemoveAction("Remove Instance", this) {
            private static final long serialVersionUID = -5501295658808008824L;

            public void onRemove(Object o) {
                removeDisplayedInstance();
            }
        };
        return itsRemoveAction;
    }

    protected Action getSelectInstanceAction() {
        addAction = new AddAction("Add Instance") {
            private static final long serialVersionUID = 9013637197574384827L;

            public void onAdd() {
                Collection clses = getCls().getTemplateSlotAllowedClses(getSlot());
                Instance instance = DisplayUtilities.pickInstance(ExpressionFieldWidget.this, clses);
                if (instance != null) {
                    setDisplayedInstance(instance);
                }
            }
        };
        return addAction;
    }

    public Collection getSelection() {
        return CollectionUtilities.createCollection(itsInstance);
    }

    public Collection getValues() {
        return CollectionUtilities.createList(itsInstance);
    }

    protected Action getViewInstanceAction() {
        return new ViewAction("View Instance", this) {
            private static final long serialVersionUID = -1023964800761519701L;

            public void onView(Object o) {
                showInstance((Instance) o);
            }
        };
    }

    public void initialize() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        LabeledComponent c = new LabeledComponent(getLabel(), panel, true);
        // true: allow stretching
        itsList = createList();
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.add(itsList);
        listPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5)); // t l b r insets
        panel.add(listPanel, BorderLayout.NORTH);
        itsExpressionWidgetHelper.initialize();
        JTextArea expressionArea = new JTextArea(2, 20);
        expressionArea.setForeground(Color.blue);
        expressionArea.setEditable(false);
        LabeledComponent expressionComponent = new LabeledComponent("Expression", new JScrollPane(expressionArea));
        expressionComponent.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5)); // t l b r insets
        panel.add(expressionComponent, BorderLayout.CENTER);
        itsExpressionWidgetHelper.setExpressionTextArea(expressionArea);
        itsExpressionWidgetHelper.addClassListeners();
        itsExpressionWidgetHelper.addAllInstancesListeners();
        addButton(c, getViewInstanceAction(), true);
        addButton(c, new ReferencersAction(this), false);
        addButton(c, getCreateInstanceAction(), true);
        addButton(c, getSelectInstanceAction(), true);
        addButton(c, getRemoveInstanceAction(), true);
        addButton(c, getDeleteInstancesAction(), false);
        add(c);
        setPreferredColumns(2);
        setPreferredRows(2);
    }

    private void removeDisplayedInstance() {
        replaceInstance(null);
        updateList();
        valueChanged();
    }

    private void replaceInstance(Instance instance) {
        if (itsInstance != null) {
            itsInstance.removeFrameListener(itsInstanceListener);
        }
        itsInstance = instance;
        if (itsInstance != null) {
            itsInstance.addFrameListener(itsInstanceListener);
        }
        notifySelectionListeners();
        itsExpressionWidgetHelper.prettyPrintExpressions(); // !! [MS]
    }

    private void setDisplayedInstance(Instance instance) {
        replaceInstance(instance);
        updateList();
        valueChanged();
    }

    public void setEditable(boolean b) {
        createAction.setAllowed(b);
        addAction.setAllowed(b);
        itsRemoveAction.setAllowed(b);
        itsDeleteAction.setAllowed(b);
    }

    public void setValues(Collection values) {
        Instance value = (Instance) CollectionUtilities.getFirstItem(values);
        replaceInstance(value);
        updateList();
    }

    private void updateList() {
        ComponentUtilities.setListValues(itsList, CollectionUtilities.createCollection(itsInstance));
    }

}
