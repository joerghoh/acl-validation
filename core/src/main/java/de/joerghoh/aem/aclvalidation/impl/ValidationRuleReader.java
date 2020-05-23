package de.joerghoh.aem.aclvalidation.impl;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.flat.TreeTraverser;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component(service = ValidationRuleReader.class)
public class ValidationRuleReader {
    
    private static final Logger LOG = LoggerFactory.getLogger(ValidationRuleReader.class);
    
    private static final String SUBSERVICE = "validation-rule-reader";
    
    @Reference
    ResourceResolverFactory rrf;
    
    @Reference
    Evaluator evaluator;
    
    // Wait until this service mapping is available, the mapping itself is not used
    @Reference(target="(subServiceName="+SUBSERVICE+")", service=ServiceUserMapped.class)
    ServiceUserMapped mapped; 
        
    
    
    public List<ValidationRuleBean> readAllRulesBelowPath(String path) {
        List<ValidationRuleBean> result = new ArrayList<>();
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, SUBSERVICE);
        
        try (ResourceResolver rr = rrf.getServiceResourceResolver(authInfo)) {
            Session embeddedSession = rr.adaptTo(Session.class);
            Node rootNode = embeddedSession.getNode(path);
            TreeTraverser.nodeIterator(rootNode).forEachRemaining(node -> {
                String nodePath = "(undefined)";
                try {
                    nodePath = node.getPath();
                    if (isRuleNode(node)) {
                        result.addAll(readRulesFromNtFile(node));
                    } else {
                        LOG.debug("Ignoring node {}",nodePath);
                    }
                } catch (RepositoryException e) {
                    LOG.error("RepositoryException while traversing tree",e);
                }
                
            });
            
            
        } catch (LoginException e) {
            LOG.error("Exception while reading rules below {}", path, e);
        } catch (PathNotFoundException e) {
            LOG.error("Path {} does not exist in the repository: {}", path, e.getMessage());
        } catch (RepositoryException e) {
            LOG.error("RepositoryException while reading rules below {}",path,e);
        }
        return result; 
    }
    
    private List<ValidationRuleBean> readRulesFromNtFile(Node node) throws RepositoryException   {
        String path = node.getPath();
        ObjectMapper mapper = new ObjectMapper();
        List<ValidationRuleBean> result = new ArrayList<>();
        try {
            Property jcrData = node.getProperty("jcr:content/jcr:data");
            try (InputStream s = jcrData.getBinary().getStream()) {
                result = mapper.readValue(s,new TypeReference<List<ValidationRuleBean>>(){});
            }
        } catch (IOException e) {
            LOG.error("Cannot read JSON from node {}",path,e);
        }
        result.forEach(r -> {
            r.repoPath = path;
        });
        return result;
    }
    
    
    boolean isRuleNode(Node node) throws RepositoryException {
        return (node.isNodeType("nt:file") && node.getName().endsWith(".json"));
    }
}
