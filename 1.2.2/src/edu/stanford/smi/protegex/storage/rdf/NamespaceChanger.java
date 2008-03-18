package edu.stanford.smi.protegex.storage.rdf;

import java.util.*;

import org.w3c.rdf.model.*;

public class NamespaceChanger implements ModelFilter {

    String _oldNamespace;
    String _newNamespace;

    public NamespaceChanger(String oldNamespace, String newNamespace) {
        _oldNamespace = oldNamespace;
        _newNamespace = newNamespace;

    }

    public Collection getStatements(Statement statement, NodeFactory nodeFactory) throws ModelException {
        Vector statements = new Vector(1);
        statements.add(getStatement(statement, nodeFactory));
        return statements;
    }

    Statement getStatement(Statement statement, NodeFactory nodeFactory) throws ModelException {
        Resource oldSubject = statement.subject();
        Resource oldPredicate = statement.predicate();
        RDFNode oldObject = statement.object();
        Resource subject = getResource(oldSubject, nodeFactory);
        Resource predicate = getResource(oldPredicate, nodeFactory);
        RDFNode object = getNode(oldObject, nodeFactory);
        if (oldSubject != subject || oldPredicate != predicate || oldObject != object) // something has changed
            return nodeFactory.createStatement(subject, predicate, object);
        else
            return statement;
    }

    RDFNode getNode(RDFNode node, NodeFactory nodeFactory) throws ModelException {
        if (node instanceof Resource)
            return getResource((Resource) node, nodeFactory);
        else
            return node;
    }

    Resource getResource(Resource resource, NodeFactory nodeFactory) throws ModelException {
        if (resource instanceof Statement)
            // does not terminate for recursive statements ... !!!
            return getStatement((Statement) resource, nodeFactory);
        else if (resource instanceof Model)
            return resource;
        else { // "normal" resource
            if (_oldNamespace.equals(resource.getNamespace()))
                return nodeFactory.createResource(_newNamespace, resource.getLocalName());
            else
                return resource;
        }
    }

}
