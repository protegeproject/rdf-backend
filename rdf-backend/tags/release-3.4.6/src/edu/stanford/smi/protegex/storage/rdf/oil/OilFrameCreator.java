package edu.stanford.smi.protegex.storage.rdf.oil;

import java.util.*;

import org.w3c.rdf.model.*;
import org.w3c.rdf.model.Model;
// import org.w3c.rdf.vocabulary.rdf_schema_19990303.*;
import org.w3c.rdf.vocabulary.rdf_schema_200001.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protegex.storage.rdf.roundtrip.*;
import edu.stanford.smi.protegex.storage.walker.*;
import edu.stanford.smi.protegex.storage.walker.protege.*;

public class OilFrameCreator extends RDFRTFrameCreator implements OilConstants {

    Resource _oilTop;
    Resource _oilPrimitiveClass;
    Resource _oilDefinedClass;
    Resource _oilTransitiveProperty;
    Resource _oilSymmetricProperty;
    Resource _oilFunctionalProperty;
    Resource _oilInverseRelationOfProperty;
    Resource _oilHasPropertyRestrictionProperty;
    Resource _oilCardinality;
    Resource _oilMinCardinality;
    Resource _oilMaxCardinality;
    Resource _oilNumberProperty;
    Resource _oilOnPropertyProperty;
    Resource _oilToClassProperty;
    Resource _oilValueType;
    Resource _oilHasOperandProperty;
    Resource _oilOr;
    Resource _oilRangeProperty;

    HashMap _slotRestrictions;

    int _genidNo; // used for producing genids for Min/MaxCardinalities

    public OilFrameCreator(
        String classesFileName,
        String instancesFileName,
        Namespaces namespaces,
        Collection errors) {
        super(classesFileName, instancesFileName, namespaces, errors);
    }

    public void start() {
        super.start();

        _oilTop = oilSystemResource("Top");
        _oilPrimitiveClass = oilSystemResource("PrimitiveClass");
        _oilDefinedClass = oilSystemResource("DefinedClass");
        _oilTransitiveProperty = oilSystemResource("TransitiveProperty");
        _oilSymmetricProperty = oilSystemResource("SymmetricProperty");
        _oilFunctionalProperty = oilSystemResource("FunctionalProperty");
        _oilInverseRelationOfProperty = oilSystemResource("inverseRelationOf");
        _oilHasPropertyRestrictionProperty = oilSystemResource("hasPropertyRestriction");
        _oilCardinality = oilSystemResource("Cardinality");
        _oilMinCardinality = oilSystemResource("MinCardinality");
        _oilMaxCardinality = oilSystemResource("MaxCardinality");
        _oilNumberProperty = oilSystemResource("number");
        _oilOnPropertyProperty = oilSystemResource("onProperty");
        _oilToClassProperty = oilSystemResource("toClass");
        _oilValueType = oilSystemResource("ValueType");
        _oilHasOperandProperty = oilSystemResource("hasOperand");
        _oilOr = oilSystemResource("Or");
        _oilRangeProperty = oilSystemResource("range");

        _slotRestrictions = new HashMap();
        _genidNo = 0;
    }

    public void addInverseSlot(Resource slotResource, WalkerFrame inverseSlot) {
        // entirely replaces super.addInverseSlot(slotResource, inverseSlot); 
        add(_rdfsModel, statement(slotResource, _oilInverseRelationOfProperty, resource(inverseSlot)));
    }

    public void createInstance(WalkerFrame inst, WalkerFrame type, String documentation) {
        Resource instResource = resource(inst);
        Model model;
        if (isOilSystemFrame(type)) // put expressions and axioms into rdfs
            model = _rdfsModel;
        else
            model = _rdfModel;
        add(model, statement(instResource, RDF.type, resource(type)));
        addComment(model, instResource, documentation);
        addLabel(model, inst);
    }

