package edu.stanford.smi.protegex.storage.rdf;

import java.io.Writer;
import java.util.Collection;

import org.xml.sax.InputSource;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBaseFactory;
import edu.stanford.smi.protege.model.KnowledgeBaseSourcesEditor;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.util.FileUtilities;
import edu.stanford.smi.protege.util.PropertyList;
import edu.stanford.smi.protege.util.SystemUtilities;
import edu.stanford.smi.protege.util.URIUtilities;
import edu.stanford.smi.protegex.storage.walker.protege.Namespaces;
import edu.stanford.smi.protegex.storage.walker.protege.ProtegeFrameCreator;
import edu.stanford.smi.protegex.storage.walker.protege.ProtegeFrameWalker;

public class RDFBackend implements KnowledgeBaseFactory {

    static final String CLASS_FILE_NAME_PROPERTY = "rdfs_file_name";
    static final String INSTANCE_FILE_NAME_PROPERTY = "rdf_file_name";
    static final String NAMESPACE_PROPERTY = "namespace_name";

    static {
        try {
            // set the parser to use so that the RDF-API library can find it.
            System.setProperty("org.xml.sax.parser", "org.apache.xerces.parsers.SAXParser");
        } catch (SecurityException e) {
            // this happens in applets, ignore it.
        }
    }

    // methods from the KnowledgeBaseFactory interface ----------------

    // override in subclasses
    public String getDescription() {
        return "RDFS (plain Protege / plain RDFS)";
    }

    /* not used any more
    public String getLabel() {
      return "RDFS";
    }
    */

    public String getProjectFilePath() {
        return null;
        // return files/rdfs.pprj; ...
    }

    public KnowledgeBaseSourcesEditor createKnowledgeBaseSourcesEditor(String projectURIString, PropertyList sources) {
        return new RDFSourcesEditor(projectURIString, sources);
    }

    public boolean isComplete(PropertyList sources) {
        return sources.getString(CLASS_FILE_NAME_PROPERTY) != null
            && sources.getString(INSTANCE_FILE_NAME_PROPERTY) != null
            && sources.getString(NAMESPACE_PROPERTY) != null;
    }

    public KnowledgeBase createKnowledgeBase(Collection errors) {
        RDFKnowledgeBase kb = new RDFKnowledgeBase(this);
        createSystemFrames(kb);
        return kb;
    }

    public void loadKnowledgeBase(KnowledgeBase kb, PropertyList sources, Collection errors) {
        SystemUtilities.setContextClassLoader(this);
        load(kb, sources, errors, false);
    }

    // #RV
    /* hack to load a project from InputStreams */
    public void loadKnowledgeBase(
        KnowledgeBase kb,
        PropertyList sources,
        InputSource classes,
        InputSource instances,
        Collection errors) {
        load(kb, sources, classes, instances, errors);
    }
    // #RV

    public void includeKnowledgeBase(KnowledgeBase kb, PropertyList sources, Collection errors) {
        load(kb, sources, errors, true);
    }

    public void saveKnowledgeBase(KnowledgeBase kb, PropertyList sources, Collection errors) {
        save(kb, sources, errors);
    }

    // #RV
    /* hack to save a project to Strings */
    public void saveKnowledgeBase(
        KnowledgeBase kb,
        PropertyList sources,
        Writer rdfsModel,
        Writer rdfModel,
        Collection errors) {
        save(kb, sources, rdfsModel, rdfModel, errors);
    }
    // #RV

    // stuff needed for RDFSourcesEditor -------------------------------

    public static String getClsesFileName(PropertyList sources) {
        return sources.getString(CLASS_FILE_NAME_PROPERTY);
    }

    public static String getInstancesFileName(PropertyList sources) {
        return sources.getString(INSTANCE_FILE_NAME_PROPERTY);
    }

    public static String getNamespace(PropertyList sources) {
        return sources.getString(NAMESPACE_PROPERTY);
    }

    public static void setSourceFiles(
        PropertyList sources,
        String classesFileName,
        String instancesFileName,
        String namespace) {
        sources.setString(CLASS_FILE_NAME_PROPERTY, classesFileName);
        sources.setString(INSTANCE_FILE_NAME_PROPERTY, instancesFileName);
        if (namespace != null) {
            if (!(namespace.endsWith("#") || namespace.endsWith("/") || namespace.endsWith(":"))) {
				namespace = namespace + "#"; // ????
			}
            if (namespace.indexOf(' ') != -1) {
				namespace = namespace.replace(' ', '_');
			}
        }
        sources.setString(NAMESPACE_PROPERTY, namespace);
    }

    // main -------------------------------------------------------------

    public static void main(String[] args) {
        edu.stanford.smi.protege.Application.main(args);
    }

    // save and load ----------------------------------------------------

