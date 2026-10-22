package org.sagebionetworks.template.global;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_ROUTE53_QUERY_LOG_GROUP_ARN;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC;
import static org.sagebionetworks.template.Constants.IDENTITY_ARN;
import static org.sagebionetworks.template.Constants.OPS_VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OPS_VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.SES_SYNAPSE_DOMAIN;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.CREATE_COMPLETE;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.SesClientWrapperImpl;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.Configuration;

import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.CreateQueryLoggingConfigRequest;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.HostedZoneConfig;
import software.amazon.awssdk.services.route53.model.ListHostedZonesRequest;
import software.amazon.awssdk.services.route53.model.ListHostedZonesResponse;
import software.amazon.awssdk.services.route53.model.ListQueryLoggingConfigsRequest;
import software.amazon.awssdk.services.route53.model.ListQueryLoggingConfigsResponse;
import software.amazon.awssdk.services.route53.model.QueryLoggingConfig;
import software.amazon.awssdk.services.route53.model.QueryLoggingConfigAlreadyExistsException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
public class GlobalResourcesBuilderImplTest {

    @Mock
    Configuration mockConfig;
    @Mock
    CloudFormationClientWrapper mockCloudFormationClientWrapper;
    VelocityEngine velocityEngine;
    @Mock
    StackTagsProvider mockStackTagsProvider;
    @Mock
    SesClientWrapperImpl mockSesClient;
	@Mock
	StsClient mockStsClient;
	
	@Mock
	SecretsManagerClient mockSecretsManager;
	
	@Mock
	CognitoIdentityProviderClient mockCognitoIdentityProviderClient;

	@Mock
	Route53Client mockRoute53Client;

	@Captor
	ArgumentCaptor<CreateQueryLoggingConfigRequest> createQueryLoggingConfigRequestCaptor;

	static final String LOG_GROUP_ARN = "arn:aws:logs:us-east-1:123456789012:log-group:/aws/route53/prod-public-hosted-zones";

	Stack stack;
	
    List<Tag> expectedTags;

    @Captor
    ArgumentCaptor<CreateOrUpdateStackRequest> stackRequestCaptor;

    @Captor
    ArgumentCaptor<CreateSecretRequest> createSecretRequestCaptor;

    GlobalResourcesBuilderImpl builder;

    @BeforeEach
    public void before() {
        velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
        
        expectedTags = new LinkedList<>();
        Tag t = Tag.builder().key("aKey").value("aValue").build();
        expectedTags.add(t);

        builder = new GlobalResourcesBuilderImpl(mockCloudFormationClientWrapper, velocityEngine, mockConfig, mockStackTagsProvider, mockSesClient, mockStsClient, mockSecretsManager, mockCognitoIdentityProviderClient, mockRoute53Client);

        List<Output> outputs = List.of(
        		Output.builder().outputKey("CognitoUserPoolClientId").outputValue("client-101").build(),
        		Output.builder().outputKey("CognitoUserPoolId").outputValue("user-pool-102").build());
        stack = Stack.builder().
        		outputs(outputs).stackStatus(CREATE_COMPLETE).
        		build();
    }

    @Test
    public void testCreateStackName() {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");
        assertEquals("synapse-prod-global-resources", builder.createStackName());
    }

    @Test
    public void testCreateContext() {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
        when(mockConfig.getProperty(PROPERTY_KEY_OPS_VPC_EXPORT_PREFIX)).thenReturn("us-east-1-vpc");
        when(mockStsClient.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder().arn("currentIdentityArn").build());
        VelocityContext context = builder.createContext();
        assertEquals("dev", context.get(STACK));
        assertEquals("us-east-1-synapse-dev-vpc-2", context.get(VPC_EXPORT_PREFIX));
        assertEquals("us-east-1-vpc", context.get(OPS_VPC_EXPORT_PREFIX));
        assertEquals("currentIdentityArn", context.get(IDENTITY_ARN));
    }