    public void createSlot(
        WalkerFrame slot,
        WalkerFrame type,
        Collection superslots,
        WalkerFrame inverseSlot,
        WalkerFrame associatedFacet,
    // FACET
    WalkerSlotRestriction slotRestriction, String documentation) {
        // just do the normal thing:
        super.createSlot(slot, type, superslots, inverseSlot, associatedFacet, // FACET
        slotRestriction, documentation);
        // additionally, remember slotRestriction (for attachSlot below) ...
        _slotRestrictions.put(slot, slotRestriction);
        // ... and create or expression for multiple allowed classes:
        if (slotRestriction != null && slotRestriction.isInstance()) {
            Collection allowedClasses = slotRestriction.getAllowedClasses();
            if (allowedClasses != null && allowedClasses.size() > 1) {
                Resource orResource = getOrResource(allowedClasses);
                add(_rdfsModel, statement(resource(slot), _oilRangeProperty, orResource));
            }
        }
    }

    public void attachSlot(
        WalkerFrame cls,
        WalkerFrame slot,
        boolean direct,
        WalkerSlotRestriction overriddenSlotRestriciton,
        String overriddenDocumentation) {
        // HACK: first do the Protege encoding ...
        super.attachSlot(cls, slot, direct, overriddenSlotRestriciton, overriddenDocumentation);
        // ... then the one for OIL: ... !!!
        WalkerSlotRestriction slotRestriction = null;
        if (direct && overriddenSlotRestriciton == null)
            slotRestriction = (WalkerSlotRestriction) _slotRestrictions.get(slot);
        else
            slotRestriction = overriddenSlotRestriciton;
        if (slotRestriction != null) {
            Resource clsResource = resource(cls);
            Resource slotResource = resource(slot);
            // range (-> ValueType)
            if (overriddenSlotRestriciton != null) { // only if overridden
                if (slotRestriction.isInstance()) { // we handle only this case ... !!!
                    Collection allowedClasses = slotRestriction.getAllowedClasses();
                    if (allowedClasses != null)
                        addRange(clsResource, slotResource, allowedClasses);
                }
            }
            // cardinalities (always since OIL does not have global cardinalities)
            int minCard = slotRestriction.getMinimumCardinality();
            int maxCard = slotRestriction.getMaximumCardinality();
            if (minCard == maxCard) {
                addCardinality(clsResource, slotResource, _oilCardinality, minCard);
            } else {
                if (minCard != 0)
                    addCardinality(clsResource, slotResource, _oilMinCardinality, minCard);
                if (maxCard != KnowledgeBase.MAXIMUM_CARDINALITY_UNBOUNDED)
                    addCardinality(clsResource, slotResource, _oilMaxCardinality, maxCard);
            }
            // anything else???
        }
    }

    void addRange(Resource clsResource, Resource slotResource, Collection allowedClasses) {
        // 1. hasPropertyRestriction to gensym
        //    (where I can tell from the gensym if it came from Protege or not)
        // 2. defintion for gensym:
        //    type: ValueType
        //    onProperty 
        //    toClass -> disjunction of allowed classes (or one class/Top)
        Resource valueTypeResource = oilGenidResource("valuetype");
        add(_rdfsModel, statement(clsResource, _oilHasPropertyRestrictionProperty, valueTypeResource));
        add(_rdfsModel, statement(valueTypeResource, RDF.type, _oilValueType));
        add(_rdfsModel, statement(valueTypeResource, _oilOnPropertyProperty, slotResource));
        Resource rangeResource;
        if (allowedClasses.isEmpty())
            rangeResource = _oilTop;
        else if (allowedClasses.size() == 1)
            rangeResource = resource((WalkerFrame) allowedClasses.iterator().next());
        else // make or expression
            rangeResource = getOrResource(allowedClasses);
        add(_rdfsModel, statement(valueTypeResource, _oilToClassProperty, rangeResource));
    }

