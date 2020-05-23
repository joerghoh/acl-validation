package de.joerghoh.aem.aclvalidation.impl;

public class EvaluationException extends Exception {

    
    EvaluationException (String message) {
        super (message);
    }
    
    EvaluationException (String message, Exception e) {
        super (message, e);
    }
    
}
