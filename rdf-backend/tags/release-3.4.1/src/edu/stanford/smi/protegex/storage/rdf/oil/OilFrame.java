package edu.stanford.smi.protegex.storage.rdf.oil;

import java.util.*;

import org.w3c.rdf.model.*;
// import org.w3c.rdf.vocabulary.rdf_schema_19990303.*;
import org.w3c.rdf.vocabulary.rdf_schema_200001.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.*;

import edu.stanford.smi.protegex.storage.rdf.*;


public class OilFrame extends RDFFrame implements OilConstants {

  public OilFrame(Resource resource, 
		  Collection classes, Collection properties) {
    super(resource, classes, properties);
  }

  public boolean isThing() {
    return isOilSystemResource("Top") ||
      _resource.equals(RDFS.Resource) || // should not be necessary, but ...
      _resource.equals(RDFS.ConstraintResource); 
  }

  public boolean isStandardClass() {
    return isOilSystemResource("Class") ||
      _resource.equals(RDFS.Class);
  }

  public boolean isStandardSlot() {
    return isOilSystemResource("Property") ||
      isOilSystemResource("TransitiveProperty") ||
      isOilSystemResource("SymmetricProperty") ||
      isOilSystemResource("FunctionalProperty") ||
      _resource.equals(RDF.Property) ||
      _resource.equals(RDFS.ConstraintProperty);
  }

  boolean isOilSystemResource(String name) {
    try {
      String namespace = _resource.getNamespace();
      String localName = _resource.getLocalName();
      return OILNAMESPACE.equals(namespace) && localName.equals(name);
    } catch (Exception e) { return false; }
  }


}


