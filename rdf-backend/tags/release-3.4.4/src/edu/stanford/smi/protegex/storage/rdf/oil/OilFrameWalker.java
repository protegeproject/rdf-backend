package edu.stanford.smi.protegex.storage.rdf.oil;

import java.util.*;

import org.w3c.rdf.model.*;

import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.rdf.roundtrip.*;
import edu.stanford.smi.protegex.storage.walker.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;


public class OilFrameWalker extends RDFRTFrameWalker implements OilConstants {

  Resource _oilClassResource;
  Resource _oilTypeResource;
  Resource _oilPropertiesResource;
  Resource _oilInverseRelationOfProperty;

  HashSet _primitiveClasses; // of Resource
  HashSet _definedClasses;
  HashSet _transitiveProperties;
  HashSet _symmetricProperties;
  HashSet _functionalProperties;

  public OilFrameWalker(String classesFileName, String instancesFileName,
      String namespace, Namespaces namespaces) {
    super(classesFileName, instancesFileName, namespace, namespaces);
  }

  public boolean init() {
    if (!super.init()) return false;
    _primitiveClasses = new HashSet();
    _definedClasses = new HashSet();
    _transitiveProperties = new HashSet();
    _symmetricProperties = new HashSet();
    _functionalProperties = new HashSet();
    _oilClassResource = oilSystemResource("Class");
    _oilTypeResource = oilSystemResource("type");
    _oilPropertiesResource = oilSystemResource("properties");
    _oilInverseRelationOfProperty = oilSystemResource("inverseRelationOf");
    return true;
  }

  public void createCls(Resource cls, Collection superclasses,
      Resource type, boolean isAbstract, String documentation) {
    if (isOilSystemResource(type)) {
      String localName = getLocalName(type);
      if (localName.equals("PrimitiveClass") ||
	  localName.equals("DefinedClass")) {
	type = _oilClassResource;
	if (localName.equals("DefinedClass"))
	  _definedClasses.add(cls);
	else
	  _primitiveClasses.add(cls);
      }
    }
    super.createCls(cls, superclasses, type, isAbstract, documentation);
  }

  public Resource getInverseProperty(Resource property) {
    return getResourceValue(_model, property, _oilInverseRelationOfProperty);
  }

  public void creatingSlot(Resource property, Resource type) {
    if (isOilSystemResource(type)) {
      String localName = getLocalName(type);
      // these cases are needed since you are not forced to
      // put a property in rdf:Property (or are you???)
      if (localName.equals("TransitiveProperty"))
	_transitiveProperties.add(property);
      else if (localName.equals("SymmetricProperty"))
	_symmetricProperties.add(property);
      else if (localName.equals("FunctionalProperty"))
	_functionalProperties.add(property);
    }
  }

  public void walkValues() { 
    super.walkValues();
    // add primitive/defined values to classes
    Collection primitiveCollection =
      CollectionUtilities.createCollection("primitive");
    Collection definedCollection =
      CollectionUtilities.createCollection("defined");
    for (Iterator pcIterator = _primitiveClasses.iterator();
	 pcIterator.hasNext();) {
      Resource cls = (Resource)pcIterator.next();
      _creator.addOwnSlotValues(wframe(cls), wframe(_oilTypeResource),
	primitiveCollection);
    }
    for (Iterator dcIterator = _definedClasses.iterator();
	 dcIterator.hasNext();) {
      Resource cls = (Resource)dcIterator.next();
      _creator.addOwnSlotValues(wframe(cls), wframe(_oilTypeResource),
	definedCollection);
    }
  }

  public void walkUnhandledTypes() {
    // handle marker classes (TransitiveProperty etc.)
    for (Iterator utIterator = _unhandledTypes.entrySet().iterator();
	 utIterator.hasNext();) {
      Map.Entry unhandled = (Map.Entry)utIterator.next();
      Resource resource = (Resource)unhandled.getKey();
      Collection types = (Collection)unhandled.getValue();
      int handled = 0; // count handled types
      for (Iterator typeIterator = types.iterator();
	   typeIterator.hasNext();) {
	Resource type = (Resource)typeIterator.next();
	if (isOilSystemResource(type)) {
	  String name = getLocalName(type);
	  if (name.equals("TransitiveProperty")) {
	    _transitiveProperties.add(resource); handled++;
	  } else if (name.equals("SymmetricProperty")) {
	    _symmetricProperties.add(resource); handled++;
	  } else if (name.equals("FunctionalProperty")) {
	    _functionalProperties.add(resource); handled++;
	  }
	}
      }
      if (types.size() > handled)
	error("resource with more than one type not completely handled: " 
	  + resource
	  + " ; types = " + types); // some of the types may be handled ... !!!
    }
  }

  void walkSpecialProperties(Collection properties, String marker) {
    for (Iterator propertyIterator = properties.iterator();
	 propertyIterator.hasNext();) {
      Resource property = (Resource)propertyIterator.next();
      _creator.addOwnSlotValues(wframe(property),
        wframe(_oilPropertiesResource),
        CollectionUtilities.createCollection(marker));
    }
  }

  public void finish() {
    super.finish();
    walkSpecialProperties(_transitiveProperties, "transitive");
    walkSpecialProperties(_symmetricProperties, "symmetric");
    walkSpecialProperties(_functionalProperties, "functional");
  }


  public WalkerFrame newWalkerFrame(Resource resource) {
    return new OilFrame(resource, _classes, _properties);
  }


  // auxiliaries

  Resource oilSystemResource(String name) {
    return resource(OILNAMESPACE, name);
  }

  boolean isOilSystemResource(Resource resource) {
    String namespace = getNamespace(resource);
    return OILNAMESPACE.equals(namespace) ||
      OILGENIDNAMESPACE.equals(namespace);
  }

  public boolean isEncodingSystemResource(Resource resource) {
    // we still allow some of the roundtrip encodings:
    return super.isEncodingSystemResource(resource) ||
      isOilSystemResource(resource);
  }

  public boolean isSimpleSystemProperty(Resource resource) {
    // most OIL properties must be handled in walkValues!
    return super.isSimpleSystemProperty(resource) &&
      (!isOilSystemResource(resource) ||
       resource.equals(_oilInverseRelationOfProperty)); // ooops :-)
  }


}


