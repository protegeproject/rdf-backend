package edu.stanford.smi.protegex.storage.rdf;

import java.util.*;

import org.w3c.rdf.model.*;

public interface ModelFilter {

    public Collection getStatements(Statement statement, NodeFactory nodeFactory) throws ModelException;

}
