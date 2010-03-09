package edu.stanford.smi.protegex.storage.rdf.roundtrip;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.walker.*;

public class RDFRTSlotRestriction implements WalkerSlotRestriction, RDFRTConstants {

    String valueType; // from RDFRTConstants: INSTANCE, CLS, etc.
    WalkerFrame allowedClass;
    Collection allowedClasses;
    Collection allowedParents;
    Collection allowedValues;
    Collection values;
    Collection defaultValues;
    int minCard = 0;
    int maxCard = KnowledgeBase.MAXIMUM_CARDINALITY_UNBOUNDED; // now -1, not 0!!

    String minVal;
    String maxVal;
    HashMap facetOverrides = new HashMap(); // FACET

    RDFRTSlotRestriction(String valueType) {
        this.valueType = valueType;
    }

    RDFRTSlotRestriction(WalkerFrame allowedClass, Collection allowedClasses) {
        valueType = INSTANCE;
        this.allowedClass = allowedClass;
        this.allowedClasses = allowedClasses;
    }

    public void setAllowedParents(Collection allowedParents) {
        this.allowedParents = allowedParents;
    }

    public void setAllowedValues(Collection allowedValues) {
        this.allowedValues = allowedValues;
    }

    public void setDefaultValues(Collection defaultValues) {
        this.defaultValues = defaultValues;
    }

    public void setValues(Collection values) {
        this.values = values;
    }

    public void setMinimumCardinality(int n) {
        minCard = n;
    }
    public void setMaximumCardinality(int n) {
        maxCard = n;
    }

    public void setMinimumValue(String v) {
        minVal = v;
    }
    public void setMaximumValue(String v) {
        maxVal = v;
    }

    public void putFacetOverride(WalkerFrame facet, Collection values) {
        facetOverrides.put(facet, values);
    }

    public boolean isAny() {
        return valueType.equals(ANY);
    }

    public boolean isBoolean() {
        return valueType.equals(BOOLEAN);
    }

    public boolean isClass() {
        return valueType.equals(CLS);
    }

    public boolean isFloat() {
        return valueType.equals(FLOAT);
    }

    public boolean isInstance() {
        return valueType.equals(INSTANCE);
    }

    public boolean isInteger() {
        return valueType.equals(INTEGER);
    }

    public boolean isString() {
        return valueType.equals(STRING);
    }

    public boolean isSymbol() {
        return valueType.equals(SYMBOL);
    }

    public WalkerFrame getAllowedClass() {
        return allowedClass;
    }
    public Collection getAllowedClasses() {
        return allowedClasses;
    }
    public Collection getAllowedParents() {
        return allowedParents;
    }
    public Collection getAllowedValues() {
        return allowedValues;
    }
    public Collection getValues() {
        return values;
    }
    public Collection getDefaultValues() {
        return defaultValues;
    }

    public int getMinimumCardinality() {
        return minCard;
    }
    public int getMaximumCardinality() {
        return maxCard;
    }

    public String getMinimumValue() {
        return minVal;
    }
    public String getMaximumValue() {
        return maxVal;
    }

    public Map getFacetOverrides() { // FACET
        return facetOverrides;
    }

}
