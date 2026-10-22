package org.sagebionetworks.template.global;

import static org.sagebionetworks.template.Constants.CLOUDWATCH_LOG_RETENTION_DAYS;
import static org.sagebionetworks.template.Constants.COGNITO_USER_POOL_CF_OUTPUT_NAME;
import static org.sagebionetworks.template.Constants.COGNITO_USER_POOL_CLIENT_CF_OUTPUT_NAME;
import static org.sagebionetworks.template.Constants.DELETION_POLICY;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_ROUTE53_QUERY_LOG_GROUP_ARN;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.IDENTITY_ARN;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.LOG_RETENTION_IN_DAYS;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.CreateQueryLoggingConfigRequest;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListHostedZonesRequest;
import software.amazon.awssdk.services.route53.model.ListHostedZonesResponse;
import software.amazon.awssdk.services.route53.model.ListQueryLoggingConfigsRequest;
import software.amazon.awssdk.services.route53.model.QueryLoggingConfigAlreadyExistsException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.StringUtils;


public class GlobalResourcesBuilderImpl implements GlobalResourcesBuilder {

	private static final Logger LOGGER = LogManager.getLogger(GlobalResourcesBuilderImpl.class);

	static final String HOSTED_ZONE_ID_PREFIX = "/hostedzone/";

	private final CloudFormationClientWrapper cloudFormationClientWrapper;
    private final VelocityEngine velocityEngine;
    private final Configuration config;
    private final StackTagsProvider stackTagsProvider;
    private final SesClientWrapper sesClientWrapper;
	private final StsClient stsClient;
	private final SecretsManagerClient secretsManager;
	private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
	private final Route53Client route53Client;

    @Inject
    public GlobalResourcesBuilderImpl(CloudFormationClientWrapper cloudFormationClientWrapper,
                                      VelocityEngine velocityEngine,
                                      Configuration config,
                                      StackTagsProvider stackTagsProvider,
                                      SesClientWrapper sesClientWrapper,
                                      StsClient stsClient,
                                      SecretsManagerClient secretsManager,
                                      CognitoIdentityProviderClient cognitoIdentityProviderClient,
                                      Route53Client route53Client) {
        this.cloudFormationClientWrapper = cloudFormationClientWrapper;
        this.velocityEngine = velocityEngine;
        this.config = config;
        this.stackTagsProvider = stackTagsProvider;
        this.sesClientWrapper = sesClientWrapper;
        this.stsClient = stsClient;
		this.secretsManager = secretsManager;
		this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
		this.route53Client = route53Client;
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
        
        // setup SES notifications and Route 53 query logging on prod stack
        if ("prod".equalsIgnoreCase(config.getProperty(PROPERTY_KEY_STACK))) {
            setupSesTopics(stackName);
            enableQueryLoggingOnPublicHostedZones(
            		cloudFormationClientWrapper.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_ROUTE53_QUERY_LOG_GROUP_ARN));
        }
    }

    /**
     * The hosted zones are not managed by CloudFormation, so query logging is attached to each
     * public hosted zone in the account directly. Zones that already have a query logging config
     * are left untouched (Route 53 allows only one per zone).
     *
     * @param logGroupArn The ARN of the log group (without the trailing ':*')
     */
    void enableQueryLoggingOnPublicHostedZones(String logGroupArn) {
    	if (StringUtils.isEmpty(logGroupArn))
    		throw new IllegalStateException("Stack output '"+GLOBAL_CFSTACK_OUTPUT_KEY_ROUTE53_QUERY_LOG_GROUP_ARN+"' is required.");

    	String marker = null;
    	do {
    		ListHostedZonesResponse response = route53Client.listHostedZones(ListHostedZonesRequest.builder().marker(marker).build());
    		for (HostedZone zone : response.hostedZones()) {
    			if (zone.config() != null && Boolean.TRUE.equals(zone.config().privateZone())) {
    				continue;
    			}
    			enableQueryLogging(zone, logGroupArn);
    		}
    		marker = Boolean.TRUE.equals(response.isTruncated()) ? response.nextMarker() : null;
    	} while (marker != null);
    }

    void enableQueryLogging(HostedZone zone, String logGroupArn) {
    	String hostedZoneId = zone.id().replace(HOSTED_ZONE_ID_PREFIX, "");

    	boolean hasQueryLogging = !route53Client.listQueryLoggingConfigs(ListQueryLoggingConfigsRequest.builder()
    			.hostedZoneId(hostedZoneId)
    			.build()).queryLoggingConfigs().isEmpty();

    	if (hasQueryLogging) {
    		LOGGER.info("Query logging already enabled for hosted zone {} ({})", zone.name(), hostedZoneId);
    		return;
    	}

    	try {
    		route53Client.createQueryLoggingConfig(CreateQueryLoggingConfigRequest.builder()
    				.hostedZoneId(hostedZoneId)
    				.cloudWatchLogsLogGroupArn(logGroupArn)
    				.build());
    		LOGGER.info("Enabled query logging for hosted zone {} ({}) to {}", zone.name(), hostedZoneId, logGroupArn);
    	} catch (QueryLoggingConfigAlreadyExistsException e) {
    		LOGGER.info("Query logging already enabled for hosted zone {} ({})", zone.name(), hostedZoneId);
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

        context.put(CLOUDWATCH_LOG_RETENTION_DAYS, LOG_RETENTION_IN_DAYS);

        return context;
    }

    public void setupSesTopics(String stackName) {
        String sesComplaintSnsTopic = this.cloudFormationClientWrapper.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC);
        String sesBounceSnsTopic = this.cloudFormationClientWrapper.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC);
        
        sesClientWrapper.setComplaintNotificationTopic(SES_SYNAPSE_DOMAIN, sesComplaintSnsTopic);
        sesClientWrapper.setBounceNotificationTopic(SES_SYNAPSE_DOMAIN, sesBounceSnsTopic);
    }


}
