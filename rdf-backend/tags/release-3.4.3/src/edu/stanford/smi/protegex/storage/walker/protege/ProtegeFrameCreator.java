package edu.stanford.smi.protegex.storage.walker.protege;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.walker.*;

public class ProtegeFrameCreator implements FrameCreator {

    public KnowledgeBase _kb;
    Namespaces _namespaces;
    boolean _included;
    Collection _errors;
    // Cls _undefinedClass;

    public ProtegeFrameCreator(KnowledgeBase kb, Namespaces namespaces, boolean included, Collection errors) {
        _kb = kb;
        _namespaces = namespaces;
        _included = included;
        _errors = errors;
    }

    public void start() {
    }

    private static int count;
    private static void printCreateCount(WalkerFrame o) {
        if (++count % 100 == 0) {
            // System.out.println(count + ": create " + o);
        }
    }

    public void createCls(
        WalkerFrame clsFrame,
        Collection superclasses,
        WalkerFrame typeFrame,
        boolean abstrct,
        String documentation) {
        // System.out.println("createCls " + clsFrame + " : " + typeFrame + " :: " + superclasses);
        printCreateCount(clsFrame);
        Cls cls = getCls(clsFrame);
        if (typeFrame != null) {
            Cls type = getMetaCls(typeFrame);
            if (!cls.getDirectType().equals(type)) {
                makeConcrete(type);
                cls.setDirectType(type);
                // System.out.println("set direct type: " + type);
            }
        }
        // add superclasses (delete old ones?) ... !!!
        if (superclasses == null || superclasses.isEmpty())
            cls.addDirectSuperclass(getThing());
        else {
            Collection superClassesToRemove = new ArrayList(cls.getDirectSuperclasses());
            for (Iterator scIterator = superclasses.iterator(); scIterator.hasNext();) {
                WalkerFrame superclass = (WalkerFrame) scIterator.next();
                Cls superCls = getCls(superclass);
                boolean removed = superClassesToRemove.remove(superCls);
                if (!removed) {
                    cls.addDirectSuperclass(superCls);
                }
            }
            Iterator i = superClassesToRemove.iterator();
            while (i.hasNext()) {
                Cls superClass = (Cls) i.next();
                cls.removeDirectSuperclass(superClass);
            }
        }
        cls.setAbstract(abstrct);
        if (documentation != null)
            cls.setDocumentation(documentation);
    }

    public boolean singleAllowedClass() {
        return false; // we are fine with multiple allowed classes
    }

    public void createInstance(WalkerFrame instFrame, WalkerFrame typeFrame, String documentation) {
        printCreateCount(instFrame);
        // System.out.println("createInstance " + instFrame + " : " + typeFrame);
        Instance instance = getInstance(instFrame, typeFrame);
        if (typeFrame != null) {
            Cls type = getCls(typeFrame);
            if (!instance.getDirectType().equals(type)) {
                // make sure we do not try to convert a simple instance
                // into a slot or class, etc.
                makeConcrete(type);
                instance.setDirectType(type);
                // System.out.println("setting type: " + type);
            }
        }
        if (documentation != null)
            instance.setDocumentation(documentation);
    }

    public void createSlot(
        WalkerFrame slotFrame,
        WalkerFrame typeFrame,
        Collection superslots,
        WalkerFrame inverseSlot,
        WalkerFrame associatedFacet,
    // FACET
    WalkerSlotRestriction slotRestriction, String documentation) {
        // System.out.println("createSlot " + slotFrame + " : " + typeFrame
        //   + " :: " + superslots);
        Slot slot = getSlot(slotFrame);
        // type
        if (typeFrame != null) {
            Cls type = getCls(typeFrame);
            if (!slot.getDirectType().equals(type)) {
                makeConcrete(type);
                slot.setDirectType(type);
                // System.out.println("set direct type: " + type);
            }
        }
        // superslots
        if (superslots != null) {
            for (Iterator superslotIterator = superslots.iterator(); superslotIterator.hasNext();) {
                WalkerFrame superslot = (WalkerFrame) superslotIterator.next();
                slot.addDirectSuperslot(getSlot(superslot));
            }
        }
        // inverse slot
        if (inverseSlot != null)
            slot.setInverseSlot(getSlot(inverseSlot));
        // FACET
        // associated facet
        if (associatedFacet != null)
            slot.setAssociatedFacet(getFacet(associatedFacet, slot));
        // slot restriction
        if (slotRestriction != null)
            restrictSlot(slot, slotRestriction);
        // documentation
        if (documentation != null)
            slot.setDocumentation(documentation);
    }

