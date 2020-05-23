package de.joerghoh.aem.aclvalidation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Designate(ocd = Validator.Config.class)
public class Validator {
    
    
    @ObjectClassDefinition(name="Acl Validation Configuration")
    public @interface Config {
        
        @AttributeDefinition(name="Configuration paths", description="Repository paths where the validation config is stored" )
        String[] configurationPaths();
        
        @AttributeDefinition(name="Check on startup", description="Run the first check already on startup")
        boolean checkOnActivate() default true;
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(Validator.class);
    
    @Reference
    ValidationRuleReader reader;
    
    @Reference
    Evaluator evaluator;
    
    Config configuration;
    
    
    @Activate
    @Modified
    public void activate(Config c   ) {
        if (c.configurationPaths().length == 0) {
            LOG.info("No path configured for validation rules");
            return;
        }
        configuration = c;
        LOG.info("Using acl validation rules from '{}‘", Arrays.toString(c.configurationPaths()));
        if (c.checkOnActivate()) {
            validateRules();
        }
    }
    
    
    private void validateRules() {
        
        List<ValidationRuleBean> rules = new ArrayList<>();
        
        for (int i = 0; i< configuration.configurationPaths().length;i++) {
            String path = configuration.configurationPaths()[i];
            rules.addAll(reader.readAllRulesBelowPath(path));
        }
        List<EvaluationResultBean> result = evaluator.evaluate(rules);
        result.forEach(evaluation -> {
            LOG.info(evaluation.toString());
        });
        
    }

}
