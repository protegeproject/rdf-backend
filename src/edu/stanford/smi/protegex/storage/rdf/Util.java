package edu.stanford.smi.protegex.storage.rdf;

import java.util.*;

import org.w3c.rdf.model.*;

public class Util { // proposed extensions for RDFUtil [MS]

    public static void filterModel(Model model, Model targetModel, ModelFilter filter) throws ModelException {
        NodeFactory nodeFactory = targetModel.getNodeFactory();
        for (Enumeration statementEnum = model.elements(); statementEnum.hasMoreElements();) {
            Statement statement = (Statement) statementEnum.nextElement();
            Collection statements = filter.getStatements(statement, nodeFactory);
            if (statements != null) {
                for (Iterator statementIterator = statements.iterator(); statementIterator.hasNext();) {
                    targetModel.add((Statement) statementIterator.next());
                }
            }
        }
    }

}
