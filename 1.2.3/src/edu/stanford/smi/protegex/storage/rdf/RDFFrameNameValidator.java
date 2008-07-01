package edu.stanford.smi.protegex.storage.rdf;

import edu.stanford.smi.protege.model.*;

/**
 * TODO Class Comment
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class RDFFrameNameValidator implements FrameNameValidator {
    private static final String VALID_START_SYMBOL_CHARS = "_:";
    private static final String VALID_SYMBOL_CHARS = ".-_:";

    public boolean isValid(String name, Frame frame) {
        boolean isValid = true;
        for (int i = 0; i < name.length() && isValid; ++i) {
            char c = name.charAt(i);
            if (i == 0) {
                isValid = isValidXMLStartChar(c);
            } else {
                isValid = isValidXMLChar(c);
            }
        }
        return isValid;
    }
    
    private boolean isValidXMLStartChar(char c) {
        return Character.isLetter(c) || VALID_START_SYMBOL_CHARS.indexOf(c) != -1;
    }
    
    private boolean isValidXMLChar(char c) {
        return Character.isLetterOrDigit(c) || VALID_SYMBOL_CHARS.indexOf(c) != -1;
    }

    public String getErrorMessage(String name, Frame frame) {
        return "Invalid RDF frame name";
    }

}
