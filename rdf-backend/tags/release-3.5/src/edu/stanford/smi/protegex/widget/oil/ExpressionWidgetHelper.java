package edu.stanford.smi.protegex.widget.oil;

import java.util.*;

import javax.swing.*;

import edu.stanford.smi.protege.event.*;
import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.widget.*;

public class ExpressionWidgetHelper implements OilConstants {

    AbstractSlotWidget itsWidget;
    KnowledgeBase itsKB;
    OilFrames frames;
    Cls itsCls;
    Slot itsSlot;
    JTextArea itsExpressionArea;
    Collection itsClasses; // with itsClsListener
    HashSet itsAllInstances; // with itsAllInstancesListener

    int itsMaxDepth = 9;
    int itsMaxWidth = 40; // max expression width
    int itsIndentStep = 4;

    static final int SAMELINE = -1; // used as indent value


    ExpressionWidgetHelper(AbstractSlotWidget widget) {
      itsWidget = widget;
    }

    void setExpressionTextArea(JTextArea textArea) {
      itsExpressionArea = textArea;
    }

    void initialize() {
      itsCls = itsWidget.getCls();
      itsSlot = itsWidget.getSlot();
      itsKB = itsCls.getKnowledgeBase();
      frames = new OilFrames(itsKB);
    }


    // pretty printer

    void prettyPrintExpressions() { 
      // called from listeners and when value/list is changed
      Collection values = itsWidget.getValues();
      StringBuffer expression = new StringBuffer();
      for (Iterator valueIterator = values.iterator();
	   valueIterator.hasNext();) {
	Instance value = (Instance)valueIterator.next();
	expression.append(prettyPrintExpression(value, 0, 0));
	expression.append("\n");
      }
      String expressionString = expression.toString();
      if (!expressionString.equals(itsExpressionArea.getText())) {
        itsExpressionArea.setText(expressionString);
	itsExpressionArea.setCaretPosition(0);
      }
    }

    String prettyPrintExpression(Instance expression, int depth, int indent) {
      if (depth >= itsMaxDepth)
	return indent("...", indent);
      Cls type = expression.getDirectType();
      if (type == null) {  // sometimes happens ...
	System.out.println("type is missing: " + expression);
        return indent("?", indent);
      }
      String typeName = type.getName();
      if (typeName.endsWith("Class")) { // hack
	return indent(expression.getBrowserText(), indent);
      } else if (typeName.equals(AND)) {
	Collection operands = frames.getOwnSlotValues(expression, HASOPERAND);
	return prettyPrint("and", operands, depth, indent);
      } else if (typeName.equals(OR)) {
	Collection operands = frames.getOwnSlotValues(expression, HASOPERAND);
	return prettyPrint("or", operands, depth, indent);
      } else if (typeName.equals(NOT)) {
	Collection operands = frames.getOwnSlotValues(expression, HASOPERAND);
	// change this, not has only one arg!! ??
	return prettyPrint("not", operands, depth, indent);
      } else if (typeName.equals(VALUETYPE) || typeName.equals(HASVALUE)) {
	String functor;
	if (typeName.equals(VALUETYPE)) functor = "value-type";
	else functor = "has-value";
	Instance oilProperty = 
	  (Instance)frames.getOwnSlotValue(expression, ONPROPERTY);
	String property;
	if (oilProperty != null) property = oilProperty.getBrowserText();
	else property = "?";
	Collection operands = frames.getOwnSlotValues(expression, TOCLASS);
	return prettyPrint(functor, property, operands, depth, indent);
      } else if (typeName.equals(MINCARDINALITY) 
		 || typeName.equals(MAXCARDINALITY)
		 || typeName.equals(CARDINALITY)) {
	String functor;
	if (typeName.equals(MINCARDINALITY)) functor = "min-cardinality";
	else if (typeName.equals(MAXCARDINALITY)) functor = "max-cardinality";
	else functor = "cardinality";
	Integer number = (Integer)frames.getOwnSlotValue(expression, NUMBER);
	String num;
	if (number == null) num = "?";
	else num = number.toString();
	Instance oilProperty = 
	  (Instance)frames.getOwnSlotValue(expression, ONPROPERTY);
	String property;
	if (oilProperty != null) property = oilProperty.getBrowserText();
	else property = "?";
	// should only be one!
	Collection operands = frames.getOwnSlotValues(expression, TOCLASS);
	return prettyPrint(functor, num, property, operands, depth, indent);
      } else
        return indent("?", indent);
    }

    String getOperandsSequence(Collection operands, int depth) {
      StringBuffer operandsExpression = new StringBuffer();
      for (Iterator opIterator = operands.iterator();
	   opIterator.hasNext();) {
	operandsExpression.append(
	  prettyPrintExpression((Instance)opIterator.next(), depth, SAMELINE));
	if (opIterator.hasNext())
	  operandsExpression.append(" ");
      }
      return operandsExpression.toString();
    }

    String prettyPrint(String functor, Collection operands,
	int depth, int indent) {
      return prettyPrint(functor, null, null, operands, depth, indent);
    }

    String prettyPrint(String functor, String op, Collection operands,
	int depth, int indent) {
      return prettyPrint(functor, op, null, operands, depth, indent);
    }

