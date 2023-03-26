package org.sagebionetworks.template.datawarehouse;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
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
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DATAWAREHOUSE_GLUE_DATABASE_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_ETL_GLUE_JOB_RESOURCES;

public class DataWarehouseBuilderImpl implements DataWarehouseBuilder {
    private CloudFormationClient cloudFormationClient;
    private VelocityEngine velocityEngine;
    private Configuration config;
    private Logger logger;
    private StackTagsProvider tagsProvider;
    private EtlJobConfig etlJobConfig;

    @Inject
    public DataWarehouseBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
                                    Configuration config, LoggerFactory loggerFactory,
                                    StackTagsProvider tagsProvider, EtlJobConfig etlJobConfig) {
        this.cloudFormationClient = cloudFormationClient;
        this.velocityEngine = velocityEngine;
        this.config = config;
        this.logger = loggerFactory.getLogger(DataWarehouseBuilderImpl.class);
        this.tagsProvider = tagsProvider;
        this.etlJobConfig = etlJobConfig;
    }

    @Override
    public void buildAndDeploy(String version) {
        VelocityContext context = new VelocityContext();
        String stack = config.getProperty(PROPERTY_KEY_STACK);
        String databaseName = getAndValidateDatabaseName();
        List<EtlJobDescriptor> etlJobDescriptors = etlJobConfig.getEtlJobDescriptors();
        etlJobDescriptors.forEach(etlDescriptor -> {
            String scriptName = etlDescriptor.getScriptName();
            String scriptNameWithVersion = scriptName.substring(0, scriptName.indexOf(".")) + "_" + version +
                    scriptName.substring(scriptName.indexOf("."));
            String scriptLocation = etlDescriptor.getScriptLocation() + scriptNameWithVersion;
            etlDescriptor.setScriptLocation(scriptLocation);
        });
        context.put(GLUE_DATABASE_NAME, databaseName.toLowerCase());
        context.put(EXCEPTION_THROWER, new VelocityExceptionThrower());
        context.put(STACK, stack);
        context.put(ETL_DESCRIPTORS, etlJobDescriptors);
        String stackName = new StringJoiner("-")
                .add(stack).add(databaseName).add("etl-jobs").toString();

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

    private String getAndValidateDatabaseName(){
        String databaseName = config.getProperty(PROPERTY_KEY_DATAWAREHOUSE_GLUE_DATABASE_NAME);
        if (StringUtils.isEmpty(databaseName)) {
            throw new IllegalArgumentException("Database name is required.");
        }
        return databaseName.toLowerCase();
    }
}
