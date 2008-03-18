package edu.stanford.smi.protegex.storage.walker.protege;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.CollectionUtilities;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.storage.rdf.RDFFrameCreator;
import edu.stanford.smi.protegex.storage.walker.*;

public class ProtegeFrameWalker implements FrameWalker {

    public KnowledgeBase _kb;
    public Namespaces _namespaces;
    public FrameCreator _creator;
    public ProtegeFrames _wframes;

    public ProtegeFrameWalker(KnowledgeBase kb, Namespaces namespaces) {
        _kb = kb;
        _namespaces = namespaces;
    }

    public void walk(FrameCreator frameCreator) {
        _creator = frameCreator;
        _wframes = newProtegeFrames();
        _creator.start();
        walkClasses();
        walkInstances();
        walkSlots();
        walkSlotAttachments();
        walkValues();
        _creator.finish();
    }

    public ProtegeFrames newProtegeFrames() {
        return new ProtegeFrames(_kb, _namespaces, _creator);
    }

    // classes

    void walkClasses() {
        Collection classes = _kb.getClses();
        for (Iterator classIterator = classes.iterator(); classIterator.hasNext();) {
            Cls cls = (Cls) classIterator.next();
            if (exportFrame(cls) && !rdfSystemFrame(cls)) {
                Cls type = cls.getDirectType();
                if (type == null) { // whould never happen!
                    System.err.println("WARNING: class has no direct type: " + cls);
                    type = _kb.getDefaultClsMetaCls();
                }
                Collection superclasses = cls.getDirectSuperclasses();
                boolean abstrct = cls.isAbstract();
                String documentation = getDocumentation(cls);
                _creator.createCls(wframe(cls), wframes(superclasses), wframe(type), abstrct, documentation);
            }
        }
    }

    // instances (without own slot values)
    // needed before slots because of template slot and default values

    void walkInstances() {
        Collection instances = _kb.getInstances();
        for (Iterator instanceIterator = instances.iterator(); instanceIterator.hasNext();) {
            Instance instance = (Instance) instanceIterator.next();
            if (instance instanceof SimpleInstance // no classes and slots
                // || instance instanceof Facet) // ??? FACET
                && exportFrame(instance)) {
            	
            	String documentation = getDocumentation(instance);
            	
            	//TT:adding support for multiple types            	
                Collection types = instance.getDirectTypes();
                                                
                if (types == null || types.size() == 0) { // whould never happen!
                    Log.getLogger().warning("Instance has no direct type: " + instance);
                    types = _kb.getRootClses(); // use :THING
                }
                
                // backwards compatibility
                if (!(_creator instanceof RDFFrameCreator)) {
                	_creator.createInstance(wframe(instance), wframe((Frame)CollectionUtilities.getFirstItem(types)), documentation);
                	return;
                }
                
                Collection wTypes = new ArrayList();
                
                for (Iterator iter = types.iterator(); iter.hasNext();) {
					Cls type = (Cls) iter.next();
				
					WalkerFrame  wType = wframe(type);
					wTypes.add(wType);
										
				}   
                
                ((RDFFrameCreator)_creator).createInstance(wframe(instance), wTypes, documentation);
                
            }
        }
    }

    // slots

    void walkSlots() {
        Collection slots = _kb.getSlots();
        for (Iterator slotIterator = slots.iterator(); slotIterator.hasNext();) {
            Slot slot = (Slot) slotIterator.next();
            if (exportFrame(slot)) {
                Cls type = slot.getDirectType();
                if (type == null) { // whould never happen!
                    System.err.println("WARNING: slot has no direct type: " + slot);
                    type = _kb.getDefaultSlotMetaCls();
                }
                Collection superslots = slot.getDirectSuperslots();
                Slot inverseSlot = slot.getInverseSlot();
                String documentation = getDocumentation(slot);
                ProtegeSlotRestriction slotRestriction = new ProtegeSlotRestriction(_wframes, slot);
                if (slotRestriction.isInstance() && _creator.singleAllowedClass()) {
                    // _creator wants a single allowed class
                    Collection allowedClasses = slot.getAllowedClses();
                    WalkerFrame commonSuperclass = wframe(getCommonSuperclass(allowedClasses));
                    slotRestriction.setAllowedClass(commonSuperclass);
                }
                Facet associatedFacet = slot.getAssociatedFacet(); // FACET
                _creator.createSlot(
                    wframe(slot),
                    wframe(type),
                    wframes(superslots),
                    wframe(inverseSlot),
                    wframe(associatedFacet),
                    slotRestriction,
                    documentation);
            }
        }
    }

