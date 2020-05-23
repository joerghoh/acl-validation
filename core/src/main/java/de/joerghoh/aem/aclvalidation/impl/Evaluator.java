package de.joerghoh.aem.aclvalidation.impl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true,service = Evaluator.class)
public class Evaluator {
    
    private static final Logger LOG = LoggerFactory.getLogger(Evaluator.class);
    
    private static final String SUBSERVICE_USERNAME = "validation-ace-reader";

    @Reference
    ResourceResolverFactory rrf;
    
    
    List<EvaluationResultBean> evaluate(List<ValidationRuleBean> rules) {
        List<EvaluationResultBean> result = new ArrayList<>();
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_USERNAME);
        try (ResourceResolver rr = rrf.getServiceResourceResolver(authInfo)) {
            Session readerSession = rr.adaptTo(Session.class);
            
            if (readerSession.getAccessControlManager() instanceof JackrabbitAccessControlManager) {
                JackrabbitAccessControlManager acm = (JackrabbitAccessControlManager) readerSession.getAccessControlManager();
                
                // We are already running on JR, so no need to validae it further
                JackrabbitSession jrSession = (JackrabbitSession) readerSession;
                PrincipalManager principalManager = jrSession.getPrincipalManager();
                JackrabbitWorkspace jrWorkspace = (JackrabbitWorkspace) jrSession.getWorkspace();
                PrivilegeManager privilegeManager = jrWorkspace.getPrivilegeManager();
                
            
                rules.forEach(rule -> {
                    result.add(evalRule(acm, principalManager, privilegeManager, rule));
                });
            }
            
            
        } catch (Exception e) {
            LOG.error("Cannot login",e);
        }
        return result;
    }


    private EvaluationResultBean evalRule(JackrabbitAccessControlManager acm,
            PrincipalManager principalManager, PrivilegeManager privilegeManager, ValidationRuleBean rule) {
        
        try {
            Set<Principal> principals = convertPrincipals(principalManager, rule.usersAsSet());
            List<Privilege> privilegesToCheck = convertPrivileges(privilegeManager,rule.getPrivileges());
            Privilege[] privs = new Privilege[privilegesToCheck.size()];
            privilegesToCheck.toArray(privs);
            
            boolean expectedResult = convertPolicyToBoolean(rule.policy);
            boolean actualResult = acm.hasPrivileges(rule.itempath, principals, privs);
            LOG.debug("Performing policy validation (policy = {}, actual result = {}, rule={}",new Object[] {expectedResult,actualResult, rule});
            
            if (actualResult == expectedResult) {
                return new EvaluationResultBean(true,rule,"evalution succesfull (got expected result)");
            } else {
                return new EvaluationResultBean(false,rule,"evaluation failed, got " + actualResult);
            }
            
        } catch (EvaluationException e) {
            return new EvaluationResultBean(false,rule,String.format("failed validation with exception: %s ",e.getMessage()));
        } catch (AccessDeniedException e) {
            return new EvaluationResultBean(false, rule,"Session does not have sufficient permissions to validate the permissions");
        } catch (PathNotFoundException e) {
            return new EvaluationResultBean(false, rule,"path does not exist: " + e.getMessage());
        } catch (RepositoryException e) {
            LOG.error("RepositoryException while evaluating rule {}",rule,e);
            return new EvaluationResultBean(false, rule,"RepositoryException " + e.getMessage());
        }
    }
    
    
    boolean convertPolicyToBoolean(String policyString) {
        String s = policyString.trim().toUpperCase();
        return "ALLOW".equals(s);
    }
    
    List<Privilege> convertPrivileges (PrivilegeManager privilegeManager, List<String> privileges) throws EvaluationException {
        List<Privilege> privilegesToCheck = new ArrayList<>();
        Iterator<String> i = privileges.iterator();
        while (i.hasNext()) {
            String privilegeString = i.next();
            try {
                Privilege p = privilegeManager.getPrivilege(privilegeString);
                privilegesToCheck.add(p);
            } catch (AccessControlException e) {
                String msg = String.format("Privilege %s does not exist", privilegeString);
                throw new EvaluationException(msg,e);
            } catch (RepositoryException e) {
                throw new EvaluationException("RepositoryException while converting privileges",e);
            }
        }
        return privilegesToCheck;
    }
    
    Set<Principal> convertPrincipals (PrincipalManager principalManager, Set<String> principals) throws EvaluationException {
        Set<Principal> result = new HashSet<>();
        Iterator<String> i = principals.iterator();
        while (i.hasNext()) {
            String p = i.next();
            if (principalManager.hasPrincipal(p)) {
                result.add(principalManager.getPrincipal(p));
            } else {
                String msg = String.format("Principal '%s' does not exist", p);
                throw new EvaluationException (msg);
            }
        }
        return result;
        
    }
    
    
    public class EvaluationResult {
        
        public boolean canPerformOperation;
        public ValidationRuleBean rule;
        
        
        public EvaluationResult (boolean result, ValidationRuleBean rule) {
            this.canPerformOperation = result;
            this.rule = rule;
        }
        
    }
    
    
}