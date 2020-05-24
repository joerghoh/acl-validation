package de.joerghoh.aem.aclvalidation.impl;

import com.adobe.granite.jmx.annotation.Description;

@Description("Acl Validation Tool")
public interface ValidatorMBean {

    @Description("start the validation process")
    public String startValidation();
    
}
