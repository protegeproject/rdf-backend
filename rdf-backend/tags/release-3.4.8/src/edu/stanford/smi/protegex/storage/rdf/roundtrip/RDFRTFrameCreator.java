package edu.stanford.smi.protegex.storage.rdf.roundtrip;

import java.io.*;
import java.util.*;

import org.w3c.rdf.model.*;
// import org.w3c.rdf.vocabulary.rdf_schema_19990303.*;
import org.w3c.rdf.vocabulary.rdf_schema_200001.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.rdf.*;
import edu.stanford.smi.protegex.storage.walker.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;


public class RDFRTFrameCreator extends RDFFrameCreator 
			       implements RDFRTConstants {

  Resource _ROLEProperty;
  Resource _INVERSEPROPERTYProperty;
  Resource _ASSOCIATEDFACETProperty; // FACET
  Resource _HASFACETProperty; // FACET
  Resource _RANGEProperty;
  Resource _ALLOWEDCLASSESProperty;
  Resource _ALLOWEDPARENTSProperty;
  Resource _ALLOWEDVALUESProperty;
  Resource _DEFAULTVALUESProperty;
  Resource _VALUESProperty;
  Resource _MINCARDINALITYProperty;
  Resource _MAXCARDINALITYProperty;
  Resource _MINVALUEProperty;
  Resource _MAXVALUEProperty;
  Resource _OVERRIDINGPROPERTYClass;
  Resource _DOMAINProperty;
  Resource _OVERRIDDENPROPERTYProperty;

  HashSet _clsSlotNames;


  public RDFRTFrameCreator(String classesFileName, String instancesFileName,
      Namespaces namespaces, Collection errors) {
    super(classesFileName, instancesFileName, namespaces, errors);
  }

// #RV
  public RDFRTFrameCreator(Writer rdfsModel, Writer rdfModel,
      Namespaces namespaces, Collection errors) {
    super(rdfsModel, rdfModel, namespaces, errors);
  }
// #RV

  public void start() {
    super.start();
    _clsSlotNames = new HashSet();
    _ROLEProperty = systemResource(ROLE);
    _INVERSEPROPERTYProperty = systemResource(INVERSEPROPERTY);
    _ASSOCIATEDFACETProperty = systemResource(ASSOCIATEDFACET); // FACET
    _HASFACETProperty = systemResource(HASFACET); // FACET
    _RANGEProperty = systemResource(RANGE);
    _ALLOWEDCLASSESProperty = systemResource(ALLOWEDCLASSES);
    _ALLOWEDPARENTSProperty = systemResource(ALLOWEDPARENTS);
    _ALLOWEDVALUESProperty = systemResource(ALLOWEDVALUES);
    _DEFAULTVALUESProperty = systemResource(DEFAULTVALUES);
    _VALUESProperty = systemResource(VALUES);
    _MINCARDINALITYProperty = systemResource(MINCARDINALITY);
    _MAXCARDINALITYProperty = systemResource(MAXCARDINALITY);
    _MINVALUEProperty = systemResource(MINVALUE);
    _MAXVALUEProperty = systemResource(MAXVALUE);
    _OVERRIDINGPROPERTYClass = systemResource(OVERRIDINGPROPERTY);
    _DOMAINProperty = systemResource(DOMAIN);
    _OVERRIDDENPROPERTYProperty = systemResource(OVERRIDDENPROPERTY);
  }

  public void addIsAbstract(Resource clsResource, boolean isAbstract) {
    if (isAbstract) {
      add(_rdfsModel, 
	statement(clsResource, _ROLEProperty, literal(ABSTRACT)));
    }
  }

  public void addInverseSlot(Resource slotResource, WalkerFrame inverseSlot) {
    add(_rdfsModel, 
      statement(slotResource, _INVERSEPROPERTYProperty,
	resource(inverseSlot)));
  }

  // FACET
  public void addAssociatedFacet(Resource slotResource, 
      WalkerFrame associatedFacet) {
    add(_rdfsModel, 
      statement(slotResource, _ASSOCIATEDFACETProperty,
	resource(associatedFacet)));
  }


  public void addSlotRestriction(Resource slotResource,
      WalkerSlotRestriction slotRestriction) {

    // value type
    if (slotRestriction.isAny()) {
      // nothing to do, not even range
    } else if (slotRestriction.isInstance()) {
      // addRange(slotResource, INSTANCE); // is implicit
      // when rdfs:range != Literal and psys:range != CLS ... ???
      WalkerFrame allowedClass = slotRestriction.getAllowedClass();
      if (allowedClass != null)
        add(_rdfsModel, 
	  statement(slotResource, RDFS.range, resource(allowedClass)));
      else
        add(_rdfsModel, statement(slotResource, RDFS.range, RDFS.Resource));
      Collection allowedClasses = slotRestriction.getAllowedClasses();
      if (allowedClasses != null && allowedClasses.size() > 1)
	// export only necessary if there is more than 1 allowed class!!!
	addValues(_rdfsModel, slotResource, 
	  _ALLOWEDCLASSESProperty, allowedClasses);
    } else if (slotRestriction.isClass()) {
      addRange(slotResource, CLS);
      add(_rdfsModel, statement(slotResource, RDFS.range, RDFS.Class)); // dito
      Collection allowedParents = slotRestriction.getAllowedParents();
      if (allowedParents != null)
	addValues(_rdfsModel, slotResource, 
	  _ALLOWEDPARENTSProperty, allowedParents);
    } else { // Literal (String, Integer, ...)
      add(_rdfsModel, statement(slotResource, RDFS.range, RDFS.Literal));
      if (slotRestriction.isBoolean())
        addRange(slotResource, BOOLEAN);
      else if (slotRestriction.isFloat())
        addRange(slotResource, FLOAT);
      else if (slotRestriction.isInteger())
        addRange(slotResource, INTEGER);
      else if (slotRestriction.isString())
	{} // is implicit when rdfs:range = Literal !!!
      else if (slotRestriction.isSymbol()) {
        addRange(slotResource, SYMBOL);
	Collection allowedValues = slotRestriction.getAllowedValues();
	if (allowedValues != null)
	  addValues(_rdfsModel, slotResource, 
	    _ALLOWEDVALUESProperty, allowedValues);
      }
    }

    // cardinalities
    int minCard = slotRestriction.getMinimumCardinality();
    if (minCard != 0)
      add(_rdfsModel, 
	statement(slotResource, _MINCARDINALITYProperty, literal(minCard)));
    int maxCard = slotRestriction.getMaximumCardinality();
    if (maxCard != KnowledgeBase.MAXIMUM_CARDINALITY_UNBOUNDED) // -1, not 0
      add(_rdfsModel, 
	statement(slotResource, _MAXCARDINALITYProperty, literal(maxCard)));

    // min/max values
    String minValue = slotRestriction.getMinimumValue();
    if (minValue != null)
      add(_rdfsModel, statement(slotResource, _MINVALUEProperty, 
	literal(minValue)));
    String maxValue = slotRestriction.getMaximumValue();
    if (maxValue != null)
      add(_rdfsModel, statement(slotResource, _MAXVALUEProperty, 
	literal(maxValue)));

    // default and (template) slot values
    Collection defaultValues = slotRestriction.getDefaultValues();
    if (defaultValues != null)
      addValues(_rdfsModel, slotResource, 
	_DEFAULTVALUESProperty, defaultValues);
    Collection values = slotRestriction.getValues();
    if (values != null)
      addValues(_rdfsModel, slotResource, _VALUESProperty, values);
  }


  void addRange(Resource slotResource, String rangeName) {
    add(_rdfsModel, 
      statement(slotResource, _RANGEProperty, literal(rangeName)));
  }


  public void overrideSlotRestriction(WalkerFrame cls, WalkerFrame slot,
      Resource clsResource, Resource slotResource, 
      WalkerSlotRestriction slotRestriction,
      String overriddenDocumentation) {
    // we reduce this case to the "normal" slot attachment case
    // simply by introducing a new slot resource which is an
    // instance of OverridingProperty
    String name = getClsSlotName(cls, slot);
    Resource overridingPropertyResource = systemResource(name);
    add(_rdfsModel,
      statement(overridingPropertyResource, RDF.type, 
	_OVERRIDINGPROPERTYClass));
    add(_rdfsModel,
      statement(overridingPropertyResource, _DOMAINProperty,
	clsResource));
    add(_rdfsModel,
      statement(overridingPropertyResource, _OVERRIDDENPROPERTYProperty,
	slotResource));
    // alternativeley, one could make this a real sub property
    // of the overridden one ... !!!
    addSlotRestriction(overridingPropertyResource, slotRestriction);
    if (overriddenDocumentation != null)
      addComment(_rdfsModel, overridingPropertyResource, 
	overriddenDocumentation);
    // FACET
    // overridden facets
    Map facetOverrides = slotRestriction.getFacetOverrides();
    if (facetOverrides != null) {
      for (Iterator facetOverridesIterator = 
                    facetOverrides.entrySet().iterator();
           facetOverridesIterator.hasNext();) {
	Map.Entry facetOverride = (Map.Entry)facetOverridesIterator.next();
	WalkerFrame facet = (WalkerFrame)facetOverride.getKey();
	Resource facetResource = resource(facet);
	add(_rdfsModel,
	  statement(overridingPropertyResource, _HASFACETProperty,
	    facetResource));
	Collection values = (Collection)facetOverride.getValue();
	addValues(_rdfsModel, overridingPropertyResource, 
	  facetResource, values);
      }
    }
  }

  String getClsSlotName(WalkerFrame cls, WalkerFrame slot) {
    String name = cls.getLocalName() + "_" + slot.getLocalName();
    // rename if already exists ...
    while (_clsSlotNames.contains(name))
      name = name + "_";
    _clsSlotNames.add(name);
    return name;
  }


  // auxiliaries

  public Resource systemResource(String name) {
    return resource(SYSTEMNAMESPACE, name);
  }


}


