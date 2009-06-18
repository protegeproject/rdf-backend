package edu.stanford.smi.protegex.storage.rdf;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.walker.*;

public class RDFSlotRestriction implements WalkerSlotRestriction {

    WalkerFrame allowedClass;
    Collection allowedClasses;

    RDFSlotRestriction() { // case 1: Literal=String
        allowedClass = null;
    }

    RDFSlotRestriction(WalkerFrame range) { // case 2: range
        allowedClass = range;
        allowedClasses = CollectionUtilities.createCollection(range);
    }

    public boolean isAny() {
        return false;
    } // ... ???
    public boolean isBoolean() {
        return false;
    }
    public boolean isClass() {
        return false;
    } // ... !!!
    public boolean isFloat() {
        return false;
    }
    public boolean isInstance() {
        return allowedClass != null;
    }
    public boolean isInteger() {
        return false;
    }
    public boolean isString() {
        return allowedClass == null;
    }
    public boolean isSymbol() {
        return false;
    }

    public WalkerFrame getAllowedClass() {
        return allowedClass;
    }
    public Collection getAllowedClasses() {
        return allowedClasses;
    }
    public Collection getAllowedParents() {
        return null;
    }
    public Collection getAllowedValues() {
        return null;
    }

    public Collection getValues() {
        return null;
    }

    public Collection getDefaultValues() {
        return null;
    }

    public int getMinimumCardinality() {
        return 0;
    }
    public int getMaximumCardinality() {
        return KnowledgeBase.MAXIMUM_CARDINALITY_UNBOUNDED;
    }

    public String getMinimumValue() {
        return null;
    }
    public String getMaximumValue() {
        return null;
    }

    public Map getFacetOverrides() { // FACET
        return null;
    }

}
