package edu.stanford.smi.protegex.storage.rdf;

import java.io.*;
import java.util.*;

import org.w3c.rdf.implementation.syntax.sirpac.*;
import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;
import org.w3c.rdf.util.*;
// import org.w3c.rdf.vocabulary.rdf_schema_19990303.*;
import org.w3c.rdf.vocabulary.rdf_schema_200001.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.*;

import edu.stanford.smi.protegex.storage.walker.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;

public class RDFFrameCreator implements FrameCreator {

    String _classesFileName;
    String _instancesFileName;
    public Namespaces _namespaces;
    Collection _errors;

    public RDFFactory _rdfFactory;
    public NodeFactory _nodeFactory;
    public Model _rdfsModel;
    public Model _rdfModel;

    public Hashtable _resources;

    public HashSet _classes;
    public HashSet _slots;

    // #RV
    boolean _differentOutput = false;
    Writer _rdfsModelWriter;
    Writer _rdfModelWriter;
    // #RV

    public RDFFrameCreator(
        String classesFileName,
        String instancesFileName,
        Namespaces namespaces,
        Collection errors) {
        _classesFileName = classesFileName;
        _instancesFileName = instancesFileName;
        _namespaces = namespaces;
        _errors = errors;
    }

    // #RV
    public RDFFrameCreator(Writer rdfsModel, Writer rdfModel, Namespaces namespaces, Collection errors) {
        _rdfsModelWriter = rdfsModel;
        _rdfModelWriter = rdfModel;
        _namespaces = namespaces;
        _errors = errors;
        _differentOutput = true;
    }
    // #RV

    public void start() {
        initRDF();
        _classes = new HashSet();
        _slots = new HashSet();
    }

    public void createCls(
        WalkerFrame cls,
        Collection superclasses,
        WalkerFrame type,
        boolean isAbstract,
        String documentation) {
        // System.out.println("createCls " + cls+ " : " + type
        //   + " :: " + superclasses);
        Resource clsResource = resource(cls);
        _classes.add(clsResource);
        add(_rdfsModel, statement(clsResource, RDF.type, resource(type)));
        for (Iterator scIterator = superclasses.iterator(); scIterator.hasNext();) {
            WalkerFrame superclass = (WalkerFrame) scIterator.next();
            add(_rdfsModel, statement(clsResource, RDFS.subClassOf, resource(superclass)));
        }
        addIsAbstract(clsResource, isAbstract);
        addComment(_rdfsModel, clsResource, documentation);
        addLabel(_rdfsModel, cls);
    }

    public boolean singleAllowedClass() {
        return true; // we need a single allowed class
    }

    // override in subclasses
    public void addIsAbstract(Resource clsResource, boolean isAbstract) {
        // nothing to do in plain RDFS version
    }

    public void createInstance(WalkerFrame inst, WalkerFrame type, String documentation) {
        // System.out.println("createInstance " + inst + " : " + type);
        Resource instResource = resource(inst);
        add(_rdfModel, statement(instResource, RDF.type, resource(type)));
        addComment(_rdfModel, instResource, documentation);
        addLabel(_rdfModel, inst);
    }
    
    //TT added to support multiple types
    public void createInstance(WalkerFrame inst, Collection types, String documentation) {        
        Resource instResource = resource(inst);        
        addComment(_rdfModel, instResource, documentation);
        addLabel(_rdfModel, inst);
        
        for (Iterator iter = types.iterator(); iter.hasNext();) {
			WalkerFrame type = (WalkerFrame) iter.next();
			add(_rdfModel, statement(instResource, RDF.type, resource(type)));	
		}        
    }    

    public void createSlot(
        WalkerFrame slot,
        WalkerFrame type,
        Collection superslots,
        WalkerFrame inverseSlot,
        WalkerFrame associatedFacet,
    // FACET
    WalkerSlotRestriction slotRestriction, String documentation) {
        // System.out.println("createSlot " + slot + " : " + type
        //   + " :: " + superslots);
        Resource slotResource = resource(slot);
        _slots.add(slotResource);
        // type
        if (type != null)
            add(_rdfsModel, statement(slotResource, RDF.type, resource(type)));
        // else ??? (what should this mean?)
        // superslots
        if (superslots != null) {
            for (Iterator superslotIterator = superslots.iterator(); superslotIterator.hasNext();) {
                WalkerFrame superslot = (WalkerFrame) superslotIterator.next();
                add(_rdfsModel, statement(slotResource, RDFS.subPropertyOf, resource(superslot)));
            }
        }
        // inverse slot
        if (inverseSlot != null)
            addInverseSlot(slotResource, inverseSlot);
        // FACET
        // associated facet
        if (associatedFacet != null)
            addAssociatedFacet(slotResource, associatedFacet);
        // slot restriction
        if (slotRestriction != null)
            addSlotRestriction(slotResource, slotRestriction);
        // documentation
        if (documentation != null)
            addComment(_rdfsModel, slotResource, documentation);
        addLabel(_rdfsModel, slot);
    }

