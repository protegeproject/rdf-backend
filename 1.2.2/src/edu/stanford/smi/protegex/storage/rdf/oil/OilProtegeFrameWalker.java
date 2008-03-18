package edu.stanford.smi.protegex.storage.rdf.oil;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;


public class OilProtegeFrameWalker extends ProtegeFrameWalker {

  public OilProtegeFrameWalker(KnowledgeBase kb, Namespaces namespaces) {
    super(kb, namespaces);
  }

  public ProtegeFrames newProtegeFrames() {
    return new OilProtegeFrames(_kb, _namespaces, _creator);
  }


}


