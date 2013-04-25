package edu.stanford.smi.protegex.widget.oil;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import edu.stanford.smi.protege.action.*;
import edu.stanford.smi.protege.event.*;
import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.ui.*;
import edu.stanford.smi.protege.util.*;

/** This is a modified InstanceListWidget for showing expressions. */
public class ExpressionListWidget extends ModifiableAbstractListWidget {

    private static final long serialVersionUID = -6199103954976842355L;
    private AllowableAction createInstanceAction;
    private AllowableAction addInstancesAction;
    private AllowableAction itsRemoveInstancesAction;
    private AllowableAction itsDeleteInstancesAction;

    private ExpressionWidgetHelper itsExpressionWidgetHelper;

    public ExpressionListWidget() {
        super();
        itsExpressionWidgetHelper = new ExpressionWidgetHelper(this);
    }

    private FrameListener itsInstanceListener = new FrameAdapter() {
        public void browserTextChanged(FrameEvent event) {
            super.browserTextChanged(event);
            // Log.trace("changed", this, "browserTextChanged");
            repaint();
        }
    };

    public static boolean isSuitable(Cls cls, Slot slot, Facet facet) {
        return ExpressionWidgetHelper.isSuitable(cls, slot, facet, true);
        // true -> multiple value mode
    }

    public void addItem(Object item) {
        super.addItem(item);
        addListener(CollectionUtilities.createCollection(item));
        itsExpressionWidgetHelper.prettyPrintExpressions();
    }

    public void addItems(Collection items) {
        super.addItems(items);
        addListener(items);
        itsExpressionWidgetHelper.prettyPrintExpressions();
    }

    private void addListener(Collection values) {
        Iterator i = values.iterator();
        while (i.hasNext()) {
            Instance instance = (Instance) i.next();
            instance.addFrameListener(itsInstanceListener);
        }
    }

    public void dispose() {
        super.dispose();
        removeListener(getValues());
        itsExpressionWidgetHelper.removeClassesAndAllInstancesListeners();
    }

    private Action getAddInstancesAction() {
        addInstancesAction = new AddAction("Select Instances") {
            private static final long serialVersionUID = -4948430651209447233L;

            public void onAdd() {
                Collection clses = getCls().getTemplateSlotAllowedClses(getSlot());
                addItems(
                    DisplayUtilities.pickInstances(
                        ExpressionListWidget.this,
                        clses,
                        (String) getValue(SHORT_DESCRIPTION)));
            }
        };
        return addInstancesAction;
    }

    public Action getCreateInstanceAction() {
        createInstanceAction = new CreateAction("Create Instance") {
            private static final long serialVersionUID = -451434151609012356L;

            public void onCreate() {
                Collection clses = getCls().getTemplateSlotAllowedClses(getSlot());
                Cls cls = DisplayUtilities.pickConcreteCls(ExpressionListWidget.this, getKnowledgeBase(), clses);
                if (cls != null) {
                    Instance instance = getKnowledgeBase().createInstance(null, cls);
                    if (instance instanceof Cls) {
                        ((Cls) instance).addDirectSuperclass(getKnowledgeBase().getRootCls());
                    }
                    showInstance(instance);
                    addItem(instance);
                }
            }
        };
        return createInstanceAction;
    }

    private Action getDeleteInstancesAction() {
        itsDeleteInstancesAction = new DeleteInstancesAction("Delete Selected Instances", this);
        return itsDeleteInstancesAction;
    }

    private Action getRemoveInstancesAction() {
        itsRemoveInstancesAction = new RemoveAction("Remove Selected Instances", this) {
            private static final long serialVersionUID = -1374313966177328036L;

            public void onRemove(Collection instances) {
                removeItems(instances);
                itsExpressionWidgetHelper.prettyPrintExpressions();
            }
        };
        return itsRemoveInstancesAction;
    }

    private Action getViewInstanceAction() {
        return new ViewAction("View Selected Instances", this) {
            private static final long serialVersionUID = -1181388030403766936L;

            public void onView(Object o) {
                showInstance((Instance) o);
            }
        };
    }

    public void initialize() {
        Action viewAction = getViewInstanceAction();
        super.initialize(viewAction);
        addButton(viewAction);
        addButton(getCreateInstanceAction());
        addButton(new ReferencersAction(this), false);
        addButton(getAddInstancesAction());
        addButton(getRemoveInstancesAction());
        addButton(getDeleteInstancesAction(), false);
        setRenderer(new FrameRenderer());
        // --- added:
        itsExpressionWidgetHelper.initialize();
        JPanel panel = getBorderLayoutPanel();
        JTextArea expressionArea = new JTextArea(6, 20);
        expressionArea.setForeground(Color.blue);
        expressionArea.setEditable(false);
        LabeledComponent expressionComponent = new LabeledComponent("Expressions", new JScrollPane(expressionArea));
        expressionComponent.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5)); // t l b r insets
        panel.add(expressionComponent, BorderLayout.CENTER);
        itsExpressionWidgetHelper.setExpressionTextArea(expressionArea);
        itsExpressionWidgetHelper.addClassListeners();
        itsExpressionWidgetHelper.addAllInstancesListeners();
    }

    private void removeListener(Collection values) {
        Iterator i = values.iterator();
        while (i.hasNext()) {
            Instance instance = (Instance) i.next();
            instance.removeFrameListener(itsInstanceListener);
        }
    }

    public void setEditable(boolean b) {
        createInstanceAction.setAllowed(b);
        addInstancesAction.setAllowed(b);
        itsRemoveInstancesAction.setAllowed(b);
        itsDeleteInstancesAction.setAllowed(b);
    }

    public void setValues(Collection values) {
        removeListener(getValues());
        addListener(values);
        super.setValues(values);
        itsExpressionWidgetHelper.prettyPrintExpressions();
    }
}
