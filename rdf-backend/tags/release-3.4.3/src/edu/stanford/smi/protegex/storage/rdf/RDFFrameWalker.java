package edu.stanford.smi.protegex.storage.rdf;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.rdf.model.Model;
import org.w3c.rdf.model.RDFNode;
import org.w3c.rdf.model.Resource;
import org.w3c.rdf.model.Statement;
import org.w3c.rdf.syntax.RDFConsumer;
import org.w3c.rdf.syntax.RDFParser;
import org.w3c.rdf.util.ModelConsumer;
import org.w3c.rdf.util.RDFFactory;
import org.w3c.rdf.util.RDFFactoryImpl;
import org.w3c.rdf.util.SetOperations;
import org.w3c.rdf.vocabulary.rdf_schema_200001.RDFS;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.RDF;
import org.xml.sax.InputSource;

import edu.stanford.db.rdf.schema.RDFSchemaModel;
import edu.stanford.smi.protege.util.CollectionUtilities;
import edu.stanford.smi.protege.util.URIUtilities;
import edu.stanford.smi.protegex.storage.walker.FrameCreator;
import edu.stanford.smi.protegex.storage.walker.FrameWalker;
import edu.stanford.smi.protegex.storage.walker.WalkerFrame;
import edu.stanford.smi.protegex.storage.walker.WalkerSlotRestriction;
import edu.stanford.smi.protegex.storage.walker.protege.Namespaces;
import edu.stanford.smi.protegex.storage.walker.protege.ProtegeFrameCreator;

public class RDFFrameWalker implements FrameWalker {

    String _classesFileName;
    String _instancesFileName;
    String _namespace;
    public Namespaces _namespaces;
    public FrameCreator _creator;

    public RDFFactory _rdfFactory;
    public Model _model;
    public Model _spClosure;
    public RDFSchemaModel _rdfSchemaModel;

    HashMap _wframes;

    public HashSet _classes;
    public HashSet _properties;
    public HashSet _instances;

    public HashMap _unhandledTypes; // Maps Resource to Collection of Resource

    Comparator _valueComparator;

    // #RV
    boolean _differentInputSource = false;
    InputSource _classesInputSource;
    InputSource _instancesInputSource;
    // #RV

    public RDFFrameWalker(String classesFileName, String instancesFileName, String namespace, Namespaces namespaces) {
        _classesFileName = classesFileName;
        _instancesFileName = instancesFileName;
        _namespace = namespace;
        _namespaces = namespaces;
        _valueComparator = new ValueComparator();
    }

    // #RV
    /* Hack to load a Project from InputStreams */
    public RDFFrameWalker(InputSource classes, InputSource instances, String namespace, Namespaces namespaces) {
        _classesInputSource = classes;
        _instancesInputSource = instances;
        _differentInputSource = true;
        _namespace = namespace;
        _namespaces = namespaces;
        _valueComparator = new ValueComparator();
    }
    // #RV

    public void walk(FrameCreator frameCreator) {
        _creator = frameCreator;
        _wframes = new HashMap();
        _unhandledTypes = new HashMap();
        if (!init()) {
            error("Fatal error. Execution stopped.");
            return; // don't walk
        }
        _classes = getClasses();
        _properties = getProperties();
        _instances = getInstances();
        // walking:
        _creator.start();
        walkClasses();
        walkInstances();
        walkSlots();
        walkSlotOverrides();
        walkValues();
        finish();
        _creator.finish();
    }

    // override in subclasses
    // #RV
    /* original method:
      public boolean init() {
        // returns false if fatal error occurs
        return getModels(_classesFileName, _instancesFileName);
      }
    */

    public boolean init() {
        // returns false if fatal error occurs
        if (_differentInputSource)
            return getModels(_classesInputSource, _instancesInputSource);
        // Hack to load a Project from InputStreams
        else
            return getModels(_classesFileName, _instancesFileName);
    }
    // #RV

    // walking

