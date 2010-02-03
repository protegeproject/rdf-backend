package edu.stanford.smi.protegex.storage.walker.protege;

import java.util.*;

public class BaseNamespaces {

    //  static final String NAMESPACES_PROPERTY = "namespaces";
    //  static final String HIDE_NAMESPACES_PROPERTY = "hide_namespaces";

    static final String SYMBOL_DEFAULT_NAMESPACE = "";
    static final String SYMBOL_NO_NAMESPACE = ":";

    String _defaultNamespace;
    int _gensymCounter;

    HashMap _abbrevs; // maps abbrev -> URI
    HashMap _uris; // maps URI -> abbrev

    public BaseNamespaces(String namespace) {
        _defaultNamespace = namespace;
        _abbrevs = new HashMap();
        _uris = new HashMap();
        _gensymCounter = 1;
    }

    // #RV special
    public HashMap includeNamespaces(BaseNamespaces newNamespaces) {
        // add newNamespaces;
        // handles cases where abbreviations do not fit or two
        // abbreviations map to same URI by returning a HashMap
        // that maps new to current abbreviations
        if (newNamespaces == null) {
            // System.err.println("newNamespaces==null");
            return null;
        }
        HashMap result = new HashMap();
        if (_defaultNamespace != newNamespaces.getDefaultNamespace()) {
            String abbrev = getAbbrev(newNamespaces.getDefaultNamespace());
            if (abbrev == null) {
                abbrev = inventAbbrev(newNamespaces.getDefaultNamespace());
                add(abbrev, newNamespaces.getDefaultNamespace());
            }
            result.put(SYMBOL_DEFAULT_NAMESPACE, abbrev);
        }
        for (Iterator abbrevIterator = (newNamespaces.getAbbrevs()).keySet().iterator(); abbrevIterator.hasNext();) {
            String newAbbrev = (String) abbrevIterator.next();
            String newUri = (String) (newNamespaces.getAbbrevs()).get(newAbbrev);
            String abbrev = null;
            if (newUri == _defaultNamespace) {
                result.put(newAbbrev, SYMBOL_DEFAULT_NAMESPACE);
            } else if (getAbbrev(newUri) != null) {
                abbrev = getAbbrev(newUri);
                if (abbrev != newAbbrev)
                    result.put(newAbbrev, abbrev);
            } else {
                abbrev = inventAbbrev(newUri);
                add(abbrev, newUri);
            }
        }
        // System.err.println("Result-x: "+result.toString());
        return result;
    }

    // #RV special
    public HashMap getModAbbrevMap(BaseNamespaces newNamespaces, HashMap missingNamespaces) {
        // same as includeNamespaces, but put missing namespaces 
        // in missingNamespaces
        if (newNamespaces == null) {
            // System.err.println("newNamespaces==null");
            return null;
        }
        HashMap result = new HashMap();
        if (_defaultNamespace != newNamespaces.getDefaultNamespace()) {
            String abbrev = getAbbrev(newNamespaces.getDefaultNamespace());
            if (abbrev == null && missingNamespaces == null) {
                result.put(SYMBOL_DEFAULT_NAMESPACE, SYMBOL_NO_NAMESPACE);
            } else {
                if (abbrev == null) {
                    abbrev = inventAbbrev(newNamespaces.getDefaultNamespace());
                    missingNamespaces.put(abbrev, newNamespaces.getDefaultNamespace());
                }
                result.put(SYMBOL_DEFAULT_NAMESPACE, abbrev);
            }
        }
        for (Iterator abbrevIterator = (newNamespaces.getAbbrevs()).keySet().iterator(); abbrevIterator.hasNext();) {
            String newAbbrev = (String) abbrevIterator.next();
            String newUri = (String) (newNamespaces.getAbbrevs()).get(newAbbrev);
            String abbrev = null;
            if (newUri == _defaultNamespace) {
                result.put(newAbbrev, SYMBOL_DEFAULT_NAMESPACE);
            } else if (getAbbrev(newUri) != null) {
                abbrev = getAbbrev(newUri);
                if (abbrev != newAbbrev)
                    result.put(newAbbrev, abbrev);
            } else {
                if (missingNamespaces != null) {
                    abbrev = inventAbbrev(newUri);
                    missingNamespaces.put(abbrev, newUri);
                    result.put(newAbbrev, abbrev);
                } else {
                    result.put(newAbbrev, SYMBOL_NO_NAMESPACE);
                }
            }
        }
        // System.err.println("Result-x: "+result.toString());
        return result;
    }