    Resource getOrResource(Collection allowedClasses) {
        Resource rangeResource = oilGenidResource("or");
        // type -> Or
        add(_rdfsModel, statement(rangeResource, RDF.type, _oilOr));
        // hasOperand for each allowed class:
        for (Iterator acIterator = allowedClasses.iterator(); acIterator.hasNext();) {
            WalkerFrame allowedClass = (WalkerFrame) acIterator.next();
            Resource allowedResource = resource(allowedClass);
            add(_rdfsModel, statement(rangeResource, _oilHasOperandProperty, allowedResource));
        }
        return rangeResource;
    }

    void addCardinality(
        Resource clsResource,
        Resource slotResource,
        Resource oilCardinalityResource,
        int cardinality) {
        // 1. hasPropertyRestriction to gensym
        //    (where I can tell from the gensym if it came from Protege or not)
        // 2. defintion for gensym:
        //    type: MinCardinality, MaxCardinality, or Cardinality
        //    number, onProperty, toClass (Top)
        Resource cardResource = oilGenidResource("card");
        add(_rdfsModel, statement(clsResource, _oilHasPropertyRestrictionProperty, cardResource));
        add(_rdfsModel, statement(cardResource, RDF.type, oilCardinalityResource));
        add(_rdfsModel, statement(cardResource, _oilNumberProperty, literal(cardinality)));
        add(_rdfsModel, statement(cardResource, _oilOnPropertyProperty, slotResource));
        add(_rdfsModel, statement(cardResource, _oilToClassProperty, _oilTop));
    }

    public void addOwnSlotValues(WalkerFrame instance, WalkerFrame slot, Collection values) {
        // map oil:type and oil:properties to correct OIL
        if (isOilSystemFrame(slot, "type")) {
            // see if it is a defined class
            if (values.contains("defined")) { // change type
                Resource resource = resource(instance);
                remove(_rdfsModel, statement(resource, RDF.type, getStandardClass()));
                add(_rdfsModel, statement(resource, RDF.type, _oilDefinedClass));
            }
        } else if (isOilSystemFrame(slot, "properties")) {
            // additional rdf:type statements
            Resource resource = resource(instance);
            if (values.contains("transitive"))
                add(_rdfsModel, statement(resource, RDF.type, _oilTransitiveProperty));
            if (values.contains("symmetric"))
                add(_rdfsModel, statement(resource, RDF.type, _oilSymmetricProperty));
            if (values.contains("functional"))
                add(_rdfsModel, statement(resource, RDF.type, _oilFunctionalProperty));
        } else {
            // super.addOwnSlotValues(instance, slot, values);
            // we cannot use super... since most stuff goes into _rdfsModel!
            Resource instanceResource = resource(instance);
            Model model; // decide to which model to add
            if (_classes.contains(instanceResource)
                || _slots.contains(instanceResource)
                || isOilSystemFrame(slot)) // !
                model = _rdfsModel;
            else
                model = _rdfModel;
            Resource property = resource(slot);
            addValues(model, instanceResource, property, values);
        }
    }

    public Resource getThing() {
        return _oilTop;
    }

    public Resource getStandardClass() {
        return RDFS.Class; // _oilPrimitiveClass also works here!!!
    }

    /* is inherited anyway
    public Resource getStandardSlot() {
      return RDFS.Property;
    }
    */

    // auxiliaries

    Resource oilSystemResource(String name) {
        return resource(OILNAMESPACE, name);
    }

    Resource oilGenidResource(String name) {
        return resource(OILGENIDNAMESPACE, name + "_genid" + (_genidNo++));
    }

    boolean isOilSystemFrame(WalkerFrame frame) {
        return frame.getNamespace().equals(OILNAMESPACE);
    }

    boolean isOilSystemFrame(WalkerFrame frame, String name) {
        return frame.getNamespace().equals(OILNAMESPACE) && frame.getLocalName().equals(name);
    }

}
