package edu.stanford.smi.protegex.storage.rdf.oil;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.walker.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;


public class OilProtegeFrames extends ProtegeFrames {

  public OilProtegeFrames(KnowledgeBase kb, Namespaces namespaces, 
      FrameCreator creator) {
    super(kb, namespaces, creator);
  }

  public ProtegeFrame newProtegeFrame(Frame frame) {
    return new OilProtegeFrame(frame, _kb, _namespaces, _creator);
  }

}


