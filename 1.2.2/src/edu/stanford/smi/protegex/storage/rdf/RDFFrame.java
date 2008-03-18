package edu.stanford.smi.protegex.storage.rdf;

import java.util.*;

import org.w3c.rdf.model.*;
// import org.w3c.rdf.vocabulary.rdf_schema_19990303.*;
import org.w3c.rdf.vocabulary.rdf_schema_200001.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.*;

import edu.stanford.smi.protegex.storage.walker.*;

public class RDFFrame implements WalkerFrame {

    public Resource _resource;
    public String _namespace;
    public String _localName;
    public Collection _classes;
    public Collection _properties;

    static final String SYSTEMNAMESPACE = "http://protege.stanford.edu/system#";

    public RDFFrame(Resource resource, Collection classes, Collection properties) {
        _resource = resource;
        _classes = classes;
        _properties = properties;
        init();
    }

    public String getName() { // full unique name
        try {
            return _resource.getURI();
        } catch (Exception e) {
            return _localName;
        }
    }

    public String getDisplayName() { // for GUIs/browsers etc.
        return _localName; // rdfs:label?
    }

    public String getNamespace() {
        return _namespace;
    }

    public String getLocalName() {
        return _localName;
    }

    public String toString() {
        return getName();
    }

    public boolean isClass() {
        return _classes.contains(_resource);
    }

    public boolean isSlot() {
        return _properties.contains(_resource);
    }

    public boolean isThing() {
        return _resource.equals(RDFS.Resource) || _resource.equals(RDFS.ConstraintResource);
        // ??? create or use :CONSTRAINT ??? !!!
    }

    public boolean isStandardClass() {
        return _resource.equals(RDFS.Class);
    }

    public boolean isStandardSlot() {
        return _resource.equals(RDF.Property) || _resource.equals(RDFS.ConstraintProperty); // not supported!!!
    }

    public void init() {

        // try to figure out namespace and local name 

        try {
            _namespace = _resource.getNamespace();
            _localName = _resource.getLocalName();
        } catch (Exception e) {
            _localName = "?";
        }

        if (_namespace == null) { // guess
            // System.out.println("no namespace: " + _resource);
            if (_localName.startsWith("http://")) { // or ftp ... !!!
                // this case should already be handled in the RDF API, but ....
                // find last # or /
                int p = _localName.lastIndexOf('#');
                if (p == -1)
                    p = _localName.lastIndexOf('/');
                if (p != -1) { // split
                    _namespace = _localName.substring(0, p + 1);
                    _localName = _localName.substring(p + 1);
                }
            } else if (_localName.startsWith("#")) {
                // should never happen
                // since source.setSystemId(_namespace) is used
                // in RDFFrameWalker.java, but ... !!!
                _localName = _localName.substring(1);
            } // else other strategies (split at first :)
        }

        if (_namespace != null) { // check correctness
            if (_namespace.indexOf(' ') != -1)
                // we should report this!
                _namespace = _namespace.replace(' ', '_');
            // other things?
        }

        // same for local name? is done anyway on export!

        // map system names starting with '_' to originial Protege names
        // this is more or less a hack !!!
        if (_namespace != null && _namespace.equals(SYSTEMNAMESPACE) && _localName.charAt(0) == '_') {
            _namespace = null; // default namespace
            int l = _localName.length();
            StringBuffer newName = new StringBuffer();
            newName.append(':');
            for (int i = 1; i < l; i++) { // skip leading _
                char c = _localName.charAt(i);
                if (c == '_')
                    newName.append('-');
                else
                    newName.append(Character.toUpperCase(c));
            }
            _localName = newName.toString();
        }

    }

}
