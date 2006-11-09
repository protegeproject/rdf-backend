package edu.stanford.smi.protegex.widget.oil;

import java.util.*;

import javax.swing.*;

import edu.stanford.smi.protege.model.*;

class OilFrames {

    KnowledgeBase itsKB;
    JTextArea itsTextArea; // for reporting warnings/errors
    HashMap itsSlots;
    HashMap itsClses;

    OilFrames(KnowledgeBase kb) {
        this(kb, null);
    }

    OilFrames(KnowledgeBase kb, JTextArea textArea) {
        itsKB = kb;
        itsTextArea = textArea; // for reporting errors
        itsSlots = new HashMap();
        itsClses = new HashMap();
    }

    Cls getCls(String name) {
        Cls cls = (Cls) itsClses.get(name);
        if (cls == null) {
            cls = itsKB.getCls(name);
            if (cls == null)
                report("class not found: " + name);
            else
                itsClses.put(name, cls);
        }
        return cls;
    }

    Slot getSlot(String name) {
        Slot slot = (Slot) itsSlots.get(name);
        if (slot == null) {
            slot = itsKB.getSlot(name);
            if (slot == null)
                report("slot not found: " + name);
            else
                itsSlots.put(name, slot);
        }
        return slot;
    }

    Collection getSubclasses(String clsName) {
        Cls cls = getCls(clsName);
        if (cls != null)
            return cls.getSubclasses();
        else
            return Collections.EMPTY_LIST;
    }

    Collection getInstances(String clsName) {
        Cls cls = getCls(clsName);
        if (cls != null)
            return cls.getInstances();
        else
            return Collections.EMPTY_LIST;
    }

    Object getOwnSlotValue(Instance inst, String slotName) {
        Slot slot = getSlot(slotName);
        if (inst != null && slot != null)
            return inst.getOwnSlotValue(slot);
        else
            return null;
    }

    Object getOwnSlotRequiredValue(Instance inst, String slotName) {
        return getOwnSlotRequiredValue(inst, slotName, null);
    }

    Object getOwnSlotRequiredValue(Instance inst, String slotName, Object def) {
        Slot slot = getSlot(slotName);
        if (inst != null && slot != null) {
            Object value = inst.getOwnSlotValue(slot);
            // check there's only one value!
            if (value != null)
                return value;
            else {
                report("no value for slot " + slotName + " on frame " + inst);
                return def;
            }
        } else
            return def;
    }

    String getOwnSlotRequiredValueName(Instance inst, String slotName, String def) {
        Frame frame = (Frame) getOwnSlotRequiredValue(inst, slotName);
        // check if cast to Frame is ok ... !!!
        if (frame == null)
            return def;
        else
            return frame.getName();
    }

    int getOwnSlotRequiredIntValue(Instance inst, String slotName, int def) {
        Integer integer = (Integer) getOwnSlotRequiredValue(inst, slotName);
        // check if cast to Integer is ok ... !!!
        if (integer == null)
            return def;
        else
            return integer.intValue();
    }

    Collection getOwnSlotValues(Instance inst, String slotName) {
        Slot slot = getSlot(slotName);
        if (inst != null && slot != null)
            return inst.getOwnSlotValues(slot);
        else
            return Collections.EMPTY_LIST;
    }

    Collection getOwnSlotValuesNotEmpty(Instance inst, String slotName) {
        Slot slot = getSlot(slotName);
        if (inst != null && slot != null) {
            Collection values = inst.getOwnSlotValues(slot);
            if (values.isEmpty())
                report("no values for slot " + slotName + " on instance " + inst);
            return values;
        } else
            return Collections.EMPTY_LIST;
    }

    boolean equals(Cls cls, String clsName) {
        Cls cls1 = getCls(clsName);
        return cls.equals(cls1);
    }

    // reporting

    void report(String text) {
        if (itsTextArea != null) {
            itsTextArea.append(text);
            itsTextArea.append("\n");
        } else {
            System.out.println("### ");
            System.out.println(text);
        }
    }

}