    // override in subclasses
    public void addInverseSlot(Resource slotResource, WalkerFrame inverseSlot) {
        // not supported in pure RDFS directly
    }

    // FACET
    // override in subclasses
    public void addAssociatedFacet(Resource slotResource, WalkerFrame associatedFacet) {
        // not supported in pure RDFS directly
    }

    // override in subclasses
    public void addSlotRestriction(Resource slotResource, WalkerSlotRestriction slotRestriction) {
        // pure RDFS version: only use things that are supported by RDFS directly
        if (slotRestriction.isAny()) {
            // nothing to do
        } else if (slotRestriction.isInstance()) {
            WalkerFrame allowedClass = slotRestriction.getAllowedClass();
            if (allowedClass != null)
                add(_rdfsModel, statement(slotResource, RDFS.range, resource(allowedClass)));
            else
                add(_rdfsModel, statement(slotResource, RDFS.range, RDFS.Resource));
        } else if (slotRestriction.isClass()) {
            add(_rdfsModel, statement(slotResource, RDFS.range, RDFS.Class));
            // dito (top class)
        } else { // Literal (String, Integer, ...)
            add(_rdfsModel, statement(slotResource, RDFS.range, RDFS.Literal));
        }
    }

    public void attachSlot(
        WalkerFrame cls,
        WalkerFrame slot,
        boolean direct,
        WalkerSlotRestriction overriddenSlotRestriciton,
        String overriddenDocumentation) {
        // System.out.println("attachSlot " + cls + " " + slot);
        Resource clsResource = resource(cls);
        Resource slotResource = resource(slot);
        if (direct) // add domain to property
            add(_rdfsModel, statement(slotResource, RDFS.domain, clsResource));
        if (overriddenSlotRestriciton != null) {
            // assumption: overriddenDocumentation can only be != null
            // if also overriddenSlotRestriciton != null !!!
            overrideSlotRestriction(
                cls,
                slot,
                clsResource,
                slotResource,
                overriddenSlotRestriciton,
                overriddenDocumentation);
        }
    }

    // override in subclasses
    public void overrideSlotRestriction(
        WalkerFrame cls,
        WalkerFrame slot,
        Resource clsResource,
        Resource slotResource,
        WalkerSlotRestriction slotRestriction,
        String overriddenDocumentation) {
        // pure RDFS version: ignore
    }

    public void addOwnSlotValues(WalkerFrame instance, WalkerFrame slot, Collection values) {
        // System.out.println("addOwnSlotValues " + instance + "." + slot
        //   + " = " + values);
        Resource instanceResource = resource(instance);
        Model model; // decide to which model to add
        // hmmm, we should ask protege to make a better guess ... !!!!
        if (_classes.contains(instanceResource) || _slots.contains(instanceResource))
            model = _rdfsModel;
        else
            model = _rdfModel;
        Resource property = resource(slot);
        addValues(model, instanceResource, property, values);
    }

    public void addValues(Model model, Resource resource, Resource property, Collection values) {
        for (Iterator valueIterator = values.iterator(); valueIterator.hasNext();) {
            Object value = valueIterator.next();
            if (value instanceof WalkerFrame) { // instance/resource
                add(model, statement(resource, property, resource((WalkerFrame) value)));
            } else { // Literal
                add(model, statement(resource, property, literal(value.toString())));
            }
        }
    }

    public void addComment(Model model, Resource resource, String comment) {
        if (comment != null)
            add(model, statement(resource, RDFS.comment, literal(comment)));
    }

    public void addLabel(Model model, WalkerFrame frame) {
        Resource resource = resource(frame);
        String label = frame.getDisplayName();
        if (label != null)
            add(model, statement(resource, RDFS.label, literal(label)));
    }