    void restrictSlot(Slot slot, WalkerSlotRestriction slotRestriction) {

        // value type
        if (slotRestriction.isAny()) {
            slot.setValueType(ValueType.ANY);
        } else if (slotRestriction.isBoolean()) {
            slot.setValueType(ValueType.BOOLEAN);
        } else if (slotRestriction.isClass()) {
            slot.setValueType(ValueType.CLS);
            Collection allowedParents = slotRestriction.getAllowedParents();
            if (allowedParents == null)
                allowedParents = Collections.EMPTY_LIST;
            slot.setAllowedParents(getClses(allowedParents));
        } else if (slotRestriction.isFloat()) {
            slot.setValueType(ValueType.FLOAT);
        } else if (slotRestriction.isInstance()) {
            slot.setValueType(ValueType.INSTANCE);
            Collection allowedClasses = slotRestriction.getAllowedClasses();
            if (allowedClasses == null)
                allowedClasses = Collections.EMPTY_LIST;
            slot.setAllowedClses(getClses(allowedClasses));
        } else if (slotRestriction.isInteger()) {
            slot.setValueType(ValueType.INTEGER);
        } else if (slotRestriction.isString()) {
            slot.setValueType(ValueType.STRING);
        } else if (slotRestriction.isSymbol()) {
            slot.setValueType(ValueType.SYMBOL);
            Collection allowedValues = slotRestriction.getAllowedValues();
            if (allowedValues == null)
                allowedValues = Collections.EMPTY_LIST;
            slot.setAllowedValues(allowedValues);
        }

        // cardinalities
        int minCard = slotRestriction.getMinimumCardinality();
        slot.setMinimumCardinality(minCard);
        int maxCard = slotRestriction.getMaximumCardinality();
        slot.setMaximumCardinality(maxCard);

        // for numbers: min and max values
        // the current GUI also allows min/max values for all value types,
        // but we don't:
        if (slotRestriction.isInteger() || slotRestriction.isFloat()) {
            // is prepared to also handle string-valued slots
            // since we use String and not Number like Protege!
            String minVal = slotRestriction.getMinimumValue();
            String maxVal = slotRestriction.getMaximumValue();
            slot.setMinimumValue((Number) getValue(minVal, slot.getValueType()));
            slot.setMaximumValue((Number) getValue(maxVal, slot.getValueType()));
        }

        // default and slot values
        Collection defaultValues = slotRestriction.getDefaultValues();
        if (defaultValues == null)
            defaultValues = Collections.EMPTY_LIST;
        slot.setDefaultValues(getValues(defaultValues, slot.getValueType()));
        Collection values = slotRestriction.getValues();
        if (values == null)
            values = Collections.EMPTY_LIST;
        slot.setValues(getValues(values, slot.getValueType()));

        // FACET
        // user-defined facets not used for global slot definitions!

    }

    public void attachSlot(
        WalkerFrame clsFrame,
        WalkerFrame slotFrame,
        boolean direct,
        WalkerSlotRestriction overriddenSlotRestriciton,
        String overriddenDocumentation) {
        // System.out.println("attachSlot " + clsFrame + " " + slotFrame);
        Cls cls = getCls(clsFrame);
        Slot slot = getSlot(slotFrame);
        if (direct)
            cls.addDirectTemplateSlot(slot);
        if (overriddenSlotRestriciton != null) {
            restrictSlot(cls, slot, overriddenSlotRestriciton);
        }
        if (overriddenDocumentation != null) {
            cls.setTemplateSlotDocumentation(slot, overriddenDocumentation);
        }
    }

