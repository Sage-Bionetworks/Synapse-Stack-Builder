package org.sagebionetworks.template.global;

import static org.sagebionetworks.template.Constants.DELETION_POLICY;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.IDENTITY_ARN;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.OPS_VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.SES_SYNAPSE_DOMAIN;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_GLOBAL_RESOURCES;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.SesClientWrapper;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.DeletionPolicy;

import com.google.inject.Inject;

import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.sts.StsClient;

public class GlobalResourcesBuilderImpl implements GlobalResourcesBuilder {

	private final CloudFormationClientWrapper cloudFormationClientWrapper;
    private final VelocityEngine velocityEngine;
    private final Configuration config;
    private final StackTagsProvider stackTagsProvider;
    private final SesClientWrapper sesClientWrapper;
	private final StsClient stsClient;

    @Inject
    public GlobalResourcesBuilderImpl(CloudFormationClientWrapper cloudFormationClientWrapper,
                                      VelocityEngine velocityEngine,
                                      Configuration config,
                                      StackTagsProvider stackTagsProvider,
                                      SesClientWrapper sesClientWrapper,
                                      StsClient stsClient) {
        this.cloudFormationClientWrapper = cloudFormationClientWrapper;
        this.velocityEngine = velocityEngine;
        this.config = config;
        this.stackTagsProvider = stackTagsProvider;
        this.sesClientWrapper = sesClientWrapper;
        this.stsClient = stsClient;
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
        cloudFormationClientWrapper.createOrUpdateStack(new CreateOrUpdateStackRequest()
            .withStackName(stackName)
            .withTemplateBody(resultJSON)
            .withCapabilities(Capability.CAPABILITY_NAMED_IAM)
            .withTags(stackTagsProvider.getStackTags(config))
        );
        cloudFormationClientWrapper.waitForStackToComplete(stackName);
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
        
        String stack = config.getProperty(PROPERTY_KEY_STACK);
        
        context.put(STACK, stack);
        context.put(DELETION_POLICY, Constants.isProd(config.getProperty(PROPERTY_KEY_STACK)) ? DeletionPolicy.Retain.name() : DeletionPolicy.Delete.name());
        
        context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
        context.put(OPS_VPC_EXPORT_PREFIX, config.getProperty(Constants.PROPERTY_KEY_OPS_VPC_EXPORT_PREFIX));
        context.put(IDENTITY_ARN, stsClient.getCallerIdentity().arn());
        
        return context;
    }

    public void setupSesTopics(String stackName) {
        String sesComplaintSnsTopic = this.cloudFormationClientWrapper.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC);
        String sesBounceSnsTopic = this.cloudFormationClientWrapper.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC);
        
        sesClientWrapper.setComplaintNotificationTopic(SES_SYNAPSE_DOMAIN, sesComplaintSnsTopic);
        sesClientWrapper.setBounceNotificationTopic(SES_SYNAPSE_DOMAIN, sesBounceSnsTopic);
    }


}