    // slot attachments

    void walkSlotAttachments() {
        Collection classes = _kb.getClses();
        for (Iterator classIterator = classes.iterator(); classIterator.hasNext();) {
            Cls cls = (Cls) classIterator.next();
            if (exportFrame(cls)) {
                Collection templateSlots = cls.getTemplateSlots();
                for (Iterator tsIterator = templateSlots.iterator(); tsIterator.hasNext();) {
                    Slot templateSlot = (Slot) tsIterator.next();
                    if (cls.hasDirectTemplateSlot(templateSlot)
                        || (hasDirectlyOverriddenTemplateSlot(cls, templateSlot))) {
                        if (!exportFrame(templateSlot)) {
                            // this breaks the restrictions of included slots
                            // so do this only for system slots:
                            if (templateSlot.isSystem()) { // e.g., :NAME
                                // this slot has not been exported, so do it now;
                                // the type is enough, the rest is in the imported project
                                Cls type = templateSlot.getDirectType();
                                _creator.createSlot(wframe(templateSlot), wframe(type), null, null, null, null, null);
                            }
                        }
                        ProtegeSlotRestriction overriddenSlotRestriction = null;
                        String overriddenDocumentation = null;
                        // see above for hasOverriddenTemplateSlot!!!
                        if (hasDirectlyOverriddenTemplateSlot(cls, templateSlot)) {
                            overriddenSlotRestriction = new ProtegeSlotRestriction(_wframes, cls, templateSlot);
                            if (overriddenSlotRestriction.isInstance() && _creator.singleAllowedClass()) {
                                // _creator wants a single allowed class
                                Collection allowedClasses = cls.getTemplateSlotAllowedClses(templateSlot);
                                WalkerFrame commonSuperclass = wframe(getCommonSuperclass(allowedClasses));
                                overriddenSlotRestriction.setAllowedClass(commonSuperclass);
                            }
                            Collection docu = cls.getTemplateSlotDocumentation(templateSlot);
                            overriddenDocumentation = getDocumentation(docu);
                        }
                        _creator.attachSlot(
                            wframe(cls),
                            wframe(templateSlot),
                            cls.hasDirectTemplateSlot(templateSlot),
                            overriddenSlotRestriction,
                            overriddenDocumentation);
                    }
                }
            }
        }
    }

    // own slot values (for all instances incl. classes and slots)

    void walkValues() {
        Collection instances = _kb.getInstances();
        for (Iterator instanceIterator = instances.iterator(); instanceIterator.hasNext();) {
            Instance instance = (Instance) instanceIterator.next();
            if (exportFrame(instance)) { // ALL instances (even classes etc.)
                WalkerFrame instanceFrame = wframe(instance);
                Collection ownSlots = instance.getOwnSlots();
                for (Iterator osIterator = ownSlots.iterator(); osIterator.hasNext();) {
                    Slot slot = (Slot) osIterator.next();
                    // if (!slot.isSystem()) { // not correct ??? !!! XXX
                    if (exportSlotValues(slot)) {
                        // (we have to check for the "missing" system properties
                        // like label and isDefinedBy ... !!!
                        Collection values = instance.getOwnSlotValues(slot);
                        if (values != null && !values.isEmpty())
                            _creator.addOwnSlotValues(instanceFrame, wframe(slot), wframes(values));
                    }
                }
            }
        }
    }