    void restrictSlot(Cls cls, Slot slot, WalkerSlotRestriction slotRestriction) {

        // same as in restrictSlot above (but: copy needed since we
        // here have cls/slot pairs!);
        // at the moment, we assume that the slotRestriction contains
        // ALL information, not just the overridden one;
        // we leave it to Protege to decide which information is the
        // overriding one!

        // value type
        if (slotRestriction.isAny()) {
            cls.setTemplateSlotValueType(slot, ValueType.ANY);
        } else if (slotRestriction.isBoolean()) {
            cls.setTemplateSlotValueType(slot, ValueType.BOOLEAN);
        } else if (slotRestriction.isClass()) {
            cls.setTemplateSlotValueType(slot, ValueType.CLS);
            Collection allowedParents = slotRestriction.getAllowedParents();
            if (allowedParents == null)
                allowedParents = Collections.EMPTY_LIST;
            cls.setTemplateSlotAllowedParents(slot, getClses(allowedParents));
        } else if (slotRestriction.isFloat()) {
            cls.setTemplateSlotValueType(slot, ValueType.FLOAT);
        } else if (slotRestriction.isInstance()) {
            cls.setTemplateSlotValueType(slot, ValueType.INSTANCE);
            Collection allowedClasses = slotRestriction.getAllowedClasses();
            if (allowedClasses == null)
                allowedClasses = Collections.EMPTY_LIST;
            cls.setTemplateSlotAllowedClses(slot, getClses(allowedClasses));
        } else if (slotRestriction.isInteger()) {
            cls.setTemplateSlotValueType(slot, ValueType.INTEGER);
        } else if (slotRestriction.isString()) {
            cls.setTemplateSlotValueType(slot, ValueType.STRING);
        } else if (slotRestriction.isSymbol()) {
            cls.setTemplateSlotValueType(slot, ValueType.SYMBOL);
            Collection allowedValues = slotRestriction.getAllowedValues();
            if (allowedValues == null)
                allowedValues = Collections.EMPTY_LIST;
            cls.setTemplateSlotAllowedValues(slot, allowedValues);
        }

        // cardinalities
        int minCard = slotRestriction.getMinimumCardinality();
        cls.setTemplateSlotMinimumCardinality(slot, minCard);
        int maxCard = slotRestriction.getMaximumCardinality();
        cls.setTemplateSlotMaximumCardinality(slot, maxCard);

        // for numbers: min and max values
        if (slotRestriction.isInteger() || slotRestriction.isFloat()) { // ANY?
            String minVal = slotRestriction.getMinimumValue();
            String maxVal = slotRestriction.getMaximumValue();
            cls.setTemplateSlotMinimumValue(slot, (Number) getValue(minVal, slot.getValueType()));
            cls.setTemplateSlotMaximumValue(slot, (Number) getValue(maxVal, slot.getValueType()));
        }

        // default and template slot values
        Collection defaultValues = slotRestriction.getDefaultValues();
        if (defaultValues == null)
            defaultValues = Collections.EMPTY_LIST;
        cls.setTemplateSlotDefaultValues(slot, getValues(defaultValues, slot.getValueType()));
        Collection values = slotRestriction.getValues();
        if (values == null)
            values = Collections.EMPTY_LIST;
        cls.setTemplateSlotValues(slot, getValues(values, slot.getValueType()));

        // FACET
        // user-defined facets
        Map facetOverrides = slotRestriction.getFacetOverrides();
        if (facetOverrides != null) {
            for (Iterator facetOverridesIterator = facetOverrides.entrySet().iterator();
                facetOverridesIterator.hasNext();
                ) {
                Map.Entry facetOverride = (Map.Entry) facetOverridesIterator.next();
                WalkerFrame facet = (WalkerFrame) facetOverride.getKey();
                Collection facetValues = (Collection) facetOverride.getValue();
                Facet protegeFacet = getFacet(facet); // check errors here ... !!!
                Slot facetSlot = protegeFacet.getAssociatedSlot();
                ValueType facetSlotValueType = facetSlot.getValueType();
                cls.setTemplateFacetValues(slot, protegeFacet, getValues(facetValues, facetSlotValueType));
            }
        }

    }

    private static int addCount;
    public void addOwnSlotValues(WalkerFrame instanceFrame, WalkerFrame slotFrame, Collection values) {
        // System.out.println("addOwnSlotValues " + instanceFrame + "." + slotFrame
        //   + " = " + values);
        Instance instance = getInstance(instanceFrame);
        Slot slot = getSlot(slotFrame);
        if (!instance.hasOwnSlot(slot)) {
            error(instanceFrame + " does not have own slot " + slotFrame);
            /* does not correctly work:
            // simply add it to type:
            // (don't do this for system classes!)
            Cls type = instance.getDirectType();
            type.addDirectTemplateSlot(slot);
            // guess range for slot ... !!!
            error("template slot " + slot + " was added to " + type +
            " (" + instanceFrame + " needed it)");
            */
        } else {
            ValueType valueType = instance.getOwnSlotValueType(slot);
            Collection pvalues = getValues(values, valueType);
            if (++addCount % 100 == 0) {
                // System.out.                println(addCount + " add " + instance + " " + slot + " " + pvalues);
            }
            for (Iterator vIterator = pvalues.iterator(); vIterator.hasNext();) {
                Object pvalue = vIterator.next();
                instance.addOwnSlotValue(slot, pvalue);
                
            }
        }
    }

