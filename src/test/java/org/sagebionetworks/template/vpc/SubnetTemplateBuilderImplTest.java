package org.sagebionetworks.template.vpc;

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
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.Configuration;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.*;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OLD_VPC_CIDR;

@RunWith(MockitoJUnitRunner.class)
public class SubnetTemplateBuilderImplTest {

    @Mock
    CloudFormationClient mockCloudFormationClient;
    @Mock
    Configuration mockConfig;
    @Mock
    LoggerFactory mockLoggerFactory;
    @Mock
    Logger mockLogger;
    @Mock
    StackTagsProvider mockStackTagsProvider;

    @Captor
    ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;

    VelocityEngine velocityEngine;
    SubnetTemplateBuilderImpl builder;

    String subnetPrefix;
    String[] avialabilityZones;
    String[] publicZones;
    String vpnCider;
    String stack;
    String peeringRoleARN;
    String oldVpcId;
    String oldVpcCidr;
    String[] colors;

    List<Tag> expectedTags;

    @Before
    public void before() {
        // use a real velocity engine
        velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

        when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

        expectedTags = new LinkedList<>();
        Tag t = new Tag().withKey("aKey").withValue("aValue");
        when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

        builder = new SubnetTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, mockConfig, mockLoggerFactory, mockStackTagsProvider);

        colors = new String[] {"Red", "Green"};
        subnetPrefix = "10.24";
        avialabilityZones = new String[] {"us-east-1a","us-east-1b"};
        vpnCider = "10.1.0.0/16";
        stack = "dev";
        peeringRoleARN = PEERING_ROLE_ARN_PREFIX+"/someKey";
        oldVpcCidr = "10.21.0.0/16";
        oldVpcId = "vpc-123def";

        when(mockConfig.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX)).thenReturn(subnetPrefix);
        when(mockConfig.getProperty(PROPERTY_KEY_VPC_AVAILABILITY_ZONES)).thenReturn("us-east-1a,us-east-1b");
        when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_VPC_AVAILABILITY_ZONES)).thenReturn(avialabilityZones);
        when(mockConfig.getProperty(PROPERTY_KEY_VPC_VPN_CIDR)).thenReturn(vpnCider);
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        when(mockConfig.getProperty(PROPERTY_KEY_VPC_PEERING_ACCEPT_ROLE_ARN)).thenReturn(peeringRoleARN);
        when(mockConfig.getProperty(PROPERTY_KEY_OLD_VPC_ID)).thenReturn(oldVpcId);
        when(mockConfig.getProperty(PROPERTY_KEY_OLD_VPC_CIDR)).thenReturn(oldVpcCidr);
        when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_COLORS)).thenReturn(colors);

    }

    @Test
    public void testCreatePublicSubnetsStackName() {
        String name = builder.createPublicSubnetsStackName();
        assertEquals("synapse-dev-vpc-2020-public-subnets", name);
    }

    @Test
    public void testCreatePrivateSubnetStackName() {
        String name = builder.createPrivateSubnetStackName("red");
        assertEquals("synapse-dev-vpc-2020-private-subnets-red", name);
    }

    @Test
    public void testCreateContext() {
        // call under test
        VelocityContext context = builder.createContext();

        assertNotNull(context);
        assertEquals("10.24.0.0/16", context.get(VPC_CIDR));
        String avZonesStr = (String)context.get(AVAILABILITY_ZONES);
        assertEquals("us-east-1a,us-east-1b", avZonesStr);
        assertEquals("dev", context.get(STACK));
        assertEquals("synapse-dev-vpc-2020", context.get(VPC_STACKNAME));
        assertNotNull(context.get(SUBNETS));

        assertEquals("10.21.0.0/16", context.get(TEMP_VPC_CIDR));

    }

    @Test
    public void testBuildAndDeployPublicSubnets() throws Exception {
        // call under test
        builder.buildAndDeployPublicSubnets();

        verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
        CreateOrUpdateStackRequest request = requestCaptor.getValue();
        assertEquals("synapse-dev-vpc-2020-public-subnets", request.getStackName());
        assertNull(request.getParameters());
        assertEquals(expectedTags, request.getTags());
        JSONObject templateJson = new JSONObject(request.getTemplateBody());
        System.out.println(templateJson.toString(JSON_INDENT));

    }

    @Test
    public void testBuildAndDeployPrivateSubnets() throws Exception {
        // call under test
        builder.buildAndDeployPrivateSubnets();

        verify(mockCloudFormationClient, times(2)).createOrUpdateStack(requestCaptor.capture());
        List<CreateOrUpdateStackRequest> requests = requestCaptor.getAllValues();

        assertEquals(2, requests.size());
        assertEquals("synapse-dev-vpc-2020-private-subnets-Red", requests.get(0).getStackName());
        assertNull(requests.get(0).getParameters());
        assertEquals(expectedTags, requests.get(0).getTags());
        assertEquals("synapse-dev-vpc-2020-private-subnets-Green", requests.get(1).getStackName());
        assertNull(requests.get(1).getParameters());
        assertEquals(expectedTags, requests.get(1).getTags());

        JSONObject templateJson = new JSONObject(requests.get(0).getTemplateBody());
        System.out.println(templateJson.toString(JSON_INDENT));
    }
}