    @Test
    public void testSetupSesTopics() {
        String stackName = "prodStackName";
        when(mockCloudFormationClientWrapper.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC)).thenReturn("theComplaintTopic");
        when(mockCloudFormationClientWrapper.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC)).thenReturn("theBounceTopic");

        // call under test
        builder.setupSesTopics(stackName);

        verify(mockSesClient).setComplaintNotificationTopic(SES_SYNAPSE_DOMAIN, "theComplaintTopic");
        verify(mockSesClient).setBounceNotificationTopic(SES_SYNAPSE_DOMAIN, "theBounceTopic");
    }

    @Test
    public void testBuildGlobalResourcesDev() throws InterruptedException {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
        when(mockConfig.getProperty(PROPERTY_KEY_OPS_VPC_EXPORT_PREFIX)).thenReturn("us-east-1-vpc");
		when(mockStsClient.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder().arn("currentIdentityArn").build());
        when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);
        when(mockCloudFormationClientWrapper.waitForStackToComplete("synapse-dev-global-resources")).thenReturn(Optional.of(stack));
        DescribeUserPoolClientResponse userPoolClientResponse = 
        		DescribeUserPoolClientResponse.builder().userPoolClient(
        				UserPoolClientType.builder().clientId("client-101").clientSecret("secret-999").build()
        		).build();
        when(mockCognitoIdentityProviderClient.describeUserPoolClient(any(DescribeUserPoolClientRequest.class))).thenReturn(userPoolClientResponse);

        builder.buildGlobalResources(); // call under test

        verify(mockCloudFormationClientWrapper).createOrUpdateStack(stackRequestCaptor.capture());
        CreateOrUpdateStackRequest req = stackRequestCaptor.getValue();
        assertEquals("synapse-dev-global-resources", req.getStackName());
        assertEquals(expectedTags, req.getTags());
        assertNull(req.getParameters());
        assertNotNull(req.getTemplateBody());

        JSONObject templateJSON = new JSONObject(req.getTemplateBody());
//        System.out.println(templateJSON.toString(2));

        assertTrue(validateResources("dev", templateJSON));

        verify(mockSesClient, never()).setComplaintNotificationTopic(anyString(), anyString());
        verify(mockSesClient, never()).setBounceNotificationTopic(anyString(), anyString());
        verifyZeroInteractions(mockRoute53Client);

        verify(mockSecretsManager, times(3)).createSecret(createSecretRequestCaptor.capture());
        // check that the correct secret keys and values are passed
        List<CreateSecretRequest> createSecretRequests = createSecretRequestCaptor.getAllValues();
        CreateSecretRequest csr = createSecretRequests.get(0);
        assertEquals("dev.org.sagebionetworks.oauth2.sagebio.client.id", csr.name());
        assertEquals("client-101", csr.secretString());
        csr = createSecretRequests.get(1);
        assertEquals("dev.org.sagebionetworks.oauth2.sagebio.client.secret", csr.name());
        assertEquals("secret-999", csr.secretString());
        csr = createSecretRequests.get(2);
        assertEquals("dev.org.sagebionetworks.oauth2.sagebio.discoveryDocument", csr.name());
        assertEquals("https://cognito-idp.us-east-1.amazonaws.com/user-pool-102/.well-known/openid-configuration", csr.secretString());
    }

    @Test
    public void testBuildGlobalResourcesProd() throws InterruptedException {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");
        when(mockConfig.getProperty(PROPERTY_KEY_OPS_VPC_EXPORT_PREFIX)).thenReturn("us-east-1-vpc");
        when(mockStsClient.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder().arn("currentIdentityArn").build());
        when(mockCloudFormationClientWrapper.getOutput("synapse-prod-global-resources", GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC)).thenReturn("complaintTopicArn");
        when(mockCloudFormationClientWrapper.getOutput("synapse-prod-global-resources", GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC)).thenReturn("bounceTopicArn");
        when(mockCloudFormationClientWrapper.getOutput("synapse-prod-global-resources", GLOBAL_CFSTACK_OUTPUT_KEY_ROUTE53_QUERY_LOG_GROUP_ARN)).thenReturn(LOG_GROUP_ARN);
        when(mockRoute53Client.listHostedZones(any(ListHostedZonesRequest.class))).thenReturn(ListHostedZonesResponse.builder()
        		.hostedZones(publicZone("/hostedzone/ZONE1", "synapse.org."))
        		.isTruncated(false)
        		.build());
        when(mockRoute53Client.listQueryLoggingConfigs(any(ListQueryLoggingConfigsRequest.class))).thenReturn(noQueryLoggingConfigs());
        when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);
        when(mockCloudFormationClientWrapper.waitForStackToComplete("synapse-prod-global-resources")).thenReturn(Optional.of(stack));
        DescribeUserPoolClientResponse userPoolClientResponse = 
        		DescribeUserPoolClientResponse.builder().userPoolClient(
        				UserPoolClientType.builder().clientId("client-101").clientSecret("secret-999").build()
        		).build();
        when(mockCognitoIdentityProviderClient.describeUserPoolClient(any(DescribeUserPoolClientRequest.class))).thenReturn(userPoolClientResponse);

        builder.buildGlobalResources(); // call under test

        verify(mockCloudFormationClientWrapper).createOrUpdateStack(stackRequestCaptor.capture());
        CreateOrUpdateStackRequest req = stackRequestCaptor.getValue();
        assertEquals("synapse-prod-global-resources", req.getStackName());
        assertEquals(expectedTags, req.getTags());
        assertNull(req.getParameters());
        assertNotNull(req.getTemplateBody());
        
        JSONObject templateJSON = new JSONObject(req.getTemplateBody());