    void load(KnowledgeBase kb, PropertyList sources, Collection errors, boolean included) {
        // the following code is "fragile" since we need information
        // from the including project (and we even change it!)
        String classesFileName = getClsesFileName(sources);
        if (classesFileName != null) {			
        	classesFileName = URIUtilities.resolve(kb.getProject().getProjectURI(), classesFileName).toString();
		}
        String instancesFileName = getInstancesFileName(sources);
        if (instancesFileName != null) {
        	instancesFileName = URIUtilities.resolve(kb.getProject().getProjectURI(), instancesFileName).toString();
		}
        String namespace = getNamespace(sources);
        Namespaces namespaces;
        if (included && kb instanceof RDFKnowledgeBase) {
            // for included projects, we have to use the namespaces from
            // the including project!
            Project project = kb.getProject();
            PropertyList includingSources = project.getSources();
            String includingNamespace = includingSources.getString(NAMESPACE_PROPERTY);
            if (includingNamespace == null || includingNamespace.equals("")) {
                // including project is not an RDF one (should never happen)
                namespaces = new Namespaces(namespace, sources);
            } else {
                namespaces = new Namespaces(includingNamespace, includingSources);
                // add a nice abbreviation for included project ... !!!
                // start recording frames for this project
                ((RDFKnowledgeBase) kb).startIncludedProject(
                    classesFileName,
                    instancesFileName,
                    namespace,
                    includingSources);
            }
        } else { // not included, use own namespaces
            namespaces = new Namespaces(namespace, sources);
        }
        if (!included && kb instanceof RDFKnowledgeBase) {
            // remove renaming namespaces from previous save
            RDFKnowledgeBase.setRenamingNamespaces(sources, "");
        }
        try {
            loadWalk(kb, sources, classesFileName, instancesFileName, namespace, namespaces, errors, included);
            if (!included && namespaces.size() >= getInterestingNamespacesSize()) {
                try { // ignore errors here
                    namespaces.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            namespaces.save(); // save in MAIN project's sources
        } catch (Exception e) {
            errors.add("Fatal error: " + e);
            e.printStackTrace();
        }
        if (included && kb instanceof RDFKnowledgeBase) {
			((RDFKnowledgeBase) kb).finishIncludedProject();
		}
    }

    // #RV
    /* hack for loading a project from InputStreams */
    void load(KnowledgeBase kb, PropertyList sources, InputSource classes, InputSource instances, Collection errors) {
        String namespace = getNamespace(sources);
        Namespaces namespaces;
        namespaces = new Namespaces(namespace, sources);
        if (kb instanceof RDFKnowledgeBase) {
            // remove renaming namespaces from previous save
            RDFKnowledgeBase.setRenamingNamespaces(sources, "");
        }
        try {
            loadWalk(kb, sources, classes, instances, namespace, namespaces, errors);
            if (namespaces.size() >= getInterestingNamespacesSize()) {
                try { // ignore errors here
                    namespaces.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            namespaces.save(); // save in MAIN project's sources
        } catch (Exception e) {
            errors.add("Fatal error: " + e);
            e.printStackTrace();
        }
    }
    // #RV

    // override in subclasses
    public int getInterestingNamespacesSize() {
        return 4;
    }

    // override in subclasses
    public void loadWalk(
        KnowledgeBase kb,
        PropertyList sources,
        String classesFileName,
        String instancesFileName,
        String namespace,
        Namespaces namespaces,
        Collection errors,
        boolean included) {
        new RDFFrameWalker(classesFileName, instancesFileName, namespace, namespaces).walk(
            new ProtegeFrameCreator(kb, namespaces, included, errors));
    }

    // #RV
    /* Hack to load a Project from InputStreams */
    public void loadWalk(
        KnowledgeBase kb,
        PropertyList sources,
        InputSource classes,
        InputSource instances,
        String namespace,
        Namespaces namespaces,
        Collection errors) {
        new RDFFrameWalker(classes, instances, namespace, namespaces).walk(
            new ProtegeFrameCreator(kb, namespaces, false, errors));
    }
    // #RV

    void save(KnowledgeBase kb, PropertyList sources, Collection errors) {
        String classesFileName = getClsesFileName(sources);
        if (classesFileName != null) {
			classesFileName = FileUtilities.getAbsolutePath(classesFileName, kb.getProject());
		}
        String instancesFileName = getInstancesFileName(sources);
        if (instancesFileName != null) {
			instancesFileName = FileUtilities.getAbsolutePath(instancesFileName, kb.getProject());
		}
        String namespace = getNamespace(sources);
        Namespaces namespaces = new Namespaces(namespace, sources);
        try {
            saveWalk(kb, sources, classesFileName, instancesFileName, namespaces, errors);
            // saveWalk is not supposed to change the namespaces (as load is) !!!
        } catch (Exception e) {
            errors.add("Fatal error: " + e);
            e.printStackTrace();
        }
    }

    // #RV
    void save(KnowledgeBase kb, PropertyList sources, Writer rdfsModel, Writer rdfModel, Collection errors) {
        String namespace = getNamespace(sources);
        Namespaces namespaces = new Namespaces(namespace, sources);
        try {
            saveWalk(kb, sources, rdfsModel, rdfModel, namespaces, errors);
            // saveWalk is not supposed to change the namespaces (as load is) !!!
        } catch (Exception e) {
            errors.add("Fatal error: " + e);
            e.printStackTrace();
        }
    }
    // #RV

    // override in subclasses
    public void saveWalk(
        KnowledgeBase kb,
        PropertyList sources,
        String classesFileName,
        String instancesFileName,
        Namespaces namespaces,
        Collection errors) {
        new ProtegeFrameWalker(kb, namespaces).walk(
            new RDFFrameCreator(classesFileName, instancesFileName, namespaces, errors));
    }

    // #RV
    public void saveWalk(
        KnowledgeBase kb,
        PropertyList sources,
        Writer rdfsModel,
        Writer rdfModel,
        Namespaces namespaces,
        Collection errors) {
        new ProtegeFrameWalker(kb, namespaces).walk(new RDFFrameCreator(rdfsModel, rdfModel, namespaces, errors));
    }
    // #RV

    // RDF system frames ------------------------------------------------

    // subclass this for different backend variants
    public void createSystemFrames(RDFKnowledgeBase kb) {
        // no special frames for plain version
    }
}