    public String getDefaultNamespace() {
        return _defaultNamespace;
    }

    public String getURI(String abbrev) {
        return (String) _abbrevs.get(abbrev);
    }

    public String getAbbrev(String uri) {
        return (String) _uris.get(uri);
    }

    public Map getNamespaceMap() { // for SiRS(map) in rdf api
        HashMap namespaceMap = (HashMap) _uris.clone();
        // invent name for default namespace;
        // if we need an abbreviation for the default namespace
        // in a future version, it should be used here ... !!!
        namespaceMap.put(_defaultNamespace, inventAbbrev(_defaultNamespace));
        // ... system namespace ... !!!
        return namespaceMap;
    }

    public Map getAbbrevs() {
        return _abbrevs;
    }

    public int size() {
        return _abbrevs.size() + 1; // for default namespace
    }

    public void add(String abbrev, String uri) {
        _abbrevs.put(abbrev, uri);
        _uris.put(uri, abbrev);
    }

    String inventAbbrev(String namespace) {
        String abbrev = null;
        if (namespace.indexOf("rdf-schema") != -1)
            abbrev = "rdfs";
        else if (namespace.indexOf("rdf-syntax") != -1)
            abbrev = "rdf";
        else if (namespace.indexOf("xmlschema") != -1)
            abbrev = "xmlschema";
        else { // split at :, /, \, and # to find last component
            String lastComponent = null;
            for (StringTokenizer tokens = new StringTokenizer(namespace, "/#:\\"); tokens.hasMoreTokens();) {
                lastComponent = tokens.nextToken();
            }
            if (lastComponent == null)
                return gensymAbbrev();
            else { // normalize, simplify etc.
                // remove www. and .com/.org etc.? ... !!!
                if (lastComponent.equals("oil")) // HACK: never "invent" oil: ... !!!
                    lastComponent = "Oil";
                abbrev = normalize(lastComponent, 15);
            }
        }
        return makeUnique(abbrev);
    }

    String gensymAbbrev() { // invent new abbreviation
        String abbrev = "ns" + _gensymCounter;
        _gensymCounter++;
        if (_abbrevs.containsKey(abbrev))
            return gensymAbbrev();
        else
            return abbrev;
    }

    String makeUnique(String abbrev) {
        if (_abbrevs.containsKey(abbrev))
            return makeUnique(abbrev + "_");
        else
            return abbrev;
    }

    String normalize(String string, int max) {
        // replace not allowed characters by _; shorten to max characters
        StringBuffer buffer = new StringBuffer();
        if (Character.isDigit(string.charAt(0)))
            buffer.append("ns_"); // better: remove (usually version number) ... !!!
        int l = string.length();
        if (l > max)
            l = max; // we don't like
        for (int i = 0; i < l; i++) {
            char c = string.charAt(i);
            if (Character.isLetterOrDigit(c)) // -, : etc. ??
                buffer.append(c);
            else
                buffer.append("_");
        }
        return buffer.toString();
    }

    public String toEncodedString() { // simply put spaces between values
        StringBuffer buffer = new StringBuffer();
        // we should use entrySet here ... !!!
        for (Iterator abbrevIterator = _abbrevs.keySet().iterator(); abbrevIterator.hasNext();) {
            String abbrev = (String) abbrevIterator.next();
            String uri = (String) _abbrevs.get(abbrev);
            buffer.append(abbrev);
            buffer.append(' ');
            buffer.append(uri);
            buffer.append(' ');
        }
        return buffer.toString();
    }

    public String toEncodedString(HashMap namespaces) {
        // simply put spaces between values
        StringBuffer buffer = new StringBuffer();
        // we should use entrySet here ... !!!
        for (Iterator abbrevIterator = namespaces.keySet().iterator(); abbrevIterator.hasNext();) {
            String abbrev = (String) abbrevIterator.next();
            String uri = (String) namespaces.get(abbrev);
            buffer.append(abbrev);
            buffer.append(' ');
            buffer.append(uri);
            buffer.append(' ');
        }
        return buffer.toString();
    }

    public void decode(String encodedString) {
        for (StringTokenizer tokens = new StringTokenizer(encodedString); tokens.hasMoreTokens();) {
            String abbrev = tokens.nextToken();
            if (tokens.hasMoreTokens()) {
                String uri = tokens.nextToken();
                add(abbrev, uri);
            } else {
                System.err.println("WARNING: encoded namespace string corrupt: " + encodedString);
            }
        }
    }

}
