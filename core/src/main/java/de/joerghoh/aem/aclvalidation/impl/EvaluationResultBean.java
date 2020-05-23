package de.joerghoh.aem.aclvalidation.impl;

public class EvaluationResultBean {

    
    protected boolean hasExpectedResult;
    protected ValidationRuleBean rule;
    protected String message = "";
    
    
    public EvaluationResultBean (boolean expectedResult,ValidationRuleBean r) {
        this.hasExpectedResult = expectedResult;
        this.rule = r;
    }
    
    public EvaluationResultBean (boolean expectedResult,ValidationRuleBean r, String message) {
        this.hasExpectedResult = expectedResult;
        this.rule = r;
        this.message = message;
    }
    
    public String toString() {
        return String.format("EvaluationResultBean[hasExpectedResult='%s',message='%s',rule='%s']", hasExpectedResult, message, rule);
    }
    
    
}
