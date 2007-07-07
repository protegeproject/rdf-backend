package edu.stanford.smi.protegex.storage.walker.protege;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.rdf.*;
import edu.stanford.smi.protegex.storage.walker.*;

public class ProtegeFrame implements WalkerFrame {

    public Frame _frame;
    public String _namespace;
    public String _localName;

    static final String SYSTEMNAMESPACE = "http://protege.stanford.edu/system#";

    public ProtegeFrame(Frame frame, KnowledgeBase kb, Namespaces namespaces, FrameCreator creator) {
        _frame = frame;
        init(kb, namespaces, creator);
    }

    public String getName() { // full unique name
        return _frame.getName();
    }

    public String getDisplayName() { // for GUIs/browsers etc.
        return _frame.getBrowserText();
    }

    public String getNamespace() {
        return _namespace;
    }

    public String getLocalName() {
        return _localName;
    }

    public String toString() {
        return getName();
    }

    public boolean isClass() {
        return _frame instanceof Cls;
    }

    public boolean isSlot() {
        return _frame instanceof Slot;
    }

    public boolean isFacet() {
        return _frame instanceof Facet;
    }

    public boolean isThing() {
        return _frame.getName().equals(":THING"); // || :SYSTEM-CLASS ??
    }

    public boolean isStandardClass() {
        return _frame.getName().equals(":STANDARD-CLASS") || _frame.getName().equals(":CLASS");
        // ??? we have to map it!

    }

    public boolean isStandardSlot() {
        return _frame.getName().equals(":STANDARD-SLOT") || _frame.getName().equals(":SLOT"); // ??? we have to map it!
    }

    // ... map remaining system frames (:CONSTRAINT, :FACET, ...) ???
    // no -> done in renameSystemFrame, see below

    void init(KnowledgeBase kb, Namespaces namespaces, FrameCreator creator) {
        if (_frame.isSystem()) {
            renameSystemFrame(); // system namespace, lower case etc.
        } else {
            String name = _frame.getName();
            // if (isSlot() || isFacet())
                name = renameSlotName(name, kb, creator);
            // ... rename other frame names (e.g., class frame names) ... !!!
            int p = name.indexOf(':');
            if (p == -1) {
                _namespace = namespaces.getDefaultNamespace();
                _localName = name;
            } else {
                String abbrev = name.substring(0, p);
                String uri = namespaces.getURI(abbrev);
                if (uri != null) {
                    _namespace = uri;
                    _localName = name.substring(p + 1);
                } else { // : did not indicate a namespace
                    _namespace = namespaces.getDefaultNamespace();
                    _localName = name;
                }
            }
            // see if we are renaming this namespace
            if (kb instanceof RDFKnowledgeBase && _frame.isIncluded()) {
                String newNamespace = ((RDFKnowledgeBase) kb).getNewNamespace(_frame, _namespace);
                if (newNamespace != null)
                    _namespace = newNamespace;
            }
        }
    }

    String renameSlotName(String name, KnowledgeBase kb, FrameCreator creator) {
        // for frame names that can be used as properties (i.e., slots and facets),
        // replace all characters not allowed in element names by '_'
        // see http://www.w3.org/TR/2000/REC-xml-20001006 for
        // definition of element names ("Name" production)
        StringBuffer buffer = new StringBuffer();
        int l = name.length();
        for (int i = 0; i < l; i++) {
            char c = name.charAt(i);
            if (c == '.' || c == '-' || c == '_' || c == ':' || Character.isLetterOrDigit(c))
                // || CombiningChar || Extender ... !!!
                buffer.append(c);
            else
                buffer.append('_');
        }
        String newName = buffer.toString();
        char c = newName.charAt(0);
        if (!(Character.isLetter(c) || c == '_' || c == ':'))
            newName = "_" + newName;
        if (newName.equals(name))
            return name;
        while (kb.getFrame(newName) != null) // make unique
            newName = newName + "_";
        // activate this if Protege allows warnings
        // creator.error("frame name changed from \"" + name + "\" to \"" +
        //   newName + "\"");
        System.err.println("WARNING: frame name changed from \"" + name + "\" to \"" + newName + "\"");
        return newName;
    }

    void renameSystemFrame() {
        String name = _frame.getName();
        _namespace = SYSTEMNAMESPACE;
        int l = name.length();
        StringBuffer newName = new StringBuffer();
        for (int i = 0; i < l; i++) {
            char c = name.charAt(i);
            if (c == ':' || c == '-')
                newName.append('_');
            else
                newName.append(Character.toLowerCase(c));
        }
        _localName = newName.toString();
    }

}