    public void walkClasses() {
        for (Iterator classIterator = sorted(_classes).iterator(); classIterator.hasNext();) {
            Resource cls = (Resource) classIterator.next();
            if (isSystemResource(cls)) {
                // we would like to allow this in the future!
                if (!isEncodingSystemResource(cls))
                    error("system classes cannot be changed: " + cls);
                continue;
            }
            Resource type = getDirectType(cls);
            Collection superclasses = getDirectSuperclasses(cls);
            boolean isAbstract = getIsAbstract(cls);
            String documentation = getComment(cls);
            createCls(cls, superclasses, type, isAbstract, documentation);
        }
    }

    public void createCls(
        Resource cls,
        Collection superclasses,
        Resource type,
        boolean isAbstract,
        String documentation) {
        _creator.createCls(wframe(cls), wframes(superclasses), wframe(type), isAbstract, documentation);
    }

    void walkInstances() {
        for (Iterator instIterator = sorted(_instances).iterator(); instIterator.hasNext();) {
            Resource instance = (Resource) instIterator.next();
            if (isSystemResource(instance)) {
                // we would like to allow this in the future!
                if (!isEncodingSystemResource(instance))
                    error("system resources cannot be changed: " + instance);
                continue;
            }
            Resource type = getDirectType(instance);
            String documentation = getComment(instance);
            _creator.createInstance(wframe(instance), wframe(type), documentation);
        }
    }

    void walkSlots() {
        for (Iterator propIterator = sorted(_properties).iterator(); propIterator.hasNext();) {
            Resource property = (Resource) propIterator.next();
            if (isSystemResource(property)) {
                // we would like to allow this in the future!
                if (isEncodingSystemResource(property)) {
                    // for now, we only handle :NAME that can be attached
                    // to some classes:
                    if (!getLocalName(property).equals("_name")) // HACK!!!
                        continue;
                } else {
                    error("system properties cannot be changed: " + property);
                    continue;
                }
            }
            WalkerFrame propertyFrame = wframe(property);
            if (!isSystemResource(property)) {
                // at the moment, system properties can only be attached,
                // not changed
                Resource type = getDirectType(property);
                if (type != null) { // no changes for included properties
                    Collection superProperties = getSuperProperties(property);
                    WalkerFrame inverseSlot = wframe(getInverseProperty(property));
                        WalkerFrame associatedFacet = // FACET
    wframe(getAssociatedFacet(property));
                    WalkerSlotRestriction slotRestriction = null;
                    slotRestriction = getSlotRestriction(property);
                    String documentation = getComment(property);
                    creatingSlot(property, type); // HACK for OIL; see below
                    _creator
                        .createSlot(propertyFrame, wframe(type), wframes(superProperties), inverseSlot, associatedFacet,
                    // FACET
                    slotRestriction, documentation);
                } else {
                    // for RDF imports
                    _creator.createSlot(propertyFrame, null, null, null, null, null, null);
                }
            }
            // domain (-> slot attachments)
            Collection domain = getDomain(property);
            for (Iterator domainIterator = domain.iterator(); domainIterator.hasNext();) {
                Resource domainResource = (Resource) domainIterator.next();
                if (isSystemResource(domainResource)) {
                    if (!isEncodingSystemResource(domainResource))
                        error("system classes cannot be changed: " + domainResource);
                } else {
                    _creator.attachSlot(wframe(domainResource), propertyFrame, true, null, null);
                }
            }
        }
    }

    // needed in some backends (OIL) -- HACK
    // (better: createSlot with all arguments ... !!!)
    public void creatingSlot(Resource property, Resource type) {
        // nothing to do here
    }

    // override in subclasses
    public WalkerSlotRestriction getSlotRestriction(Resource property) {
        WalkerSlotRestriction slotRestriction = null;
        Resource rangeResource = getRange(property);
        if (rangeResource != null)
            if (rangeResource.equals(RDFS.Literal) || getSuperclasses(rangeResource).contains(RDFS.Literal))
                slotRestriction = new RDFSlotRestriction();
        // else // check for XML Schema data types ... !!! XXX
        else
            slotRestriction = new RDFSlotRestriction(wframe(rangeResource));
        return slotRestriction;
    }

