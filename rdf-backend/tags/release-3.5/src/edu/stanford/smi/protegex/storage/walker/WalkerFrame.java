package edu.stanford.smi.protegex.storage.walker;

public interface WalkerFrame {

    public String getName(); // full unique name

    public String getDisplayName(); // for GUIs/browsers etc., not
    // necessarily unique

    public String getNamespace();

    public String getLocalName();

    // test for type

    public boolean isClass();

    public boolean isSlot();

    // test for special frames

    public boolean isThing(); // :THING, rdf:Resource, Top etc.

    public boolean isStandardClass(); // :STANDARD-CLASS, rdfs:Class, etc.

    public boolean isStandardSlot(); // :STANDARD-SLOT, rdf:Property, etc.

    // add: isConstraint (:CONSTRAINT, rdfs.ConstraintResource); 
    //      isConstraintSlot (for rdfs:ConstraintProperty) ??

}
