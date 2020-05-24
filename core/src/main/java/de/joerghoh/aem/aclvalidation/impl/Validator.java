package de.joerghoh.aem.aclvalidation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(immediate=true, 
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service={HealthCheck.class,Validator.class},
        property = {
                HealthCheck.NAME+"= Acl Validator",
                HealthCheck.MBEAN_NAME+"= AclValidation",
                HealthCheck.TAGS+"=acl,security",
                "jmx.objectname=de.joerghoh.aem:type=AclValidator"
        }
)
@Designate(ocd = Validator.Config.class)
public class Validator implements HealthCheck, ValidatorMBean {
    
    
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
    
    List<EvaluationResultBean> lastResults = null;
    
    
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
            writeResultsToLog(validateRules());
        }
    }
    
    
    private List<EvaluationResultBean> validateRules() {
        
        List<ValidationRuleBean> rules = new ArrayList<>();
        for (int i = 0; i< configuration.configurationPaths().length;i++) {
            String path = configuration.configurationPaths()[i];
            rules.addAll(reader.readAllRulesBelowPath(path));
        }
        List<EvaluationResultBean> result = evaluator.evaluate(rules);
        lastResults = result;
        return result;
    }

    
    private void writeResultsToLog(List<EvaluationResultBean> result) {
        result.forEach(evaluation -> {
            LOG.info(evaluation.toString());
        });
    }
    
    
    
    //------- 
    

    @Override
    public Result execute() {
        if (lastResults == null) {
            validateRules();
        }
        FormattingResultLog log = new FormattingResultLog();
        lastResults.forEach(evaluation -> {
            if (evaluation.hasExpectedResult) {
                log.info(" message= {} (rule={})",  evaluation.message, evaluation.rule);
            } else {
                log.critical(" message = {} (rule={})", evaluation.message, evaluation.rule);
            }
        });
        return new Result (log);
    }


    @Override
    public String startValidation() {
        List<EvaluationResultBean> result = validateRules();
        return result.stream().map(EvaluationResultBean::toString).collect(Collectors.joining("\n"));
    }
    
    

}
