package edu.stanford.smi.protegex.storage.rdf.configurable;

/**
 * TODO Class Comment
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public interface RDFFilesPlugin {
    void setFiles(String clsesFileName, String instancesFileName);
    void setNamespace(String namespace);
}