    // #RV
    /* original mehtod
      public void finish() {
        try {
          // we should rename old into new RDFS namespace here ??? ... !!!
          // System.out.print("Saving " + _classesFileName + ". ");
          // System.out.flush();
          PrintStream out = new PrintStream(new FileOutputStream(_classesFileName));
          // RDFSerializer serializer = _rdfFactory.createSerializer(); // old
          Map namespaceMap = _namespaces.getNamespaceMap();
          RDFSerializer serializer = new SiRS(namespaceMap);
          RDFUtil.dumpModel(_rdfsModel, out, serializer);
          out.close();
          // System.out.println("done.");
          // System.out.print("Saving " + _instancesFileName + ". ");
          // System.out.flush();
          // SiRS cannot be reused, so let's get a new one
          serializer = new SiRS(namespaceMap);
          out = new PrintStream(new FileOutputStream(_instancesFileName));
          RDFUtil.dumpModel(_rdfModel, out, serializer);
          out.close();
          // System.out.println("done.");
        } catch (Exception e) { error(e); }
      } */

    // modified method
    public void finish() {
        try {
            // we should rename old into new RDFS namespace here ??? ... !!!
            // System.out.print("Saving " + _classesFileName + ". ");
            // System.out.flush();
            Map namespaceMap = _namespaces.getNamespaceMap();
            RDFSerializer serializer = new SiRS(namespaceMap);
            if (_differentOutput) {
                serializer.serialize(_rdfsModel, _rdfsModelWriter);
            } else {
                PrintStream out = new PrintStream(new FileOutputStream(_classesFileName));
                // RDFSerializer serializer = _rdfFactory.createSerializer(); // old
                RDFUtil.dumpModel(_rdfsModel, out, serializer);
                out.close();
            }
            // System.out.println("done.");
            // System.out.print("Saving " + _instancesFileName + ". ");
            // System.out.flush();
            // SiRS cannot be reused, so let's get a new one
            serializer = new SiRS(namespaceMap);
            if (_differentOutput) {
                serializer.serialize(_rdfModel, _rdfModelWriter);
            } else {
                PrintStream out = new PrintStream(new FileOutputStream(_instancesFileName));
                RDFUtil.dumpModel(_rdfModel, out, serializer);
                out.close();
            }
            // System.out.println("done.");
        } catch (Exception e) {
            error(e);
        }
    }
    // #RV

    // RDF and RDFS constants, resources, etc.

    void initRDF() {
        _resources = new Hashtable();
        _rdfFactory = new RDFFactoryImpl();
        _rdfsModel = _rdfFactory.createModel();
        _rdfModel = _rdfFactory.createModel();
        try {
            _nodeFactory = _rdfsModel.getNodeFactory(); // node factory is
            // used for BOTH models which is correct for the current
            // rdf api implemention!!!
        } catch (Exception e) {
            error(e);
        }
    }

    // RDF model/statement/resource handling

    public void add(Model model, Statement statement) {
        if (statement != null) {
            try {
                model.add(statement);
            } catch (Exception e) {
                error(e);
            }
        }
    }

    public void remove(Model model, Statement statement) {
        if (statement != null) {
            try {
                model.remove(statement);
            } catch (Exception e) {
                error(e);
            }
        }
    }

    public Statement statement(Resource subject, Resource predicate, RDFNode object) {
        if (subject != null && predicate != null && object != null) {
            try {
                return _nodeFactory.createStatement(subject, predicate, object);
            } catch (Exception e) {
                error(e);
            }
        }
        return null;
    }

    public Resource resource(String namespace, String name) {
        try {
            return _nodeFactory.createResource(namespace, name);
        } catch (Exception e) {
            error(e);
        }
        return null;
    }

    // override in subclasses (or getThing ...)
    public Resource resource(WalkerFrame frame) {
        Resource resource = (Resource) _resources.get(frame);
        if (resource != null) {
            return resource;
        } else {
            if (frame.isThing())
                return getThing();
            else if (frame.isStandardClass())
                return getStandardClass();
            else if (frame.isStandardSlot())
                return getStandardSlot();
            // the other special cases ...
            else {
                String namespace = frame.getNamespace();
                if (namespace == null)
                    namespace = _namespaces.getDefaultNamespace();
                resource = resource(namespace, frame.getLocalName());
            }
            _resources.put(frame, resource);
            return resource;
        }
    }

    public Resource getThing() {
        return RDFS.Resource;
    }

    public Resource getStandardClass() {
        return RDFS.Class;
    }

    public Resource getStandardSlot() {
        return RDF.Property;
    }

    public Literal literal(String string) {
        try {
            return _nodeFactory.createLiteral(string);
        } catch (Exception e) {
            error(e);
        }
        return null;
    }

    public Literal literal(int n) {
        return literal("" + n);
    }

    // error handling

    public void error(String message) {
        _errors.add(message);
    }

    public void error(Exception exc) {
        _errors.add(exc.toString());
        exc.printStackTrace();
    }


}
