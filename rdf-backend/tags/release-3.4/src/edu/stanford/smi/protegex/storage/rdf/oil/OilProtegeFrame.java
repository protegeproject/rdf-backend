package edu.stanford.smi.protegex.storage.rdf.oil;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.walker.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;

public class OilProtegeFrame extends ProtegeFrame {

  public OilProtegeFrame(Frame frame, KnowledgeBase kb,
      Namespaces namespaces, FrameCreator creator) {
    super(frame, kb, namespaces, creator);
  }

  public boolean isThing() {
    return _frame.getName().equals("oil:Top") || super.isThing();
  }

  public boolean isStandardClass() {
    return _frame.getName().equals("oil:Class") || super.isStandardClass();
  }

  public boolean isStandardSlot() {
    return _frame.getName().equals("oil:Property") || super.isStandardSlot();
  }

}


