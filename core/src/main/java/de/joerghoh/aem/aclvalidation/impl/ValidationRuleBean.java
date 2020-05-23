package de.joerghoh.aem.aclvalidation.impl;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ValidationRuleBean {

    @JsonProperty("principals")
    public String principals;
    
    @JsonProperty("itempath")
    public String itempath;
    
    @JsonProperty("privileges")
    public String privileges;
    
    @JsonProperty("policy")
    public String policy;
    
    @JsonIgnore
    public String repoPath;
    
    
    public String toString() {
        return String.format("ValidationRuleBean[principal='%s', itempath='%s', privileges='%s', policy='%s', rule stored at '%s']", 
                new Object[] {principals,itempath,privileges,policy, repoPath});
    }
    
    public Set<String> usersAsSet() {
        return new HashSet<>(Arrays.asList(principals.split(",")));
    }
   
    public List<String> getPrivileges() {
        return Arrays.asList(privileges.split(","));
    }
}
