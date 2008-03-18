package edu.stanford.smi.protegex.storage.rdf.oil;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.rdf.*;
import edu.stanford.smi.protegex.storage.rdf.roundtrip.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;

public class OilBackend extends RDFRTBackend implements OilConstants {


  public String getDescription() {
    return "OIL (Ontology Inference Layer) [beta]";
  }

  public String getProjectFilePath() {
    return "files/oil.pprj"; // empty oil project with nice forms
  }

  public void loadWalk(KnowledgeBase kb, PropertyList sources,
      String classesFileName, String instancesFileName, 
      String namespace, Namespaces namespaces,
      Collection errors, boolean included) {
    namespaces.add("oil", OILNAMESPACE);
    new OilFrameWalker(classesFileName, instancesFileName, 
	namespace, namespaces)
      .walk(new OilProtegeFrameCreator(kb, namespaces, included, errors));
  }

  public int getInterestingNamespacesSize() {
    return 5; // oil ns!
  }

  public void saveWalk(KnowledgeBase kb, PropertyList sources,
      String classesFileName, String instancesFileName, 
      Namespaces namespaces, Collection errors) {
    namespaces.add("oil", OILNAMESPACE);
    new OilProtegeFrameWalker(kb, namespaces)
      .walk(new OilFrameCreator(classesFileName,
	instancesFileName, namespaces, errors));
  }

  public void createSystemFrames(RDFKnowledgeBase kb) {
    int unrestricted = KnowledgeBase.MAXIMUM_CARDINALITY_UNBOUNDED;

    // create OIL system frames

    // classes

    // Expression
    createOilClass(kb, "Expression");
    createOilClass(kb, "ClassExpression", "Expression");
    createOilClass(kb, "BooleanExpression", "ClassExpression");
    createOilClass(kb, "And", "BooleanExpression");
    createOilClass(kb, "Or", "BooleanExpression");
    createOilClass(kb, "Not", "BooleanExpression");
    createOilClass(kb, "Class", list("ClassExpression", ":STANDARD-CLASS"));
    createOilClass(kb, "PropertyRestriction", "ClassExpression");
    createOilClass(kb, "CardinalityRestriction", "PropertyRestriction");
    createOilClass(kb, "Cardinality", "CardinalityRestriction");
    createOilClass(kb, "MinCardinality", "CardinalityRestriction");
    createOilClass(kb, "MaxCardinality", "CardinalityRestriction");
    createOilClass(kb, "HasValue", "PropertyRestriction");
    createOilClass(kb, "ValueType", "PropertyRestriction");
    // Top and Bottom
    createOilClass(kb, "Top", ":THING", "Class");
    createOilClass(kb, "Bottom", "Top", "Class");
    // Axiom
    createOilClass(kb, "Axiom");
    createOilClass(kb, "Covering", "Axiom");
    createOilClass(kb, "Cover", "Covering");
    createOilClass(kb, "DisjointCover", "Covering");
    createOilClass(kb, "Disjoint", "Axiom");
    createOilClass(kb, "Equivalence", "Axiom");
    // Property
    createOilClass(kb, "Property", ":STANDARD-SLOT");
    // createOilClass(kb, "ConstraintProperty", "Property"); // is not supported

    // slots
    createOilInstanceSlot(kb, "domain", "ClassExpression", 0,unrestricted);
    createOilInstanceSlot(kb, "hasObject", "ClassExpression", 1,unrestricted);
    createOilInstanceSlot(kb, "hasOperand", "Expression", 1,unrestricted);
    createOilInstanceSlot(kb, "hasPropertyRestriction", 
      "PropertyRestriction", 0,unrestricted);
    createOilInstanceSlot(kb, "hasSubject", "ClassExpression", 1,1);
    createOilIntegerSlot(kb, "number", 1,1);
    createOilInstanceSlot(kb, "onProperty", "Property", 1,1);
    createOilSymbolSlot(kb, "properties", 
      list("functional", "transitive", "symmetric"), 0,unrestricted);
    createOilInstanceSlot(kb, "range", "Expression", 0,unrestricted);
    createOilInstanceSlot(kb, "subClassOf", "ClassExpression", 0,unrestricted);
    // createOilInstanceSlot(kb, "subPropertyOf", "Property", 0,unrestricted);
    createOilInstanceSlot(kb, "toClass", "ClassExpression", 0,unrestricted);
    createOilSymbolSlot(kb, "type", list("primitive", "defined"), 0,1);

    // attach/restrict slots (in order of classes)
    attachOilSlot(kb, "BooleanExpression", "hasOperand");
    attachOilSlot(kb, "Not", "hasOperand", 1,1);
    attachOilSlot(kb, "Class", "hasPropertyRestriction");
    attachOilSlot(kb, "Class", "subClassOf");
    attachOilSlot(kb, "Class", "type");
    attachOilSlot(kb, "PropertyRestriction", "onProperty");
    attachOilSlot(kb, "PropertyRestriction", "toClass");
    attachOilSlot(kb, "CardinalityRestriction", "number");
    attachOilSlot(kb, "CardinalityRestriction", "toClass", 0,1);
    attachOilSlot(kb, "HasValue", "toClass", 1,unrestricted);
    attachOilSlot(kb, "ValueType", "toClass", 1,unrestricted);
    attachOilSlot(kb, "Axiom", "hasObject");
    attachOilSlot(kb, "Covering", "hasSubject");
    attachOilSlot(kb, "Property", "domain");
    attachOilSlot(kb, "Property", "range");
    attachOilSlot(kb, "Property", "properties");
    // attachOilSlot(kb, "Property", "subPropertyOf"); // we use Protege's

    // some documentation ... !!!

  }


