package org.sagebionetworks.template.etl;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.Configuration;

import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.ETL_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_ETL_GLUE_JOB_RESOURCES;

public class EtlBuilderImpl implements EtlBuilder {
    private CloudFormationClient cloudFormationClient;
    private VelocityEngine velocityEngine;
    private Configuration config;
    private Logger logger;
    private StackTagsProvider tagsProvider;
    private EtlConfig etlConfig;

    @Inject
    public EtlBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
                          Configuration config, LoggerFactory loggerFactory,
                          StackTagsProvider tagsProvider, EtlConfig etlConfig) {
        this.cloudFormationClient = cloudFormationClient;
        this.velocityEngine = velocityEngine;
        this.config = config;
        this.logger = loggerFactory.getLogger(EtlBuilderImpl.class);
        this.tagsProvider = tagsProvider;
        this.etlConfig = etlConfig;
    }

    @Override
    public void buildAndDeploy() {
        VelocityContext context = new VelocityContext();
        String stack = config.getProperty(PROPERTY_KEY_STACK);
        List<EtlDescriptor> etlDescriptors = etlConfig.getEtlDescriptors();
        etlDescriptors.forEach(etlDescriptor -> {
            etlDescriptor.setSourcePath(TemplateUtils.replaceStackVariable(etlDescriptor.getSourcePath(), stack));
            etlDescriptor.setDestinationPath(TemplateUtils.replaceStackVariable(etlDescriptor.getDestinationPath(), stack));
            etlDescriptor.setScriptLocation(TemplateUtils.replaceStackVariable(etlDescriptor.getScriptLocation(), stack));
            Set<String> buckets = etlDescriptor.getBuckets().stream()
                    .map(b -> TemplateUtils.replaceStackVariable(b, stack)).collect(Collectors.toSet());
            etlDescriptor.setBuckets(buckets);
        });
        context.put(STACK, stack);
        context.put(ETL_DESCRIPTORS, etlDescriptors);
        String stackName = new StringJoiner("-")
                .add(stack).add(config.getProperty(PROPERTY_KEY_INSTANCE)).add("etl").toString();

        Parameter parameter = new Parameter();

        // Merge the context with the template
        Template template = this.velocityEngine.getTemplate(TEMPLATE_ETL_GLUE_JOB_RESOURCES);
        StringWriter stringWriter = new StringWriter();
        template.merge(context, stringWriter);
        // Parse the resulting template
        String resultJSON = stringWriter.toString();
        JSONObject templateJson = new JSONObject(resultJSON);

        // Format the JSON
        resultJSON = templateJson.toString(JSON_INDENT);
        this.logger.info(resultJSON);
        // create or update the template
        this.cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest().withStackName(stackName)
                .withTemplateBody(resultJSON).withParameters(parameter).withTags(tagsProvider.getStackTags())
                .withCapabilities(CAPABILITY_NAMED_IAM));
    }
}