    // override in subclasses
    public void walkSlotOverrides() {
        // plain RDFS does not support slot overrides
        // (but we could simulate this with subslots!!!)
    }

    public void walkValues() {
        // walk ALL instances (incl. classes and properties)
        walkValues(_classes, true);
        walkValues(_properties, true);
        walkValues(_instances, false);
    }

    void walkValues(Collection instances, boolean classOrProperty) {
        for (Iterator instIterator = instances.iterator(); instIterator.hasNext();) {
            Resource instance = (Resource) instIterator.next();
            if (isSystemResource(instance)) {
                // we would like to allow this in the future!
                if (!classOrProperty && !isEncodingSystemResource(instance))
                    error("system resources cannot be changed: " + instance);
                continue;
            }
            WalkerFrame instanceFrame = wframe(instance);
            // the following does NOT sort the values ... !!!
            // do this!!! XXX
            try {
                Model pvModel = _model.find(instance, null, null);
                for (Enumeration tripleEnum = pvModel.elements(); tripleEnum.hasMoreElements();) {
                    Statement triple = (Statement) tripleEnum.nextElement();
                    Resource property = triple.predicate();
                    if (!isSimpleSystemProperty(property)) {
                        RDFNode valueNode = triple.object();
                        Object value;
                        if (valueNode instanceof Resource) {
                            Resource valueResource = (Resource) valueNode;
                            if (isEncodingSystemResource(valueResource))
                                continue; // ignore encoding values, they are handled elsewhere
                            value = wframe(valueResource);
                        } else
                            value = getLabel(valueNode); // Literal
                        _creator.addOwnSlotValues(
                            instanceFrame,
                            wframe(property),
                            CollectionUtilities.createCollection(value));
                    }
                }
            } catch (Exception e) {
                error(e);
            }
        }
    }

    public void finish() {
        walkUnhandledTypes();
    }

    public void walkUnhandledTypes() {
        for (Iterator utIterator = _unhandledTypes.entrySet().iterator(); utIterator.hasNext();) {
            Map.Entry unhandled = (Map.Entry) utIterator.next();
            Resource resource = (Resource) unhandled.getKey();
            Collection types = (Collection) unhandled.getValue();
            
            //TT added support for multiple types
            if (!(_creator instanceof ProtegeFrameCreator)) {
                error("Resource with more than one type not yet handled: " + resource + " ; unhandled types = " + types);
                // use addOwnSlotValues here if we add missing template slots
                // (i.e., rdf:type) ... !!!
            } else {
            	((ProtegeFrameCreator)_creator).addTypesToInstance(wframe(resource), wframes(types));
            }            
            
        }
    }

    // general RDFS inferences (no WalkerFrames involved here!)

    public Resource getDirectType(Resource resource) {
        Collection types = getValues(_spClosure, resource, RDF.type);
        if (types.isEmpty()) // ???
            return null;
        else if (types.size() == 1)
            // check that type is not a literal!
            return (Resource) types.iterator().next();
        else { // more than one type; pick main one and remember others
            Resource type = pickMainType(types);
            ArrayList unhandled = new ArrayList(types);
            unhandled.remove(type);
            _unhandledTypes.put(resource, unhandled);
            return type;
        }
    }

    // subclasses might want to add heuristics here!
    // current subclasses might rely on these heuristics,
    // so don't change them (OIL does!!!)
    public Resource pickMainType(Collection types) {
        // pick a type from rdf/rdfs (or other official w3 namespace) if possible
        ArrayList w3Types = new ArrayList();
        for (Iterator typeIterator = types.iterator(); typeIterator.hasNext();) {
            Resource type = (Resource) typeIterator.next();
            if (getURI(type).startsWith("http://www.w3.org"))
                w3Types.add(type);
        }
        // pick "shortest" type (often the superclass if you have "marker classes")
        if (!w3Types.isEmpty())
            types = w3Types; // use w3 types
        Resource mainType = null;
        int minlen = 0;
        for (Iterator typeIterator = types.iterator(); typeIterator.hasNext();) {
            Resource type = (Resource) typeIterator.next();
            int len = getURI(type).length();
            if (mainType == null || len < minlen) {
                mainType = type;
                minlen = len;
            }
        }
        return mainType;
    }

