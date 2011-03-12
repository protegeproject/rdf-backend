package edu.stanford.smi.protegex.storage.walker;

import java.util.*;

public interface WalkerSlotRestriction {

    public boolean isAny();
    public boolean isBoolean();
    public boolean isClass();
    public boolean isFloat();
    public boolean isInstance();
    public boolean isInteger();
    public boolean isString();
    public boolean isSymbol();

    public WalkerFrame getAllowedClass();
    public Collection getAllowedClasses();
    public Collection getAllowedParents();
    public Collection getAllowedValues();

    public Collection getValues();
    public Collection getDefaultValues();

    public int getMinimumCardinality();
    public int getMaximumCardinality();

    public String getMinimumValue();
    public String getMaximumValue();

    public Map getFacetOverrides(); // FACET

}
