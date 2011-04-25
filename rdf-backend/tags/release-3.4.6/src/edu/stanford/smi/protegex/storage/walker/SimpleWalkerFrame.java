package edu.stanford.smi.protegex.storage.walker;

public class SimpleWalkerFrame implements WalkerFrame {

    String _name;
    String _namespace;
    String _localName;
    boolean _isClass;
    boolean _isSlot;
    boolean _isThing;
    boolean _isStandardClass;
    boolean _isStandardSlot;

    public SimpleWalkerFrame(String namespace, String localName, boolean isClass, boolean isSlot) {
        this(namespace, localName, isClass, isSlot, false, false, false);
    }

    public SimpleWalkerFrame(
        String namespace,
        String localName,
        boolean isClass,
        boolean isSlot,
        boolean isThing,
        boolean isStandardClass,
        boolean isStandardSlot) {
        _namespace = namespace;
        _localName = localName;
        _name = namespace + ":" + localName;
        _isClass = isClass;
        _isSlot = isSlot;
        _isThing = isThing;
        _isStandardClass = isStandardClass;
        _isStandardSlot = isStandardSlot;
    }

    public String getName() {
        return _name;
    }

    public String getDisplayName() {
        return _name;
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
        return _isClass;
    }

    public boolean isSlot() {
        return _isSlot;
    }

    public boolean isThing() {
        return _isThing;
    }

    public boolean isStandardClass() {
        return _isStandardClass;
    }

    public boolean isStandardSlot() {
        return _isStandardSlot;
    }

}