    public String getComment(Resource resource) {
        Collection comments = getValues(_spClosure, resource, RDFS.comment);
        if (comments.isEmpty())
            return null;
        else {
            StringBuffer commentBuffer = new StringBuffer();
            for (Iterator commentIterator = comments.iterator(); commentIterator.hasNext();) {
                RDFNode comment = (RDFNode) commentIterator.next();
                commentBuffer.append(getLabel(comment)); // not correct for
                // comments that are resources ... !!!
                if (commentIterator.hasNext())
                    commentBuffer.append("\n");
            }
            return commentBuffer.toString();
        }
    }

    HashSet getClasses() {
        HashSet classes = new HashSet();
        try {
            Model classesModel = _rdfSchemaModel.find(null, RDF.type, RDFS.Class);
            addSubjects(classes, classesModel);
            // guess further classes (needed since metaclass might be in included
            // project!); HACK ... !!!
            // a better solution would probably be to communicate with the
            // current Protege KB ????
            classesModel = _spClosure.find(null, RDFS.subClassOf, null);
            addSubjects(classes, classesModel);
            // addObjects(classes, classesModel); // not needed ?
        } catch (Exception e) {
            error(e);
        }
        return classes;
    }

    public Collection getDirectSuperclasses(Resource cls) {
        return getValues(_spClosure, cls, RDFS.subClassOf);
    }

    public Collection getSuperclasses(Resource cls) {
        return getValues(_rdfSchemaModel, cls, RDFS.subClassOf);
    }

    // override in subclasses
    public boolean getIsAbstract(Resource cls) {
        return false;
    }

    HashSet getInstances() {
        HashSet instances = new HashSet();
        try {
            Model instancesModel = _spClosure.find(null, RDF.type, null); // _model?
            addSubjects(instances, instancesModel);
        } catch (Exception e) {
            error(e);
        }
        // remove classes and properties
        instances.removeAll(_classes);
        instances.removeAll(_properties);
        // remove other things??? ... !!!
        return instances;
    }

    HashSet getProperties() {
        HashSet properties = new HashSet();
        try {
            Model propertiesModel = _rdfSchemaModel.find(null, RDF.type, RDF.Property);
            addSubjects(properties, propertiesModel);
            // do same hack here as for classes: everything with
            // domain, range, or super properties is also a property
            // (see remark about HACK in getClasses)
            propertiesModel = _spClosure.find(null, RDFS.domain, null);
            addSubjects(properties, propertiesModel);
            propertiesModel = _spClosure.find(null, RDFS.range, null);
            addSubjects(properties, propertiesModel);
            propertiesModel = _spClosure.find(null, RDFS.subPropertyOf, null);
            addSubjects(properties, propertiesModel);
            // addObjects(properties, propertiesModel); // not needed ?
            // add everything in predicate position? ... !!!
        } catch (Exception e) {
            error(e);
        }
        return properties;
    }

    Collection getSuperProperties(Resource property) {
        return getValues(_model, property, RDFS.subPropertyOf);
        // _model ist not entirely correct: does not work if you
        // use a subproperty of subPropertyOf !!!
        // -> need for a "find" that also evaluates the subproperties
        // (or: walk through all subproperties)
    }

    // override in subclasses
    public Resource getInverseProperty(Resource property) {
        return null;
    }

    // FACET
    // override in subclasses
    public Resource getAssociatedFacet(Resource property) {
        return null;
    }

    Collection getDomain(Resource property) {
        return getValues(_rdfSchemaModel, property, RDFS.domain);
    }

    public Resource getRange(Resource property) {
        Collection ranges = getValues(_rdfSchemaModel, property, RDFS.range);
        if (ranges.isEmpty())
            return null;
        else if (ranges.size() == 1)
            return (Resource) ranges.iterator().next();
        else {
            error("property has more than one range: " + property + " ; ranges = " + ranges);
            return null;
        }
    }