    public void finish() {
        // clean up some stuff:
        // set superclass of classes without superclasses
        Collection classes = _kb.getClses();
        for (Iterator classIterator = classes.iterator(); classIterator.hasNext();) {
            Cls cls = (Cls) classIterator.next();
            Collection superclasses = cls.getDirectSuperclasses();
            if (superclasses == null
                || superclasses.isEmpty()
                && !cls.isSystem()) // don't do this with :THING!! (???!!!)
                cls.addDirectSuperclass(getUndefinedClass());
        }
    }

    // Protege basic frame handling

    // IMPORTANT: all createSomething methods must be wrapped
    // in edu.stanford.smi.protegex.storage.rdf.RDFKnowledgeBase !!!

    Cls getCls(WalkerFrame frame) {
        Frame specialFrame = getSpecialFrame(frame);
        if (specialFrame != null)
            return (Cls) specialFrame; // check if it is a class!
        else {
            String name = frameName(frame);
            Cls cls = _kb.getCls(name);
            if (cls != null)
                return cls;
            else {
                Cls type = getStandardClass();
                cls = _kb.createCls(name, Collections.EMPTY_LIST, type);
                // superclasses MUST be added later! (see finish())
                cls.setIncluded(_included);
                return cls;
            }
        }
    }

    Cls getMetaCls(WalkerFrame frame) {
        Cls cls = getCls(frame);
        if (!cls.isClsMetaCls()) {
            cls.addDirectSuperclass(getStandardClass());
        }
        return cls;
    }

    Collection getClses(Collection wframes) {
        ArrayList classes = new ArrayList();
        for (Iterator wframeIterator = wframes.iterator(); wframeIterator.hasNext();) {
            WalkerFrame wframe = (WalkerFrame) wframeIterator.next();
            classes.add(getCls(wframe));
        }
        return classes;
    }

    Slot getSlot(WalkerFrame frame) {
        Frame specialFrame = getSpecialFrame(frame);
        if (specialFrame != null)
            return (Slot) specialFrame; // check if it is a slot!
        else {
            String name = frameName(frame);
            Slot slot = _kb.getSlot(name);
            if (slot != null)
                return slot;
            else {
                Cls type = getStandardSlot();
                slot = _kb.createSlot(name, type);
                // make it "unrestricted", i.e. multiple any (and not single string)
                slot.setValueType(ValueType.ANY);
                slot.setMaximumCardinality(KnowledgeBase.MAXIMUM_CARDINALITY_UNBOUNDED);
                slot.setIncluded(_included);
                return slot;
            }
        }
    }

    // FACET
    Facet getFacet(WalkerFrame frame) {
        Frame specialFrame = getSpecialFrame(frame);
        if (specialFrame != null)
            return (Facet) specialFrame; // check if it is a facet!
        else {
            String name = frameName(frame);
            Facet facet = _kb.getFacet(name);
            if (facet != null) {
                Slot slot = facet.getAssociatedSlot();
                if (slot == null)
                    error("facet not correctly defined: " + frame);
                return facet;
            } else {
                error("facet not defined: " + frame);
                return null;
            }
        }
    }

    Facet getFacet(WalkerFrame frame, Slot slot) {
        Frame specialFrame = getSpecialFrame(frame);
        if (specialFrame != null)
            return (Facet) specialFrame; // check if it is a facet!
        else {
            String name = frameName(frame);
            Facet facet = _kb.getFacet(name);
            if (facet != null) {
                facet.setAssociatedSlot(slot); // check old value ??? !!!
                return facet;
            } else {
                Cls type = getStandardFacet();
                facet = _kb.createFacet(name, type);
                facet.setAssociatedSlot(slot);
                facet.setIncluded(_included);
                return facet;
            }
        }
    }

    Instance getInstance(WalkerFrame frame) {
        return getInstance(frame, null);
    }

