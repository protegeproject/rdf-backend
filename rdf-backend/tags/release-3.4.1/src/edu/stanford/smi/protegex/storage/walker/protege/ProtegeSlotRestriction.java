package edu.stanford.smi.protegex.storage.walker.protege;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.walker.*;

public class ProtegeSlotRestriction implements WalkerSlotRestriction {

    ProtegeFrames _wframes;
    ValueType valueType;
    WalkerFrame allowedClass;
    Collection allowedClasses;
    Collection allowedParents;
    Collection allowedValues;
    Collection values;
    Collection defaultValues;
    int minCard;
    int maxCard;
    Number minVal;
    Number maxVal;
    // FACET
    HashMap facetOverrides; // facet -> values (for user-defined facets)

    ProtegeSlotRestriction(ProtegeFrames wframes, Slot slot) {
        // for global slots
        _wframes = wframes;
        valueType = slot.getValueType();
        allowedClasses = wframes(slot.getAllowedClses());
        allowedParents = wframes(slot.getAllowedParents());
        allowedValues = wframes(slot.getAllowedValues());
        values = wframes(slot.getValues());
        defaultValues = wframes(slot.getDefaultValues());
        minCard = slot.getMinimumCardinality();
        maxCard = slot.getMaximumCardinality();
        minVal = slot.getMinimumValue();
        maxVal = slot.getMaximumValue();
        facetOverrides = null;
    }

    ProtegeSlotRestriction(ProtegeFrames wframes, Cls cls, Slot slot) {
        // for overridden slot facets
        // simply store ALL facets, not just the overridden ones
        // the current code (see ProtegeFrameCreator) relies on it,
        // so don't change this to just store the overridden facets!!
        _wframes = wframes;
        valueType = cls.getTemplateSlotValueType(slot);
        allowedClasses = wframes(cls.getTemplateSlotAllowedClses(slot));
        allowedParents = wframes(cls.getTemplateSlotAllowedParents(slot));
        allowedValues = wframes(cls.getTemplateSlotAllowedValues(slot));
        values = wframes(cls.getTemplateSlotValues(slot));
        defaultValues = wframes(cls.getTemplateSlotDefaultValues(slot));
        minCard = cls.getTemplateSlotMinimumCardinality(slot);
        maxCard = cls.getTemplateSlotMaximumCardinality(slot);
        minVal = cls.getTemplateSlotMinimumValue(slot);
        maxVal = cls.getTemplateSlotMaximumValue(slot);
        // FACET
        // user-defined facets
        facetOverrides = new HashMap();
        Collection facets = cls.getTemplateFacets(slot);
        for (Iterator facetIterator = facets.iterator(); facetIterator.hasNext();) {
            Facet facet = (Facet) facetIterator.next();
            if (!facet.isSystem()) { // user-defined (don't use isIncluded here!)
                Collection facetValues = cls.getDirectTemplateFacetValues(slot, facet);
                facetOverrides.put(wframe(facet), wframes(facetValues));
            }
        }
    }

    void setAllowedClass(WalkerFrame allowedClass) {
        this.allowedClass = allowedClass;
    }

    public boolean isAny() {
        return valueType.equals(ValueType.ANY);
    }

    public boolean isBoolean() {
        return valueType.equals(ValueType.BOOLEAN);
    }

    public boolean isClass() {
        return valueType.equals(ValueType.CLS);
    }

    public boolean isFloat() {
        return valueType.equals(ValueType.FLOAT);
    }

    public boolean isInstance() {
        return valueType.equals(ValueType.INSTANCE);
    }

    public boolean isInteger() {
        return valueType.equals(ValueType.INTEGER);
    }

    public boolean isString() {
        return valueType.equals(ValueType.STRING);
    }

    public boolean isSymbol() {
        return valueType.equals(ValueType.SYMBOL);
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
        if (minVal != null)
            return minVal.toString();
        else
            return null;
    }

    public String getMaximumValue() {
        if (maxVal != null)
            return maxVal.toString();
        else
            return null;
    }

    // FACET
    public Map getFacetOverrides() {
        return facetOverrides;
    }

    // auxiliaries

    WalkerFrame wframe(Frame frame) {
        return (WalkerFrame) _wframes.wframe(frame);
    }

    Collection wframes(Collection values) {
        return _wframes.wframes(values);
    }

}