    // get the models and compute closures / RDFSchemaModel

    boolean getModels(String classesFileName, String instancesFileName) {
        // returns false if fatal error occurs
        _rdfFactory = new RDFFactoryImpl();
        Model rdfsModel;
        Model rdfModel;
        try {
            rdfsModel = parseFile(classesFileName);
        } catch (Exception e) {
            error(e.toString());
            return false;
        }
        try {
            rdfModel = parseFile(instancesFileName);
        } catch (Exception e) {
            error(e.toString());
            return false;
        }
        try {
            // use unite ... !!!
            // _model = SetOperations.union(rdfsModel, rdfModel);
            // transform new to old namespace:
            // (at the moment, we do not reverse this effect when
            //  saving ... !!!)
            
            // rwf Hack this to use the new namespace.  Why was the old one intentionally used?
            _model = _rdfFactory.createModel();
            Util.filterModel(
                rdfsModel,
                _model,
                new NamespaceChanger(
                    org.w3c.rdf.vocabulary.rdf_schema_19990303.RDFS._Namespace,
                    org.w3c.rdf.vocabulary.rdf_schema_200001.RDFS._Namespace));
            Util.filterModel(
                rdfModel,
                _model,
                new NamespaceChanger(
                    org.w3c.rdf.vocabulary.rdf_schema_19990303.RDFS._Namespace,
                    org.w3c.rdf.vocabulary.rdf_schema_200001.RDFS._Namespace));
            Model transSPClosure = RDFSchemaModel.computeClosure(_model, RDFS.subPropertyOf);
            _spClosure = SetOperations.union(_model, transSPClosure);
            Model transClosure = RDFSchemaModel.computeClosure(_model, RDFS.subClassOf);
            SetOperations.unite(transClosure, transSPClosure);
            _rdfSchemaModel = new RDFSchemaModel(_model, transClosure);
            // RDFUtil.printStatements(..., System.out);
        } catch (Exception exc) {
            error(exc);
            return false;
        }
        return true;
    }

    // #RV
    /* Hack to load a Project from InputStreams */
    boolean getModels(InputSource classes, InputSource instances) {
        // returns false if fatal error occurs
        _rdfFactory = new RDFFactoryImpl();
        Model rdfsModel;
        Model rdfModel;
        try {
            rdfsModel = parseInputSource(classes);
        } catch (Exception e) {
            error(e.toString());
            return false;
        }
        try {
            rdfModel = parseInputSource(instances);
        } catch (Exception e) {
            error(e.toString());
            return false;
        }
        try {
            // use unite ... !!!
            // _model = SetOperations.union(rdfsModel, rdfModel);
            // transform new to old namespace:
            // (at the moment, we do not reverse this effect when
            //  saving ... !!!)
            _model = _rdfFactory.createModel();
//            Util.filterModel(
//                rdfsModel,
//                _model,
//                new NamespaceChanger(
//                    org.w3c.rdf.vocabulary.rdf_schema_200001.RDFS._Namespace,
//                    org.w3c.rdf.vocabulary.rdf_schema_19990303.RDFS._Namespace));
//            Util.filterModel(
//                rdfModel,
//                _model,
//                new NamespaceChanger(
//                    org.w3c.rdf.vocabulary.rdf_schema_200001.RDFS._Namespace,
//                    org.w3c.rdf.vocabulary.rdf_schema_19990303.RDFS._Namespace));
            Model transSPClosure = RDFSchemaModel.computeClosure(_model, RDFS.subPropertyOf);
            _spClosure = SetOperations.union(_model, transSPClosure);
            Model transClosure = RDFSchemaModel.computeClosure(_model, RDFS.subClassOf);
            SetOperations.unite(transClosure, transSPClosure);
            _rdfSchemaModel = new RDFSchemaModel(_model, transClosure);
            // RDFUtil.printStatements(..., System.out);
        } catch (Exception exc) {
            error(exc);
            return false;
        }
        return true;
    }
    // #RV