    Instance getInstance(WalkerFrame frame, WalkerFrame typeFrame) {
        // works for simple instances AND classes/slots, but not facets!
        Instance instance = (Instance) getSpecialFrame(frame);
        if (instance != null)
            return instance;
        else {
            String name = frameName(frame);
            instance = _kb.getInstance(name);
            if (instance != null)
                return instance;
            else {
                Cls type = (typeFrame == null) ? null : getCls(typeFrame);
                if (type == null) {
                    // this is important since one cannot change a simple
                    // instance into a slot or class, etc.
                    if (frame.isClass())
                        type = getStandardClass();
                    else if (frame.isSlot())
                        type = getStandardSlot();
                    else
                        type = getThing();
                }
                // makeConcrete(type);
                instance = _kb.createInstance(name, type);
                instance.setIncluded(_included);
                return instance;
            }
        }
    }

    Object getValue(Object value, ValueType valueType) {
        // convert simple values (i.e., no WalkerFrames)
        if (value == null)
            return null;
        String valueString = value.toString();
        if (valueType.equals(ValueType.ANY)) {
            return value; // or: valueString ... ???
        } else if (valueType.equals(ValueType.BOOLEAN)) {
            if (valueString.trim().toLowerCase().startsWith("t"))
                // catches true, TRUE, True, ...
                return Boolean.TRUE;
            else
                return Boolean.FALSE;
        } else if (valueType.equals(ValueType.FLOAT)) {
            try {
                return new Float(valueString);
            } catch (Exception e) {
                error("expected a float value: " + value);
                return new Float(0);
            }
        } else if (valueType.equals(ValueType.INTEGER)) {
            try {
                // return new Integer(valueString);
                return new Integer(new Float(valueString).intValue());
            } catch (Exception e) {
                error("expected an integer value: " + value);
                return new Integer(0);
            }
        } else { // STRING, SYMBOL -> String
            return valueString;
        }
    }

    Collection getValues(Collection values, ValueType valueType) {
        // handles WalkerFrames and normal values!
        ArrayList returnValues = new ArrayList();
        for (Iterator valueIterator = values.iterator(); valueIterator.hasNext();) {
            Object value = valueIterator.next();
            if (value instanceof WalkerFrame) {
                if (valueType != ValueType.ANY && valueType != ValueType.INSTANCE && valueType != ValueType.CLS)
                    error("value must be of type " + valueType + ": " + value);
                else
                    returnValues.add(getInstance((WalkerFrame) value));
            } else {
                if (valueType == ValueType.INSTANCE || valueType == ValueType.CLS)
                    error("value must be of type " + valueType + ": " + value);
                else
                    returnValues.add(getValue(value, valueType));
            }
        }
        return returnValues;
    }

    void makeConcrete(Cls type) {
        if (type.isAbstract())
            type.setAbstract(false);
    }

    // handling of special frames

    Frame getSpecialFrame(WalkerFrame frame) {
        if (frame.isThing())
            return getThing();
        else if (frame.isStandardClass())
            return getStandardClass();
        else if (frame.isStandardSlot())
            return getStandardSlot();
        else // add here ... FACET ???? !!!
            return null;
    }

    // return special frames
    // subclass these for specialized back ends!

    public Cls getThing() {
        return _kb.getCls(":THING");
    }

    public Cls getStandardClass() {
        return _kb.getCls(":STANDARD-CLASS");
    }

    public Cls getStandardSlot() {
        return _kb.getCls(":STANDARD-SLOT");
    }

    // FACET
    public Cls getStandardFacet() {
        return _kb.getCls(":STANDARD-FACET");
    }

    public Cls getUndefinedClass() {
        return getThing();
        /*
        if (_undefinedClass == null) {
          _undefinedClass = _kb.createCls(":UNDEFINED", Collections.EMPTY_LIST);
          _undefinedClass.addDirectSuperclass(getThing());
          _undefinedClass.setIncluded(_included);
        }
        return _undefinedClass;
        */
    }

    // frame name

    String frameName(WalkerFrame walkerFrame) {
        return _namespaces.getFrameName(walkerFrame);
    }

    // error handling

    public void error(String message) {
        _errors.add(message);
    }

    void error(Exception exc) {
        error(exc.toString());
        exc.printStackTrace();
    }

    //added to support multiple types
	public void addTypesToInstance(WalkerFrame wframe, Collection wTypes) {
		
		Instance instance = getInstance(wframe);
		
		for (Iterator iter = wTypes.iterator(); iter.hasNext();) {
			WalkerFrame wType = (WalkerFrame) iter.next();
		
			Cls type = getCls(wType);
			
			if (type != null && !instance.hasType(type)) {
				instance.addDirectType(type);
			}
		}
		
	}

}