    String prettyPrint(String functor, String op1, String op2, 
	Collection operands, int depth, int indent) {
      // op1 and op2 are an (optional) additional operands
      depth++; // for all subexpressions
      // assume same line first
      String operandsSequence = 
	getOperandsSequence(operands, depth);
      if (op2 != null)
	operandsSequence = op2 + " " + operandsSequence;
      if (op1 != null)
	operandsSequence = op1 + " " + operandsSequence;
      int length = indent*itsIndentStep + 1 + functor.length() + 1
	+ operandsSequence.length();
      if (indent == SAMELINE || length <= itsMaxWidth)
	return indent(indent)
	  + "(" + functor + " " + operandsSequence + ")";
      else { // does not fit on same line, so lets break it up
	StringBuffer buffer = new StringBuffer();
	buffer.append(indent(indent));
	buffer.append("(" + functor);
	if (op1 != null) buffer.append(" " + op1); // on same line??
	if (op2 != null) buffer.append(" " + op2);
	buffer.append("\n");
	indent++;
        for (Iterator opIterator = operands.iterator();
	     opIterator.hasNext();) {
	  buffer.append(
	    prettyPrintExpression((Instance)opIterator.next(), depth, indent));
	  if (opIterator.hasNext())
	    buffer.append("\n");
	}
	buffer.append(")");
	return buffer.toString();
      }
    }

    String indent(int indent) {
      StringBuffer spaces = new StringBuffer();
      int l = indent*itsIndentStep;
      for (int i = 0; i < l; i++)
	spaces.append(' ');
      return spaces.toString();
    }

    String indent(String string, int indent) {
      if (indent == SAMELINE)
	return string;
      else
	return indent(indent) + string;
    }



    // all instances and class listeners: 
    // pretty print expression if anything (!) has changed!
    // we should have a GLOBAL place where you can listen for
    // any changes (one per project)

    // attached to ALL classes that can affect the expression
    private FrameListener itsAllInstancesListener =
      new FrameAdapter() {
        public void ownSlotValueChanged(FrameEvent event) { prettyPrintExpressions(); }
        public void browserTextChanged(FrameEvent event) { prettyPrintExpressions(); }
        public void nameChanged(FrameEvent event) { prettyPrintExpressions(); }
	// do we need any of the other methods??
      }
    ;

    // must listen to ALL classes used for the expressions
    private ClsListener itsClsListener =
      new ClsAdapter() {
        public void directInstanceCreated(ClsEvent event) { 
	  prettyPrintExpressions(); 
	  Instance instance = event.getInstance();
	  itsAllInstances.add(instance);
	  instance.addFrameListener(itsAllInstancesListener);
	}
        public void directInstanceDeleted(ClsEvent event) { 
	  prettyPrintExpressions();
	  Instance instance = event.getInstance();
	  instance.removeFrameListener(itsAllInstancesListener);
	  itsAllInstances.remove(instance);
	}
      }
    ;


    public static boolean isSuitable(Cls cls, Slot slot, Facet facet, 
				     boolean multiple) {
        boolean isSuitable = false;
        if (cls != null && slot != null) {
            boolean isInstance = cls.getTemplateSlotValueType(slot) == ValueType.INSTANCE;
            boolean isMultiple = cls.getTemplateSlotAllowsMultipleValues(slot);
	    if (isInstance && (multiple == isMultiple)) {
	      Cls expressionClass = cls.getKnowledgeBase().getCls(EXPRESSION);
	      if (expressionClass != null) {
		Collection allowedClasses = 
		  cls.getTemplateSlotAllowedClses(slot);
		boolean allowedClassesFit = true;
		for (Iterator acIterator = allowedClasses.iterator();
		     acIterator.hasNext();) {
		  Cls allowed = (Cls)acIterator.next();
	          if (!allowed.equals(expressionClass) 
		      && !allowed.hasSuperclass(expressionClass))
                    allowedClassesFit = false;
		}
		isSuitable = allowedClassesFit;
	      }
	    }
        }
        return isSuitable;
    }


    void addClassListeners() {
      itsClasses = frames.getSubclasses(EXPRESSION);
      for (Iterator classIterator = itsClasses.iterator();
	   classIterator.hasNext();)
	((Cls)classIterator.next()).addClsListener(itsClsListener);
    }

    void addAllInstancesListeners() {
      itsAllInstances = new HashSet();
      for (Iterator classIterator = itsClasses.iterator();
	   classIterator.hasNext();) {
	Cls cls = (Cls)classIterator.next();
	Collection instances = cls.getDirectInstances();
	itsAllInstances.addAll(instances);
      }
      for (Iterator instanceIterator = itsAllInstances.iterator();
	   instanceIterator.hasNext();)
	((Instance)instanceIterator.next())
	  .addFrameListener(itsAllInstancesListener);
    }

    void removeClassesAndAllInstancesListeners() {
      for (Iterator classIterator = itsClasses.iterator();
	   classIterator.hasNext();)
	((Cls)classIterator.next()).removeClsListener(itsClsListener);
      for (Iterator instanceIterator = itsAllInstances.iterator();
	   instanceIterator.hasNext();)
	((Instance)instanceIterator.next())
	  .removeFrameListener(itsAllInstancesListener);
    }


}


