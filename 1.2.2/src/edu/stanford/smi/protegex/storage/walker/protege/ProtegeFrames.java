package edu.stanford.smi.protegex.storage.walker.protege;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.walker.*;

public class ProtegeFrames {

    public KnowledgeBase _kb;
    public Namespaces _namespaces;
    public Hashtable _wframes; // Frame -> ProtegeFrame (WalkerFrame)
    public FrameCreator _creator;

    public ProtegeFrames(KnowledgeBase kb, Namespaces namespaces, FrameCreator creator) {
        _kb = kb;
        _namespaces = namespaces;
        _wframes = new Hashtable();
        _creator = creator;
    }

    public ProtegeFrame getProtegeFrame(Frame frame) {
        ProtegeFrame protegeFrame = (ProtegeFrame) _wframes.get(frame);
        if (protegeFrame == null) { // we need a new one
            protegeFrame = newProtegeFrame(frame);
            _wframes.put(frame, protegeFrame);
        }
        return protegeFrame;
    }

    public ProtegeFrame newProtegeFrame(Frame frame) {
        return new ProtegeFrame(frame, _kb, _namespaces, _creator);
    }

    Object wframe(Object value) { // "walker" frame
        if (value == null)
            return null;
        else if (value instanceof Frame)
            return getProtegeFrame((Frame) value);
        else
            return value;
    }

    Collection wframes(Collection values) {
        ArrayList wframes = new ArrayList();
        for (Iterator vIterator = values.iterator(); vIterator.hasNext();) {
            Object value = vIterator.next();
            wframes.add(wframe(value));
        }
        return wframes;
    }

}
