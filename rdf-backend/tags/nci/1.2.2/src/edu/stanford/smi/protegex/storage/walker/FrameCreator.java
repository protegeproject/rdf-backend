package edu.stanford.smi.protegex.storage.walker;

import java.util.*;

public interface FrameCreator {

    public void start();

    public void createCls(
        WalkerFrame cls,
        Collection superclasses,
        WalkerFrame type,
        boolean abstrct,
        String documentation);

    public boolean singleAllowedClass();

    public void createInstance(WalkerFrame instance, WalkerFrame type, String documentation);

    public void createSlot(
        WalkerFrame slot,
        WalkerFrame type,
        Collection superslots,
        WalkerFrame inverseSlot,
        WalkerFrame associatedFacet,
        WalkerSlotRestriction slotRestriction,
        String documentation);

    public void attachSlot(
        WalkerFrame cls,
        WalkerFrame slot,
        boolean direct,
        WalkerSlotRestriction overriddenSlotRestriction,
        String overriddenDocumentation);
    // note: overriddenSlotRestriction != null if slot is somewhere
    // overridden (overriddenDocumentation can only be additionally != null!)

    public void addOwnSlotValues(WalkerFrame instance, WalkerFrame slot, Collection values);

    public void finish();

    public void error(String message);

}