    // basic RDF[S] stuff

    Model parseFile(String fileName) throws Exception {
        // RDFUtil.parse does not do a good job for files (esp. on Windows)
        /*
        if (fileName != null) {
          System.out.print("Parsing " + fileName + ". ");
          System.out.flush();
        }
        */
        Model model = _rdfFactory.createModel();
        if (fileName != null) {
            RDFParser parser = _rdfFactory.createParser();
            BufferedReader reader = URIUtilities.createBufferedReader(URIUtilities.createURI(fileName));
            InputSource source = new InputSource(reader);
            source.setSystemId(_namespace);
            // NOT: _namespaces.getDefaultNamespace()
            // this is not correct for included projects!
            RDFConsumer consumer = new ModelConsumer(model);
            parser.parse(source, consumer);
            reader.close();
        }
        /*
        if (fileName != null)
          System.out.println("done.");
        */
        return model;
    }

    // #RV
    Model parseInputSource(InputSource source) throws Exception {
        // RDFUtil.parse does not do a good job for files (esp. on Windows)
        Model model = _rdfFactory.createModel();
        if (source != null) {
            RDFParser parser = _rdfFactory.createParser();
            source.setSystemId(_namespace);
            // NOT: _namespaces.getDefaultNamespace()
            // this is not correct for included projects!
            RDFConsumer consumer = new ModelConsumer(model);
            parser.parse(source, consumer);
        }
        return model;
    }
    // #RV

    public Collection getValues(Model model, Resource resource, Resource property) {
        // use addObjects ... ???
        HashSet values = new HashSet();
        try {
            Model resultModel = model.find(resource, property, null);
            for (Enumeration tripleEnum = resultModel.elements(); tripleEnum.hasMoreElements();) {
                Statement statement = (Statement) tripleEnum.nextElement();
                RDFNode value = statement.object();
                values.add(value);
            }
        } catch (Exception e) {
            error(e);
        }
        return values;
    }

    public Collection getStringValues(Model model, Resource resource, Resource property) {
        Collection values = getValues(model, resource, property);
        ArrayList stringValues = new ArrayList();
        for (Iterator valueIterator = values.iterator(); valueIterator.hasNext();) {
            RDFNode value = (RDFNode) valueIterator.next();
            stringValues.add(getLabel(value));
        }
        return stringValues;
    }

    public RDFNode getValue(Model model, Resource resource, Resource property) {
        Collection values = getValues(model, resource, property);
        if (values.size() == 1)
            try {
                return (RDFNode) values.iterator().next();
            } catch (Exception e) {
                error(e);
                return null;
            } else
            return null;
    }

    public Resource getResourceValue(Model model, Resource resource, Resource property) {
        RDFNode value = getValue(model, resource, property);
        if (value != null && value instanceof Resource)
            return (Resource) value;
        else
            return null;
    }

    public String getStringValue(Model model, Resource resource, Resource property) {
        RDFNode value = getValue(model, resource, property);
        if (value != null)
            return getLabel(value);
        else
            return null;
    }

