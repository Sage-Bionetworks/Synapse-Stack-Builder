package org.sagebionetworks.template.etl;

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
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.VelocityExceptionThrower;

import java.io.StringWriter;
import java.util.List;
import java.util.StringJoiner;

import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.ETL_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.EXCEPTION_THROWER;
import static org.sagebionetworks.template.Constants.GLUE_DATABASE_NAME;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_GLUE_DATA_BASE_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.S3_KEY;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_ETL_GLUE_JOB_RESOURCES;

public class EtlBuilderImpl implements EtlBuilder {

    public static final String PROCESSED_ACCESS_RECORD_FOLDER_NAME = "processedAccessRecord";
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
    public void buildAndDeploy(String version) {
        VelocityContext context = new VelocityContext();
        String stack = config.getProperty(PROPERTY_KEY_STACK);
        String databaseName = config.getProperty(PROPERTY_KEY_GLUE_DATA_BASE_NAME);
        List<EtlDescriptor> etlDescriptors = etlConfig.getEtlDescriptors();
        etlDescriptors.forEach(etlDescriptor -> {
            String scriptName = etlDescriptor.getScriptName();
            String scriptNameWithVersion = scriptName.substring(0, scriptName.indexOf(".")) + "_" + version +
                    scriptName.substring(scriptName.indexOf("."));
            String scriptLocation = etlDescriptor.getScriptLocation() + scriptNameWithVersion;
            etlDescriptor.setScriptLocation(scriptLocation);
        });
        context.put(GLUE_DATABASE_NAME, databaseName);
        context.put(EXCEPTION_THROWER, new VelocityExceptionThrower());
        context.put(STACK, stack);
        String destinationS3Key = new StringJoiner("/").add(databaseName).add(PROCESSED_ACCESS_RECORD_FOLDER_NAME).toString();
        context.put(S3_KEY, destinationS3Key);
        context.put(ETL_DESCRIPTORS, etlDescriptors);
        String stackName = new StringJoiner("-")
                .add(stack).add(databaseName).add("etl-job").toString();

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
                .withTemplateBody(resultJSON).withTags(tagsProvider.getStackTags())
                .withCapabilities(CAPABILITY_NAMED_IAM));
    }
}
