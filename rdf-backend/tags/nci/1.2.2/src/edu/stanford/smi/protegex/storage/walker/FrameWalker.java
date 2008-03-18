package edu.stanford.smi.protegex.storage.walker;


public interface FrameWalker {

  public void walk(FrameCreator frameCreator);
    // must call start() at beginning and finish() and end
    // order of the calls: see ProtegeFrameWalker

}


