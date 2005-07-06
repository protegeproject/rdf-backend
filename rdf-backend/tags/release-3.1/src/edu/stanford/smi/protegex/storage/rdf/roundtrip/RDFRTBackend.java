package edu.stanford.smi.protegex.storage.rdf.roundtrip;

import java.io.*;
import java.util.*;

import org.xml.sax.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.rdf.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;
// #RV

public class RDFRTBackend extends RDFBackend {

    public String getDescription() {
        return "RDFS (round trip: plain Protege / extended RDFS)";
    }

    public void loadWalk(
        KnowledgeBase kb,
        PropertyList sources,
        String classesFileName,
        String instancesFileName,
        String namespace,
        Namespaces namespaces,
        Collection errors,
        boolean included) {
        new RDFRTFrameWalker(classesFileName, instancesFileName, namespace, namespaces).walk(
            new ProtegeFrameCreator(kb, namespaces, included, errors));
    }

    // #RV
    public void loadWalk(
        KnowledgeBase kb,
        PropertyList sources,
        InputSource classes,
        InputSource instances,
        String namespace,
        Namespaces namespaces,
        Collection errors) {
        new RDFRTFrameWalker(classes, instances, namespace, namespaces).walk(
            new ProtegeFrameCreator(kb, namespaces, false, errors));
    }
    // #RV

    public void saveWalk(
        KnowledgeBase kb,
        PropertyList sources,
        String classesFileName,
        String instancesFileName,
        Namespaces namespaces,
        Collection errors) {
        new ProtegeFrameWalker(kb, namespaces).walk(
            new RDFRTFrameCreator(classesFileName, instancesFileName, namespaces, errors));
    }

    // #RV
    public void saveWalk(
        KnowledgeBase kb,
        PropertyList sources,
        Writer rdfsModel,
        Writer rdfModel,
        Namespaces namespaces,
        Collection errors) {
        new ProtegeFrameWalker(kb, namespaces).walk(new RDFRTFrameCreator(rdfsModel, rdfModel, namespaces, errors));
    }
    // #RV

}
