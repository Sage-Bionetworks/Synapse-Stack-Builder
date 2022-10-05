package org.sagebionetworks.template.global;

import com.amazonaws.services.cloudformation.model.Tag;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.SesClientImpl;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.Configuration;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC;
import static org.sagebionetworks.template.Constants.GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.SES_SYNAPSE_DOMAIN;
import static org.sagebionetworks.template.Constants.STACK;

@RunWith(MockitoJUnitRunner.class)
public class GlobalResourcesBuilderImplTest {

    @Mock
    Configuration mockConfig;
    @Mock
    CloudFormationClient mockCloudFormationClient;
    VelocityEngine velocityEngine;
    @Mock
    LoggerFactory mockLoggerFactory;
    @Mock
    Logger mockLogger;
    @Mock
    StackTagsProvider mockStackTagsProvider;
    @Mock
    SesClientImpl mockSesClient;

    List<Tag> expectedTags;

    @Captor
    ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;


    GlobalResourcesBuilderImpl builder;

    @Before
    public void before() {
        velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

        when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

        expectedTags = new LinkedList<>();
        Tag t = new Tag().withKey("aKey").withValue("aValue");
        expectedTags.add(t);

        builder = new GlobalResourcesBuilderImpl(mockCloudFormationClient, velocityEngine, mockConfig, mockLoggerFactory, mockStackTagsProvider, mockSesClient);

    }

    @Test
    public void testCreateStackName() {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");
        assertEquals("synapse-prod-global-resources", builder.createStackName());
    }

    @Test
    public void testCreateContext() {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
        VelocityContext context = builder.createContext();
        assertEquals("dev", context.get(STACK));
    }

    @Test
    public void testSetupSesTopics() {
        String stackName = "prodStackName";
        when(mockCloudFormationClient.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC)).thenReturn("theComplaintTopic");
        when(mockCloudFormationClient.getOutput(stackName, GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC)).thenReturn("theBounceTopic");

        // call under test
        builder.setupSesTopics(stackName);

        verify(mockSesClient).setComplaintNotificationTopic(SES_SYNAPSE_DOMAIN, "theComplaintTopic");
        verify(mockSesClient).setBounceNotificationTopic(SES_SYNAPSE_DOMAIN, "theBounceTopic");
    }

    @Test
    public void testBuildGlobalResourcesDev() throws InterruptedException {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
        when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

        builder.buildGlobalResources(); // call under test

        verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
        CreateOrUpdateStackRequest req = requestCaptor.getValue();
        assertEquals("synapse-dev-global-resources", req.getStackName());
        assertEquals(expectedTags, req.getTags());
        assertNull(req.getParameters());
        assertNotNull(req.getTemplateBody());

        JSONObject templateJSON = new JSONObject(req.getTemplateBody());
        JSONObject resources = templateJSON.getJSONObject("Resources");
        assertTrue(resources.has("SesSynapseOrgBounceTopic"));
        assertTrue(resources.has("SesSynapseOrgComplaintTopic"));
        assertTrue(resources.has("devNotificationTopic"));
        assertFalse(resources.has("SesHighBounceRateAlarm")); // dev stack does not have alarm

        verify(mockSesClient, never()).setComplaintNotificationTopic(anyString(), anyString());
        verify(mockSesClient, never()).setBounceNotificationTopic(anyString(), anyString());

    }

    @Test
    public void testBuildGlobalResourcesProd() throws InterruptedException {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");
        when(mockCloudFormationClient.getOutput("synapse-prod-global-resources", GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC)).thenReturn("complaintTopicArn");
        when(mockCloudFormationClient.getOutput("synapse-prod-global-resources", GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC)).thenReturn("bounceTopicArn");
        when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

        builder.buildGlobalResources(); // call under test

        verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
        CreateOrUpdateStackRequest req = requestCaptor.getValue();
        assertEquals("synapse-prod-global-resources", req.getStackName());
        assertEquals(expectedTags, req.getTags());
        assertNull(req.getParameters());
        assertNotNull(req.getTemplateBody());

        JSONObject templateJSON = new JSONObject(req.getTemplateBody());
        JSONObject resources = templateJSON.getJSONObject("Resources");
        assertTrue(resources.has("SesSynapseOrgBounceTopic"));
        assertTrue(resources.has("SesSynapseOrgComplaintTopic"));
        assertTrue(resources.has("prodNotificationTopic"));
        assertTrue(resources.has("SesHighBounceRateAlarm"));
        assertTrue(resources.has("RdsEnhancedMonitoringRole"));
        JSONObject outputs = templateJSON.getJSONObject("Outputs");
        assertTrue(outputs.has("RdsEnhancedMonitoringRole"));
        assertTrue(outputs.has("SesSynapseOrgBounceTopic"));
        assertTrue(outputs.has("SesSynapseOrgComplaintTopic"));
        assertTrue(outputs.has("NotificationTopic"));

        verify(mockSesClient).setComplaintNotificationTopic(SES_SYNAPSE_DOMAIN, "complaintTopicArn");
        verify(mockSesClient).setBounceNotificationTopic(SES_SYNAPSE_DOMAIN, "bounceTopicArn");

    }

}