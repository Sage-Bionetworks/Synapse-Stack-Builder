package org.sagebionetworks.template.global;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC;
import static org.sagebionetworks.template.Constants.IDENTITY_ARN;
import static org.sagebionetworks.template.Constants.OPS_VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.SES_SYNAPSE_DOMAIN;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OPS_VPC_EXPORT_PREFIX;

import java.util.LinkedList;
import java.util.List;

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
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.Configuration;

import software.amazon.awssdk.services.cloudformation.model.Tag;
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
	
    List<Tag> expectedTags;

    @Captor
    ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;


    GlobalResourcesBuilderImpl builder;

    @BeforeEach
    public void before() {
        velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
        
        expectedTags = new LinkedList<>();
        Tag t = Tag.builder().key("aKey").value("aValue").build();
        expectedTags.add(t);

        builder = new GlobalResourcesBuilderImpl(mockCloudFormationClientWrapper, velocityEngine, mockConfig, mockStackTagsProvider, mockSesClient, mockStsClient);

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

        builder.buildGlobalResources(); // call under test

        verify(mockCloudFormationClientWrapper).createOrUpdateStack(requestCaptor.capture());
        CreateOrUpdateStackRequest req = requestCaptor.getValue();
        assertEquals("synapse-dev-global-resources", req.getStackName());
        assertEquals(expectedTags, req.getTags());
        assertNull(req.getParameters());
        assertNotNull(req.getTemplateBody());

        String expectedJson = new JSONObject(TemplateUtils.loadContentFromFile("global/dev-global-resources.json")).toString();
        
        JSONObject templateJSON = new JSONObject(req.getTemplateBody());
//        System.out.println(templateJSON.toString(2));
        
        assertEquals(expectedJson, templateJSON.toString());

        verify(mockSesClient, never()).setComplaintNotificationTopic(anyString(), anyString());
        verify(mockSesClient, never()).setBounceNotificationTopic(anyString(), anyString());

    }

    @Test
    public void testBuildGlobalResourcesProd() throws InterruptedException {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");
        when(mockConfig.getProperty(PROPERTY_KEY_OPS_VPC_EXPORT_PREFIX)).thenReturn("us-east-1-vpc");
        when(mockStsClient.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder().arn("currentIdentityArn").build());
        when(mockCloudFormationClientWrapper.getOutput("synapse-prod-global-resources", GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC)).thenReturn("complaintTopicArn");
        when(mockCloudFormationClientWrapper.getOutput("synapse-prod-global-resources", GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC)).thenReturn("bounceTopicArn");
        when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);

        builder.buildGlobalResources(); // call under test

        verify(mockCloudFormationClientWrapper).createOrUpdateStack(requestCaptor.capture());
        CreateOrUpdateStackRequest req = requestCaptor.getValue();
        assertEquals("synapse-prod-global-resources", req.getStackName());
        assertEquals(expectedTags, req.getTags());
        assertNull(req.getParameters());
        assertNotNull(req.getTemplateBody());

        String expectedJson = new JSONObject(TemplateUtils.loadContentFromFile("global/prod-global-resources.json")).toString();
        
        JSONObject templateJSON = new JSONObject(req.getTemplateBody());
//        System.out.println(templateJSON.toString(2));

        assertEquals(expectedJson, templateJSON.toString());

        verify(mockSesClient).setComplaintNotificationTopic(SES_SYNAPSE_DOMAIN, "complaintTopicArn");
        verify(mockSesClient).setBounceNotificationTopic(SES_SYNAPSE_DOMAIN, "bounceTopicArn");

    }

}