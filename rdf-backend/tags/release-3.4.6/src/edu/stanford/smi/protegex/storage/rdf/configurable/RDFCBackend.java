package edu.stanford.smi.protegex.storage.rdf.configurable;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.rdf.*;
import edu.stanford.smi.protegex.storage.rdf.roundtrip.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;

public class RDFCBackend extends RDFBackend {
    public static final String DESCRIPTION = "RDF Files";
    static final String USE_ROUNDTRIP_PROPERTY = "use_roundtrip";

    public static void main(String[] args) {
        edu.stanford.smi.protege.Application.main(args);
    }

    public String getDescription() {
        return DESCRIPTION;
    }

    public KnowledgeBaseSourcesEditor createKnowledgeBaseSourcesEditor(String projectURIString, PropertyList sources) {
        return new RDFCSourcesEditor(projectURIString, sources);
    }

    // configuration

    static boolean getUseRoundtripBackend(PropertyList sources) {
        Boolean useRT = sources.getBoolean(USE_ROUNDTRIP_PROPERTY);
        return useRT == null || useRT.booleanValue();
        // default is true!
    }

    public static void saveConfiguration(PropertyList sources, boolean useRoundtripBackend) {
        sources.setBoolean(USE_ROUNDTRIP_PROPERTY, useRoundtripBackend);
        // add here for additional configurations ...
    }

    // load and save

    public void loadWalk(
        KnowledgeBase kb,
        PropertyList sources,
        String classesFileName,
        String instancesFileName,
        String namespace,
        Namespaces namespaces,
        Collection errors,
        boolean included) {
        if (getUseRoundtripBackend(sources))
            new RDFRTFrameWalker(classesFileName, instancesFileName, namespace, namespaces).walk(
                new ProtegeFrameCreator(kb, namespaces, included, errors));
        else // simple backend
            // we probably should ALWAYS use the roundtrip backend ... !!!
            new RDFFrameWalker(classesFileName, instancesFileName, namespace, namespaces).walk(
                new ProtegeFrameCreator(kb, namespaces, included, errors));
    }

    public void saveWalk(
        KnowledgeBase kb,
        PropertyList sources,
        String classesFileName,
        String instancesFileName,
        Namespaces namespaces,
        Collection errors) {
        if (kb instanceof RDFKnowledgeBase)
             ((RDFKnowledgeBase) kb).useRenamingNamespaces();
        if (getUseRoundtripBackend(sources))
            new ProtegeFrameWalker(kb, namespaces).walk(
                new RDFRTFrameCreator(classesFileName, instancesFileName, namespaces, errors));
        else
            new ProtegeFrameWalker(kb, namespaces).walk(
                new RDFFrameCreator(classesFileName, instancesFileName, namespaces, errors));
    }

}