    // auxiliaries

    WalkerFrame wframe(Frame frame) {
        return (WalkerFrame) _wframes.wframe(frame);
    }

    Collection wframes(Collection frames) {
        return _wframes.wframes(frames);
    }

    public boolean exportFrame(Frame frame) {
        return !frame.isIncluded();
    }

    public boolean exportSlotValues(Slot slot) {
        String name = slot.getName();
        return !slot.isSystem()
        || name.equals(Model.Slot.ANNOTATED_INSTANCE)
        || name.equals(Model.Slot.ANNOTATION_TEXT)
        || name.equals(Model.Slot.CREATION_TIMESTAMP)
        || name.equals(Model.Slot.CREATOR)
        || name.equals(Model.Slot.CONSTRAINTS) 
        || name.startsWith(":PAL-")
        || name.equals(Model.Slot.FROM)
        || name.equals(Model.Slot.TO);
    }

    boolean rdfSystemFrame(Frame frame) {
        // this allows us to exclude rdf[s] classes to be exported
        // (esp. those from projects created with the old backend,
        //  but also those created on load like rdfs:Literal)
        String name = frame.getName();
        // we assume that the namespace abbreviations rdf and rdfs are
        // NEVER used as user namespaces!!!
        return name.startsWith("rdf:") || name.startsWith("rdfs:");
        // handle RDF Helper and URI???
    }

    String getDocumentation(Frame frame) {
        Collection docu = frame.getDocumentation();
        return getDocumentation(docu);
    }

    String getDocumentation(Collection docu) {
        if (docu == null || docu.isEmpty())
            return null;
        else if (docu.size() == 1)
            return (String) docu.iterator().next();
        else { // hack??? does this ever happen?
            StringBuffer documentation = new StringBuffer();
            for (Iterator docuIterator = docu.iterator(); docuIterator.hasNext();) {
                String docuString = (String) docuIterator.next();
                documentation.append(docuString);
                if (docuIterator.hasNext())
                    documentation.append("\n");
            }
            return documentation.toString();
        }
    }

    Cls getCommonSuperclass(Collection classes) {
        if (classes == null || classes.isEmpty())
            return null;
        else if (classes.size() == 1)
            return (Cls) classes.iterator().next();
        else {
            // 1. find common intersection of superclasses
            HashSet intersection = null;
            for (Iterator classIterator = classes.iterator(); classIterator.hasNext();) {
                Cls cls = (Cls) classIterator.next();
                HashSet superclasses = new HashSet(cls.getSuperclasses());
                superclasses.add(cls);
                if (intersection == null)
                    intersection = superclasses;
                else // intersect
                    intersection.retainAll(superclasses);
            }
            // 2. remove all classes that are superclasses of any other class
            if (intersection.size() > 1) {
                HashSet copiedIntersection = (HashSet) intersection.clone();
                for (Iterator classIterator = copiedIntersection.iterator(); classIterator.hasNext();) {
                    Cls cls = (Cls) classIterator.next();
                    intersection.removeAll(cls.getSuperclasses());
                    if (intersection.size() == 1) // finished
                        break;
                }
            }
            // 3. pick the first one (or: minimize path length ...)
            if (!intersection.isEmpty())
                return (Cls) intersection.iterator().next();
            else
                return null;
        }
    }

    boolean hasDirectlyOverriddenTemplateSlot(Cls cls, Slot templateSlot) {
        // because of a bug in Protege we have to exclude :ROLE 
        // (and also :DIRECT-TYPE)
        // remove this if bug is fixed ... !!!
        // (we cannot use "&& hasOverriddenTemplateSlot" since this also
        // has a bug when going back to the original slot definition)
        // this is not correct if someone really wants to override :ROLE!!!
        return cls.hasDirectlyOverriddenTemplateSlot(templateSlot)
            && !templateSlot.getName().equals(":ROLE")
            && !templateSlot.getName().equals(":DIRECT-TYPE");
    }

}