//        System.out.println(templateJSON.toString(2));

        assertTrue(validateResources("prod", templateJSON));

        verify(mockSesClient).setComplaintNotificationTopic(SES_SYNAPSE_DOMAIN, "complaintTopicArn");
        verify(mockSesClient).setBounceNotificationTopic(SES_SYNAPSE_DOMAIN, "bounceTopicArn");
        verify(mockRoute53Client).createQueryLoggingConfig(CreateQueryLoggingConfigRequest.builder()
        		.hostedZoneId("ZONE1").cloudWatchLogsLogGroupArn(LOG_GROUP_ARN).build());

        verify(mockSecretsManager, times(3)).createSecret(createSecretRequestCaptor.capture());
        // check that the correct secret keys and values are passed
        List<CreateSecretRequest> createSecretRequests = createSecretRequestCaptor.getAllValues();
        CreateSecretRequest csr = createSecretRequests.get(0);
        assertEquals("prod.org.sagebionetworks.oauth2.sagebio.client.id", csr.name());
        assertEquals("client-101", csr.secretString());
        csr = createSecretRequests.get(1);
        assertEquals("prod.org.sagebionetworks.oauth2.sagebio.client.secret", csr.name());
        assertEquals("secret-999", csr.secretString());
        csr = createSecretRequests.get(2);
        assertEquals("prod.org.sagebionetworks.oauth2.sagebio.discoveryDocument", csr.name());
        assertEquals("https://cognito-idp.us-east-1.amazonaws.com/user-pool-102/.well-known/openid-configuration", csr.secretString());

    }

    private boolean validateResources(String stack, JSONObject templateJSON) {
        return(validateSynapseHelpCollectionResources(stack, templateJSON)
                && validateStackNotificationTopics(stack, templateJSON)
                && validateSesTopics(templateJSON)
                && validateRdsSnapshotCmk(stack, templateJSON)
                && validateWebAclLogGroup(stack, templateJSON)
                && validateRoute53QueryLogging(stack, templateJSON)
        );
    }

    private boolean validateRoute53QueryLogging(String stack, JSONObject templateJSON) {
        final JSONObject resources = templateJSON.getJSONObject("Resources");
        final List<String> expectedKeys = List.of(stack + "Route53QueryLogGroup", stack + "Route53QueryLogResourcePolicy");
        assertEquals("/aws/route53/" + stack + "-public-hosted-zones",
                resources.getJSONObject(stack + "Route53QueryLogGroup").getJSONObject("Properties").getString("LogGroupName"));
        // the policy document must be valid JSON once joined
        JSONObject policyJoin = resources.getJSONObject(stack + "Route53QueryLogResourcePolicy")
                .getJSONObject("Properties").getJSONObject("PolicyDocument");
        StringBuilder policy = new StringBuilder();
        policyJoin.getJSONArray("Fn::Join").getJSONArray(1).forEach(part -> policy.append(part instanceof String ? part : "x"));
        assertEquals("route53.amazonaws.com", new JSONObject(policy.toString()).getJSONArray("Statement")
                .getJSONObject(0).getJSONObject("Principal").getString("Service"));
        return resources.keySet().containsAll(expectedKeys)
                && templateJSON.getJSONObject("Outputs").has(GLOBAL_CFSTACK_OUTPUT_KEY_ROUTE53_QUERY_LOG_GROUP_ARN);
    }

    @Test
    public void testEnableQueryLoggingOnPublicHostedZones() {
        when(mockRoute53Client.listHostedZones(any(ListHostedZonesRequest.class))).thenReturn(ListHostedZonesResponse.builder()
        		.hostedZones(
        				publicZone("/hostedzone/ZONE1", "synapse.org."),
        				HostedZone.builder().id("/hostedzone/PRIVATE").name("private.org.")
        					.config(HostedZoneConfig.builder().privateZone(true).build()).build(),
        				publicZone("/hostedzone/ZONE2", "prod.sagebase.org."))
        		.isTruncated(false)
        		.build());
        when(mockRoute53Client.listQueryLoggingConfigs(ListQueryLoggingConfigsRequest.builder().hostedZoneId("ZONE1").build()))
        		.thenReturn(noQueryLoggingConfigs());
        when(mockRoute53Client.listQueryLoggingConfigs(ListQueryLoggingConfigsRequest.builder().hostedZoneId("ZONE2").build()))
        		.thenReturn(ListQueryLoggingConfigsResponse.builder()
        				.queryLoggingConfigs(QueryLoggingConfig.builder().id("existing").hostedZoneId("ZONE2").build()).build());

        // call under test
        builder.enableQueryLoggingOnPublicHostedZones(LOG_GROUP_ARN);

        verify(mockRoute53Client, times(1)).createQueryLoggingConfig(createQueryLoggingConfigRequestCaptor.capture());
        CreateQueryLoggingConfigRequest req = createQueryLoggingConfigRequestCaptor.getValue();
        assertEquals("ZONE1", req.hostedZoneId());
        assertEquals(LOG_GROUP_ARN, req.cloudWatchLogsLogGroupArn());
        verify(mockRoute53Client, never()).listQueryLoggingConfigs(ListQueryLoggingConfigsRequest.builder().hostedZoneId("PRIVATE").build());
    }

    @Test
    public void testEnableQueryLoggingOnPublicHostedZonesWithPagination() {
        when(mockRoute53Client.listHostedZones(ListHostedZonesRequest.builder().build())).thenReturn(ListHostedZonesResponse.builder()
        		.hostedZones(publicZone("/hostedzone/ZONE1", "synapse.org."))
        		.isTruncated(true)
        		.nextMarker("next")
        		.build());
        when(mockRoute53Client.listHostedZones(ListHostedZonesRequest.builder().marker("next").build())).thenReturn(ListHostedZonesResponse.builder()
        		.hostedZones(publicZone("/hostedzone/ZONE2", "prod.sagebase.org."))
        		.isTruncated(false)
        		.build());
        when(mockRoute53Client.listQueryLoggingConfigs(any(ListQueryLoggingConfigsRequest.class))).thenReturn(noQueryLoggingConfigs());

        // call under test
        builder.enableQueryLoggingOnPublicHostedZones(LOG_GROUP_ARN);

        verify(mockRoute53Client, times(2)).createQueryLoggingConfig(createQueryLoggingConfigRequestCaptor.capture());
        List<CreateQueryLoggingConfigRequest> requests = createQueryLoggingConfigRequestCaptor.getAllValues();
        assertEquals("ZONE1", requests.get(0).hostedZoneId());
        assertEquals("ZONE2", requests.get(1).hostedZoneId());
    }

    @Test
    public void testEnableQueryLoggingWithAlreadyExistsException() {
        when(mockRoute53Client.listQueryLoggingConfigs(any(ListQueryLoggingConfigsRequest.class))).thenReturn(noQueryLoggingConfigs());
        when(mockRoute53Client.createQueryLoggingConfig(any(CreateQueryLoggingConfigRequest.class)))
        		.thenThrow(QueryLoggingConfigAlreadyExistsException.builder().message("exists").build());

        // call under test, should not throw
        builder.enableQueryLogging(publicZone("/hostedzone/ZONE1", "synapse.org."), LOG_GROUP_ARN);

        verify(mockRoute53Client).createQueryLoggingConfig(any(CreateQueryLoggingConfigRequest.class));
    }

    @Test
    public void testEnableQueryLoggingOnPublicHostedZonesWithMissingArn() {
        assertThrows(IllegalStateException.class, () -> {
        	// call under test
        	builder.enableQueryLoggingOnPublicHostedZones(null);
        });
        verifyZeroInteractions(mockRoute53Client);
    }

    private static HostedZone publicZone(String id, String name) {
        return HostedZone.builder().id(id).name(name).config(HostedZoneConfig.builder().privateZone(false).build()).build();
    }

    private static ListQueryLoggingConfigsResponse noQueryLoggingConfigs() {
        return ListQueryLoggingConfigsResponse.builder().queryLoggingConfigs(List.of()).build();
    }

    private boolean validateStackNotificationTopics(String stack, JSONObject templateJSON) {
        final JSONObject resources = templateJSON.getJSONObject("Resources");
        final Set<String> actualKeys = resources.keySet();
        final List<String> resourceNameSuffixes = List.of("NotificationTopic", "NotificationTopicPolicy");
        final List<String> expectedKeys = resourceNameSuffixes.stream().map(s -> stack + s).collect(Collectors.toList());
        boolean valid = actualKeys.containsAll(expectedKeys);
        return valid;
    }

    private boolean validateSesTopics(JSONObject templateJSON) {
        final JSONObject resources = templateJSON.getJSONObject("Resources");
        final Set<String> actualKeys = resources.keySet();
        final List<String> expectedKeys = List.of("SesSynapseOrgBounceTopic", "SesSynapseOrgComplaintTopic");
        boolean valid = actualKeys.containsAll(expectedKeys);
        return valid;
    }

    private boolean validateWebAclLogGroup(String stack, JSONObject templateJSON) {
        final JSONObject resources = templateJSON.getJSONObject("Resources");
        final Set<String> actualKeys = resources.keySet();
        final List<String> expectedKeys = List.of(stack + "WebAclLogGroup");
        boolean valid = actualKeys.containsAll(expectedKeys);
        return valid;
    }


    private boolean validateRdsSnapshotCmk(String stack, JSONObject templateJSON) {
        final JSONObject resources = templateJSON.getJSONObject("Resources");
        final Set<String> actualKeys = resources.keySet();
        final List<String> suffixes = List.of("RdsSnapshotCmk", "RdsSnapshotCmkAlias");
        final List<String> expectedKeys = suffixes.stream().map(s -> stack + s).collect(Collectors.toList());
        boolean valid = actualKeys.containsAll(expectedKeys);
        return valid;
    }

    private boolean validateSynapseHelpCollectionResources(String stack, JSONObject templateJSON) {
        final JSONObject resources = templateJSON.getJSONObject("Resources");
        final Set<String> actualKeys = resources.keySet();
        final List<String> expectedKeys = List.of(
                "SynapseHelpKnowledgeBaseExecutionRole",
                "SynapseHelpCollectionDeployerDataAccessPolicy",
                "SynapseHelpCollectionKnowledgeBaseDataAccessPolicy",
                "SynapseHelpCollectionEncryptionPolicy",
                "SynapseHelpCollectionNetworkPolicy",
                "SynapseHelpCollection",
                "SynapseHelpCollectionCreateIndexWaitCondition",
                "SynapseHelpKnoweldgeBaseExecutionRolePolicy",
                "SynapseHelpKnowledgeBase",
                "SynapseHelpKnowledgeBaseDataSource",
                "SynapseHelpKnowledgeBaseDataSourceSyncWaitCondition",
                "SynapseHelpKnowledgeBaseIngestionScheduleRole",
                "SynapseHelpKnowledgeBaseIngestionSchedule"
                );
        boolean valid = actualKeys.containsAll(expectedKeys);
        return valid;
    }

}