  void createOilTopAndBottom(RDFKnowledgeBase kb) {
  }

  void createOilClass(RDFKnowledgeBase kb, String cls) {
    createOilClass(kb, cls, Collections.EMPTY_LIST);
  }

  void createOilClass(RDFKnowledgeBase kb, String cls, String superCls) {
    createOilClass(kb, cls, list(superCls));
  }

  void createOilClass(RDFKnowledgeBase kb, String cls, Collection superClses) {
    createOilClass(kb, cls, superClses, null);
  }

  void createOilClass(RDFKnowledgeBase kb, String cls, String superCls, 
      String metaCls) {
    createOilClass(kb, cls, list(superCls), metaCls);
  }

  void createOilClass(RDFKnowledgeBase kb, String cls, 
      Collection superClses, String metaCls) {
    // super classes and type must already exist!
    ArrayList parents = new ArrayList();
    if (superClses != null && !superClses.isEmpty()) {
      for (Iterator scIterator = superClses.iterator();
	   scIterator.hasNext();) {
	String superCls = (String)scIterator.next();
	Cls parent = getOilCls(kb, superCls);
	if (parent != null)
	  parents.add(parent);
      }
    } else {
      Cls thing = getOilCls(kb, ":THING");
      if (thing != null)
	parents.add(thing);
    }
    if (metaCls == null) {
      Cls newCls = kb.createCls(oilSystemFrame(cls), parents);
      newCls.setIncluded(true);
    } else {
      Cls protegeMetaCls = getOilCls(kb, metaCls);
      if (protegeMetaCls != null) {
	Cls newCls = kb.createCls(oilSystemFrame(cls), parents, protegeMetaCls);
	newCls.setIncluded(true);
      }
    }
  }


  void createOilInstanceSlot(RDFKnowledgeBase kb, String slot, 
      String allowedCls, int minCard, int maxCard) {
    Slot newSlot = kb.createSlot(oilSystemFrame(slot));
    newSlot.setIncluded(true);
    newSlot.setValueType(ValueType.INSTANCE);
    ArrayList allowedClses = new ArrayList();
    Cls protegeAllowedCls = getOilCls(kb, allowedCls);
    if (protegeAllowedCls != null)
      allowedClses.add(protegeAllowedCls);
    newSlot.setAllowedClses(allowedClses);
    newSlot.setMinimumCardinality(minCard);
    newSlot.setMaximumCardinality(maxCard);
  }

  void createOilSymbolSlot(RDFKnowledgeBase kb, String slot, 
      Collection allowedValues, int minCard, int maxCard) {
    Slot newSlot = kb.createSlot(oilSystemFrame(slot));
    newSlot.setIncluded(true);
    newSlot.setValueType(ValueType.SYMBOL);
    newSlot.setAllowedValues(allowedValues);
    newSlot.setMinimumCardinality(minCard);
    newSlot.setMaximumCardinality(maxCard);
  }

  void createOilIntegerSlot(RDFKnowledgeBase kb, String slot, 
      int minCard, int maxCard) {
    Slot newSlot = kb.createSlot(oilSystemFrame(slot));
    newSlot.setIncluded(true);
    newSlot.setValueType(ValueType.INTEGER);
    newSlot.setMinimumCardinality(minCard);
    newSlot.setMaximumCardinality(maxCard);
  }

  void attachOilSlot(RDFKnowledgeBase kb, String cls, String slot) {
    Cls protegeCls = getOilCls(kb, cls);
    Slot protegeSlot = getOilSlot(kb, slot);
    if (protegeCls != null && protegeSlot != null)
      protegeCls.addDirectTemplateSlot(protegeSlot);
  }

  void attachOilSlot(RDFKnowledgeBase kb, String cls, String slot, 
      int minCard, int maxCard) {
    Cls protegeCls = getOilCls(kb, cls);
    Slot protegeSlot = getOilSlot(kb, slot);
    if (protegeCls != null && protegeSlot != null) {
      protegeCls.setTemplateSlotMinimumCardinality(protegeSlot, minCard);
      protegeCls.setTemplateSlotMaximumCardinality(protegeSlot, maxCard);
    }
  }

  Cls getOilCls(RDFKnowledgeBase kb, String name) {
    Cls cls = kb.getCls(oilSystemFrame(name));
    if (cls == null)
      System.err.println("[OIL backend] internal error: class " 
	+ name + " does not exist");
    return cls;
  }

  Slot getOilSlot(RDFKnowledgeBase kb, String name) {
    Slot slot = kb.getSlot(oilSystemFrame(name));
    if (slot == null)
      System.err.println("[OIL backend] internal error: slot " 
	+ name + " does not exist");
    return slot;
  }

  Collection list(String v1) {
    return list(v1, null, null);
  }

  Collection list(String v1, String v2) {
    return list(v1, v2, null);
  }

  Collection list(String v1, String v2, String v3) {
    ArrayList list = new ArrayList();
    if (v1 != null) list.add(v1);
    if (v2 != null) list.add(v2);
    if (v3 != null) list.add(v3);
    return list;
  }

  String oilSystemFrame(String name) {
    if (!name.startsWith(":"))
      return "oil:" + name;
    else
      return name;
  }


}


