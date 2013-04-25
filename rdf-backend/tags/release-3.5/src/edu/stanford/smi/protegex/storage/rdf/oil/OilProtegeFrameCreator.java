package edu.stanford.smi.protegex.storage.rdf.oil;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;


public class OilProtegeFrameCreator extends ProtegeFrameCreator {

  public OilProtegeFrameCreator(KnowledgeBase kb, Namespaces namespaces,
      boolean included, Collection errors) {
    super(kb, namespaces, included, errors);
  }

  public Cls getThing() {
    return _kb.getCls("oil:Top");
  }

  public Cls getStandardClass() {
    return _kb.getCls("oil:Class");
  }

  public Cls getStandardSlot() {
    return _kb.getCls("oil:Property");
  }

}