    public int getIntValue(Model model, Resource resource, Resource property, int defaultValue) {
        String value = getStringValue(model, resource, property);
        if (value == null)
            return defaultValue;
        else { // parse
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    /* unused
    public Float getFloatValue(Model model, Resource resource, 
        Resource property) {
      String value = getStringValue(model, resource, property);
      if (value == null)
        return null;
      else { // parse
        try {
    	return new Float(value);
        } catch (Exception e) {
    	return null;
        }
      }
    }
    */

    // wframe handling

    public WalkerFrame wframe(Resource resource) {
        if (resource != null) {
            WalkerFrame frame = (WalkerFrame) _wframes.get(resource);
            if (frame != null) {
                return frame;
            } else {
                frame = newWalkerFrame(resource);
                _wframes.put(resource, frame);
                return frame;
            }
        } else {
            return null;
        }
    }

    // overwrite in sublcasses
    public WalkerFrame newWalkerFrame(Resource resource) {
        return new RDFFrame(resource, _classes, _properties);
    }

    public Collection wframes(Collection values) {
        // values must be RDFNodes!
        // result list is sorted
        if (values == null)
            return null;
        else {
            ArrayList wframes = new ArrayList();
            for (Iterator valueIterator = values.iterator(); valueIterator.hasNext();) {
                RDFNode value = (RDFNode) valueIterator.next();
                if (value instanceof Resource)
                    wframes.add(wframe((Resource) value));
                else
                    wframes.add(getLabel(value));
            }
            sort(wframes);
            return wframes;
        }
    }

    // auxiliaries

    public String getLabel(RDFNode node) {
        try {
            return node.getLabel();
        } catch (Exception e) {
            error(e);
            return "?";
        }
    }

    public String getNamespace(Resource resource) {
        // be careful: this method might return null; sync it
        // with the one in RDFFrame (guess!) ... !!!
        try {
            return resource.getNamespace();
        } catch (Exception e) {
            error(e);
            return "?";
        }
    }

    public String getLocalName(Resource resource) {
        // see remark in getNamespace
        try {
            return resource.getLocalName();
        } catch (Exception e) {
            error(e);
            return "?";
        }
    }

    public String getURI(Resource resource) {
        try {
            return resource.getURI();
        } catch (Exception e) {
            error(e);
            return "?";
        }
    }

    public void addObjects(HashSet set, Model model) {
        try {
            for (Enumeration tripleEnum = model.elements(); tripleEnum.hasMoreElements();) {
                Statement statement = (Statement) tripleEnum.nextElement();
                set.add(statement.object());
            }
        } catch (Exception e) {
            error(e);
        }
    }

    public void addSubjects(HashSet set, Model model) {
        try {
            for (Enumeration tripleEnum = model.elements(); tripleEnum.hasMoreElements();) {
                Statement statement = (Statement) tripleEnum.nextElement();
                set.add(statement.subject());
            }
        } catch (Exception e) {
            error(e);
        }
    }

    public boolean isSystemResource(Resource resource) {
        String uri = getURI(resource);
        return uri.startsWith(RDF._Namespace) || uri.startsWith(RDFS._Namespace) || isEncodingSystemResource(resource);
    }

    public boolean isSimpleSystemProperty(Resource resource) {
        // system properties that are "automatically" handled;
        // we must "remove" isDefinedBy etc. here so they can be
        // handled in walkValues ... !!!
        // "trivial" system properties that are handled like
        // user-defined ones are also excluded (they all start with "_")
        return isSystemResource(resource) && !getLocalName(resource).startsWith("_"); // exclude trivial resources
    }

    // override in sublasses that introduce additional system stuff
    // this is then automatically excluded
    public boolean isEncodingSystemResource(Resource resource) {
        return false;
    }

    // sorting

    Collection sorted(Collection values) {
        ArrayList list = new ArrayList(values);
        sort(list);
        return list;
    }

    void sort(List list) {
        Collections.sort(list, _valueComparator);
    }

    // reporting errors

    public void error(String string) {
        _creator.error(string);
    }

    public void error(Exception exc) {
        _creator.error(exc.toString());
        exc.printStackTrace();
    }

    /*inner*/
    class ValueComparator implements Comparator {

        public int compare(Object v1, Object v2) {
            // does not do the right job for numbers!!
            String v1s;
            String v2s;
            try {
                if (v1 instanceof Resource)
                    v1s = ((Resource) v1).getURI();
                // we should use ns abbrev+local name instead of URI, but
                // we cannot access the ns abbreviations here!!! ...
                else
                    v1s = v1.toString();
            } catch (Exception e) {
                error(e);
                v1s = v1.toString();
            }
            try {
                if (v2 instanceof Resource)
                    v2s = ((Resource) v2).getURI();
                else
                    v2s = v2.toString();
            } catch (Exception e) {
                error(e);
                v2s = v2.toString();
            }
            return v1s.compareTo(v2s);
        }

    }

}
