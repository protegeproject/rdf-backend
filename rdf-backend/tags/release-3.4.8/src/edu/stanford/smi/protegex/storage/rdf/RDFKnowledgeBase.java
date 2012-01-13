package edu.stanford.smi.protegex.storage.rdf;

import java.io.*;
import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;

public class RDFKnowledgeBase extends DefaultKnowledgeBase {

    // at the moment, we ONLY need this class for renaming
    // namespaces of included frames :-(
    // this will change if Protege supports namespaces!

    static final String INCLUDED_PROJECTS_PROPERTY = "included_projects";
    static final String RENAMING_NAMESPACES_PROPERTY = "renaming_namespaces";

    PropertyList _mainSources;
    IncludedProject _currentProject;
    ArrayList _includedProjects; // of IncludedProject
    ArrayList _renamingNamespaces;

    public RDFKnowledgeBase(KnowledgeBaseFactory factory) {
        super(factory);
        _currentProject = null;
        _includedProjects = null;
        _renamingNamespaces = null;
        setFrameNameValidator(new RDFFrameNameValidator());
    }

    public void startIncludedProject(String classesFileName, String instancesFileName,
            String namespace, PropertyList mainSources) {
        _mainSources = mainSources;
        if (_includedProjects == null) { // first included project
            _includedProjects = new ArrayList();
            _mainSources.setString(INCLUDED_PROJECTS_PROPERTY, "");
            // reset renaming namespaces ... ???
        }
        _currentProject = new IncludedProject(classesFileName, instancesFileName, namespace,
                mainSources);
        _includedProjects.add(_currentProject);
    }

    public void finishIncludedProject() {
        _currentProject = null;
    }

    void newFrame(Frame frame) {
        if (_currentProject != null)
            _currentProject.newFrame(frame);
    }

    // static access to INCLUDED_PROJECTS_PROPERTY and
    // RENAMING_NAMESPACES_PROPERTY in sources

    public static List getIncludedProjects(PropertyList sources) {
        ArrayList includedProjects = new ArrayList();
        String includedProjectsString = sources.getString(INCLUDED_PROJECTS_PROPERTY);
        if (includedProjectsString != null) {
            for (StringTokenizer tokens = new StringTokenizer(includedProjectsString); tokens
                    .hasMoreTokens();) {
                String projectName = tokens.nextToken();
                tokens.nextToken(); // skip namespace
                includedProjects.add(projectName);
            }
        }
        return includedProjects;
    }

    public static List getIncludedProjectsNamespaces(PropertyList sources) {
        ArrayList includedProjectsNamespaces = new ArrayList();
        String includedProjectsString = sources.getString(INCLUDED_PROJECTS_PROPERTY);
        if (includedProjectsString != null) {
            for (StringTokenizer tokens = new StringTokenizer(includedProjectsString); tokens
                    .hasMoreTokens();) {
                tokens.nextToken(); // skip name
                String namespace = tokens.nextToken();
                includedProjectsNamespaces.add(namespace);
            }
        }
        return includedProjectsNamespaces;
    }

    public static void setRenamingNamespaces(PropertyList sources, String renamingNamespacesString) {
        sources.setString(RENAMING_NAMESPACES_PROPERTY, renamingNamespacesString);
    }

    public void useRenamingNamespaces() {
        if (_mainSources != null && _includedProjects != null) {
            _renamingNamespaces = new ArrayList();
            String renamingNamespacesString = _mainSources.getString(RENAMING_NAMESPACES_PROPERTY);
            if (renamingNamespacesString != null) {
                for (StringTokenizer tokens = new StringTokenizer(renamingNamespacesString); tokens
                        .hasMoreTokens();) {
                    String namespace = tokens.nextToken();
                    _renamingNamespaces.add(namespace);
                }
            }
            if (_renamingNamespaces.isEmpty()
                    || _includedProjects.size() != _renamingNamespaces.size()) {
                _renamingNamespaces = null;
            } else {
                System.out.println("Using namespace renaming for included projects;");
                System.out.println("  new namespaces: " + _renamingNamespaces);
            }
        }
    }

    public String getNewNamespace(Frame frame, String frameNamespace) {
        if (_renamingNamespaces != null) {
            Iterator projectIterator = _includedProjects.iterator();
            Iterator namespaceIterator = _renamingNamespaces.iterator();
            while (projectIterator.hasNext()) {
                IncludedProject project = (IncludedProject) projectIterator.next();
                String newNamespace = (String) namespaceIterator.next();
                if (project.contains(frame)) {
                    String oldNamespace = project.getNamespace();
                    if (oldNamespace.equals(frameNamespace))
                        return newNamespace;
                    else
                        return null;
                }
            }
        }
        return null;
    }

    // all createSomething used in ProtegeFrameCreator methods must be wrapped

    public Cls createCls(String name, Collection parents) {
        Cls cls = super.createCls(name, parents);
        newFrame(cls);
        return cls;
    }

    public Slot createSlot(String name, Cls type) {
        Slot slot = super.createSlot(name, type);
        newFrame(slot);
        return slot;
    }

    public Instance createInstance(String name, Cls type) {
        Instance instance = super.createInstance(name, type);
        newFrame(instance);
        return instance;
    }

    public Facet createFacet(String name, Cls type) {
        Facet facet = super.createFacet(name, type);
        newFrame(facet);
        return facet;
    }

    // add here if ProtegeFrameCreator is extended !!!

    /* inner */class IncludedProject {

        String _name;
        String _classesFileName;
        String _instancesFileName;
        String _namespace;
        HashSet _frames;

        IncludedProject(String classesFileName, String instancesFileName, String namespace,
                PropertyList mainSources) {
            _classesFileName = classesFileName;
            _instancesFileName = instancesFileName;
            _namespace = namespace;
            guessName();
            _frames = new HashSet();
            // append name+namespace to end
            String includedProjectsString = mainSources.getString(INCLUDED_PROJECTS_PROPERTY);
            if (includedProjectsString == null)
                includedProjectsString = "";
            includedProjectsString += _name + " " + _namespace + " ";
            mainSources.setString(INCLUDED_PROJECTS_PROPERTY, includedProjectsString);
        }

        void newFrame(Frame frame) {
            _frames.add(frame);
        }

        boolean contains(Frame frame) {
            return _frames.contains(frame);
        }

        String getNamespace() {
            return _namespace;
        }

        void guessName() {
            String cName = null;
            if (_classesFileName != null) {
                cName = new File(_classesFileName).getName();
                int p = cName.indexOf(".");
                if (p != -1)
                    cName = cName.substring(0, p);
            }
            String iName = null;
            if (_instancesFileName != null) {
                iName = new File(_instancesFileName).getName();
                int p = iName.indexOf(".");
                if (p != -1)
                    iName = iName.substring(0, p);
            }
            if (cName != null) {
                if (iName != null) {
                    if (cName.equals(iName))
                        _name = cName;
                    else
                        _name = cName + "/" + iName; // plus original extensions
                                                     // ... !!!
                } else {
                    _name = cName;
                }
            } else if (iName != null) {
                _name = iName;
            } else {
                _name = "<empty>"; // no classes/instances files
            }
            if (_name.indexOf(' ') != -1)
                _name = _name.replace(' ', '_');
        }

    }

    public synchronized void addOwnSlotValue(Frame frame, Slot slot, Object value) {
        List values = new ArrayList(getDirectOwnSlotValues(frame, slot));
        values.remove(value);
        values.add(value);
        setDirectOwnSlotValues(frame, slot, values);
    }
}