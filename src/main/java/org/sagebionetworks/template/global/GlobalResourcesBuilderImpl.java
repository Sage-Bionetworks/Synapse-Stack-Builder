package org.sagebionetworks.template.global;

import static org.sagebionetworks.template.Constants.COGNITO_USER_POOL_CF_OUTPUT_NAME;
import static org.sagebionetworks.template.Constants.COGNITO_USER_POOL_CLIENT_CF_OUTPUT_NAME;
import static org.sagebionetworks.template.Constants.DELETION_POLICY;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.IDENTITY_ARN;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.OPS_VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.SAGEBIO_COGNITO_APP_CLIENT_ID;
import static org.sagebionetworks.template.Constants.SAGEBIO_COGNITO_APP_CLIENT_SECRET;
import static org.sagebionetworks.template.Constants.SAGEBIO_COGNITO_APP_DISCOVERY_DOCUMENT;
import static org.sagebionetworks.template.Constants.SES_SYNAPSE_DOMAIN;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_GLOBAL_RESOURCES;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientResponse;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.StringUtils;


public class GlobalResourcesBuilderImpl implements GlobalResourcesBuilder {

	private final CloudFormationClientWrapper cloudFormationClientWrapper;
    private final VelocityEngine velocityEngine;
    private final Configuration config;
    private final StackTagsProvider stackTagsProvider;
    private final SesClientWrapper sesClientWrapper;
	private final StsClient stsClient;
	private final SecretsManagerClient secretsManager;
	private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

    @Inject
    public GlobalResourcesBuilderImpl(CloudFormationClientWrapper cloudFormationClientWrapper,
                                      VelocityEngine velocityEngine,
                                      Configuration config,
                                      StackTagsProvider stackTagsProvider,
                                      SesClientWrapper sesClientWrapper,
                                      StsClient stsClient,
                                      SecretsManagerClient secretsManager,
                                      CognitoIdentityProviderClient cognitoIdentityProviderClient) {
        this.cloudFormationClientWrapper = cloudFormationClientWrapper;
        this.velocityEngine = velocityEngine;
        this.config = config;
        this.stackTagsProvider = stackTagsProvider;
        this.sesClientWrapper = sesClientWrapper;
        this.stsClient = stsClient;
		this.secretsManager = secretsManager;
		this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
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
        Optional<Stack> stack = cloudFormationClientWrapper.waitForStackToComplete(stackName);
        
        // CloudFormation can't get the Cognito application credentials and put them into
        // Secrets Manager, so we do that as a post-processing step using the AWS client directly
        copyCognitoSecretsToSecretsManager(config.getProperty(PROPERTY_KEY_STACK), stack.get());
        
        // setup SES notifications on prod stack
        if ("prod".equalsIgnoreCase(config.getProperty(PROPERTY_KEY_STACK))) {
            setupSesTopics(stackName);
        }
    }
    
    void copyCognitoSecretsToSecretsManager(String stackPrefix, Stack cfStack) {
    	List<Output> outputs = cfStack.outputs();

    	// Convert outputs to a Map<String, String> for convenience:
    	Map<String, String> outputMap = outputs.stream()
    	    .collect(Collectors.toMap(
    	        software.amazon.awssdk.services.cloudformation.model.Output::outputKey,
    	        software.amazon.awssdk.services.cloudformation.model.Output::outputValue
    	));

    	String userPoolId = outputMap.get(COGNITO_USER_POOL_CF_OUTPUT_NAME);
    	String appClientId = outputMap.get(COGNITO_USER_POOL_CLIENT_CF_OUTPUT_NAME);
    	
    	if (StringUtils.isEmpty(userPoolId)) 
    		throw new IllegalStateException("Stack output '"+COGNITO_USER_POOL_CF_OUTPUT_NAME+"' is required.");
    	if (StringUtils.isEmpty(appClientId)) 
    		throw new IllegalStateException("Stack output '"+COGNITO_USER_POOL_CLIENT_CF_OUTPUT_NAME+"' is required.");
    	
    	DescribeUserPoolClientRequest userPoolClientRequest = 
    			DescribeUserPoolClientRequest.builder().userPoolId(userPoolId).clientId(appClientId).build();
    	
    	// Describe the user pool client to get the client id and secret
    	DescribeUserPoolClientResponse userPoolClientResponse = 
    			cognitoIdentityProviderClient.describeUserPoolClient(userPoolClientRequest);

    	String cognitoAppClientId = userPoolClientResponse.userPoolClient().clientId();
    	String cognitoAppClientSecret = userPoolClientResponse.userPoolClient().clientSecret();

    	if (StringUtils.isEmpty(cognitoAppClientId)) throw new IllegalStateException("Cognito app is missing client id.");
    	if (StringUtils.isEmpty(cognitoAppClientSecret)) throw new IllegalStateException("Cognito app is missing client secret.");

    	String idName = stackPrefix + "."+ SAGEBIO_COGNITO_APP_CLIENT_ID;
    	setSecret(idName, cognitoAppClientId);
    	String secretName = stackPrefix + "."+ SAGEBIO_COGNITO_APP_CLIENT_SECRET;
    	setSecret(secretName, cognitoAppClientSecret);
    	
    	String discoveryDocumentName = stackPrefix + "."+ SAGEBIO_COGNITO_APP_DISCOVERY_DOCUMENT;
    	String discoveryDocumentUrl = "https://cognito-idp.us-east-1.amazonaws.com/"+userPoolId+"/.well-known/openid-configuration";
    	setSecret(discoveryDocumentName, discoveryDocumentUrl);
        
    }
    
    void setSecret(String key, String value) {
    	try {
    		secretsManager.createSecret(CreateSecretRequest.builder()
    				.name(key)
    				.secretString(value)
    				.build());
    	} catch (software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException e) {
    		secretsManager.putSecretValue(PutSecretValueRequest.builder()
    				.secretId(key)
    				.secretString(value)
    				.build());
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
        
        context.put(COGNITO_USER_POOL_CF_OUTPUT_NAME, COGNITO_USER_POOL_CF_OUTPUT_NAME);
        context.put(COGNITO_USER_POOL_CLIENT_CF_OUTPUT_NAME, COGNITO_USER_POOL_CLIENT_CF_OUTPUT_NAME);
        
        return context;
    }

    public void setupSesTopics(String stackName) {
        String sesComplaintSnsTopic = this.cloudFormationClientWrapper.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC);
        String sesBounceSnsTopic = this.cloudFormationClientWrapper.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC);
        
        sesClientWrapper.setComplaintNotificationTopic(SES_SYNAPSE_DOMAIN, sesComplaintSnsTopic);
        sesClientWrapper.setBounceNotificationTopic(SES_SYNAPSE_DOMAIN, sesBounceSnsTopic);
    }


}
