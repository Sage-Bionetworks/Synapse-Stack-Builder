package org.sagebionetworks.template.global;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.SesClient;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.Configuration;

import java.io.StringWriter;

import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.SES_SYNAPSE_DOMAIN;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_GLOBAL_RESOURCES;
import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC;

public class GlobalResourcesBuilderImpl implements GlobalResourcesBuilder {

    CloudFormationClient cloudFormationClient;
    VelocityEngine velocityEngine;
    Configuration config;
    Logger logger;
    StackTagsProvider stackTagsProvider;
    SesClient sesClient;

    @Inject
    public GlobalResourcesBuilderImpl(CloudFormationClient cloudFormationClient,
                                      VelocityEngine velocityEngine,
                                      Configuration config,
                                      LoggerFactory loggerFactory,
                                      StackTagsProvider stackTagsProvider,
                                      SesClient sesClient) {
        this.cloudFormationClient = cloudFormationClient;
        this.velocityEngine = velocityEngine;
        this.config = config;
        this.logger = loggerFactory.getLogger(GlobalResourcesBuilderImpl.class);
        this.stackTagsProvider = stackTagsProvider;
        this.sesClient = sesClient;
    }

    @Override
    public void buildGlobalResources() throws InterruptedException {
        String stackName = createStackName();
        VelocityContext context = createContext();
        Template template = velocityEngine.getTemplate(TEMPLATE_GLOBAL_RESOURCES);
        StringWriter stringWriter = new StringWriter();
        template.merge(context, stringWriter);
        String resultJSON = stringWriter.toString();
        JSONObject templateJson = new JSONObject(resultJSON);
        resultJSON = templateJson.toString(JSON_INDENT);
        //this.logger.info(resultJSON);
        cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest()
            .withStackName(stackName)
            .withTemplateBody(resultJSON)
            .withCapabilities(CAPABILITY_NAMED_IAM)
            .withTags(stackTagsProvider.getStackTags())
        );
        cloudFormationClient.waitForStackToComplete(stackName);
        // setup SES notifications on prod stack
        if ("prod".equalsIgnoreCase(config.getProperty(PROPERTY_KEY_STACK))) {
            setupSesTopics(stackName);
        }
    }

    public String createStackName() {
        return String.format(GLOBAL_RESOURCES_STACK_NAME_FORMAT, config.getProperty(PROPERTY_KEY_STACK));
    }

    public VelocityContext createContext() {
        VelocityContext context = new VelocityContext();
        context.put(STACK, config.getProperty(PROPERTY_KEY_STACK));
        return context;
    }

    public void setupSesTopics(String stackName) {
        String sesComplaintSnsTopic = this.cloudFormationClient.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC);
        String sesBounceSnsTopic = this.cloudFormationClient.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC);
        sesClient.setComplaintNotificationTopic(SES_SYNAPSE_DOMAIN, sesComplaintSnsTopic);
        sesClient.setBounceNotificationTopic(SES_SYNAPSE_DOMAIN, sesBounceSnsTopic);
    }


}
