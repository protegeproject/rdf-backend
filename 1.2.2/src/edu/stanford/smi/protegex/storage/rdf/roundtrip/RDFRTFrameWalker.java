package edu.stanford.smi.protegex.storage.rdf.roundtrip;

import java.util.*;

import org.w3c.rdf.model.*;
import org.w3c.rdf.model.Model;
// import org.w3c.rdf.vocabulary.rdf_schema_19990303.*;
import org.w3c.rdf.vocabulary.rdf_schema_200001.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.*;
import org.xml.sax.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protegex.storage.rdf.*;
import edu.stanford.smi.protegex.storage.walker.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;

public class RDFRTFrameWalker extends RDFFrameWalker implements RDFRTConstants {

    public NodeFactory _nodeFactory;
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

    public RDFRTFrameWalker(
        String classesFileName,
        String instancesFileName,
        String namespace,
        Namespaces namespaces) {
        super(classesFileName, instancesFileName, namespace, namespaces);
    }

    // #RV
    public RDFRTFrameWalker(InputSource classes, InputSource instances, String namespace, Namespaces namespaces) {
        super(classes, instances, namespace, namespaces);
    }
    // #RV

    public boolean init() {
        // return false if fatal error occurs
        if (!super.init())
            return false;
        try {
            _nodeFactory = _model.getNodeFactory();
        } catch (Exception e) {
            error(e);
            return false;
        }
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
        return true;
    }

    public boolean getIsAbstract(Resource cls) {
        String value = getStringValue(_model, cls, _ROLEProperty);
        return ABSTRACT.equals(value);
    }

    public Resource getInverseProperty(Resource property) {
        return getResourceValue(_model, property, _INVERSEPROPERTYProperty);
    }

    // FACET
    public Resource getAssociatedFacet(Resource property) {
        return getResourceValue(_model, property, _ASSOCIATEDFACETProperty);
    }

    public WalkerSlotRestriction getSlotRestriction(Resource property) {

        RDFRTSlotRestriction slotRestriction = null;
        Resource rangeResource = getRange(property);
        String protegeRange = getStringValue(_model, property, _RANGEProperty);

        if (protegeRange == null) { // guess range
            if (rangeResource == null) {
                protegeRange = ANY;
            } else {
                if (rangeResource.equals(RDFS.Literal) || getSuperclasses(rangeResource).contains(RDFS.Literal))
                    protegeRange = STRING;
                // try to guess the data type if subclass of Literal is used?
                // else // check for XML Schema data types ... !!!
                else
                    protegeRange = INSTANCE; // guess CLS ... !!! ???
            }
        }

        if (protegeRange.equals(INSTANCE)) {
            WalkerFrame rangeFrame = wframe(rangeResource);
            Collection allowedClasses = wframes(getValues(_model, property, _ALLOWEDCLASSESProperty));
            if (allowedClasses == null || allowedClasses.isEmpty())
                // hmmm, this is not correct: we cannot have empty
                // allowedClasses anymore! change this? ... !!!
                allowedClasses = CollectionUtilities.createCollection(rangeFrame);
            slotRestriction = new RDFRTSlotRestriction(rangeFrame, allowedClasses);
            // handle case where rangeResource == null ... ???
        } else if (protegeRange.equals(CLS)) {
            slotRestriction = new RDFRTSlotRestriction(protegeRange);
            Collection allowedParents = wframes(getValues(_model, property, _ALLOWEDPARENTSProperty));
            slotRestriction.setAllowedParents(allowedParents);
        } else if (protegeRange.equals(SYMBOL)) {
            slotRestriction = new RDFRTSlotRestriction(protegeRange);
            Collection values = getStringValues(_model, property, _ALLOWEDVALUESProperty);
            slotRestriction.setAllowedValues(values);
        } else {
            slotRestriction = new RDFRTSlotRestriction(protegeRange);
        }

        // cardinalities
        int minCard = getIntValue(_model, property, _MINCARDINALITYProperty, 0);
        slotRestriction.setMinimumCardinality(minCard);
        int maxCard =
            getIntValue(_model, property, _MAXCARDINALITYProperty, KnowledgeBase.MAXIMUM_CARDINALITY_UNBOUNDED);
        // unrestricted is now -1, not 0 any more!
        slotRestriction.setMaximumCardinality(maxCard);

        // min/max values
        String minValue = getStringValue(_model, property, _MINVALUEProperty);
        slotRestriction.setMinimumValue(minValue);
        String maxValue = getStringValue(_model, property, _MAXVALUEProperty);
        slotRestriction.setMaximumValue(maxValue);

        // default and (template) slot values
        Collection defaultValues = wframes(getValues(_model, property, _DEFAULTVALUESProperty));
        slotRestriction.setDefaultValues(defaultValues);
        Collection values = wframes(getValues(_model, property, _VALUESProperty));
        slotRestriction.setValues(values);

        // FACET
        // user-defined facets (in overriding properties)
        Collection facets = getValues(_model, property, _HASFACETProperty);
        for (Iterator facetIterator = facets.iterator(); facetIterator.hasNext();) {
            Resource facet = (Resource) facetIterator.next();
            Collection facetValues = wframes(getValues(_model, property, facet));
            slotRestriction.putFacetOverride(wframe(facet), facetValues);
        }

        return slotRestriction;

    }

    public void walkSlotOverrides() {
        // walk through the OverridingProperty instances and attach them
        HashSet overridingProperties = new HashSet();
        try {
            Model propertiesModel = _model.find(null, RDF.type, _OVERRIDINGPROPERTYClass);
            addSubjects(overridingProperties, propertiesModel);
        } catch (Exception e) {
            error(e);
        }
        for (Iterator propIterator = overridingProperties.iterator(); propIterator.hasNext();) {
            Resource property = (Resource) propIterator.next();
            WalkerSlotRestriction slotRestriction = getSlotRestriction(property);
            Resource domain = getResourceValue(_model, property, _DOMAINProperty);
            Resource overriddenProperty = getResourceValue(_model, property, _OVERRIDDENPROPERTYProperty);
            String overriddenDocumentation = getComment(property);
            if (domain != null && overriddenProperty != null)
                _creator.attachSlot(
                    wframe(domain),
                    wframe(overriddenProperty),
                    false,
                    slotRestriction,
                    overriddenDocumentation);
            else
                error(OVERRIDINGPROPERTY + " corrupt: " + property);
        }
    }

    // auxiliaries

    public Resource resource(String namespace, String name) {
        // probably move this to superclass (RDFFrameWalker) ... ???
        // (incl. _nodeFactory)
        try {
            return _nodeFactory.createResource(namespace, name);
        } catch (Exception e) {
            error(e);
        }
        return null;
    }

    Resource systemResource(String name) {
        return resource(SYSTEMNAMESPACE, name);
    }

    public boolean isEncodingSystemResource(Resource resource) {
        return SYSTEMNAMESPACE.equals(getNamespace(resource));
    }

}
