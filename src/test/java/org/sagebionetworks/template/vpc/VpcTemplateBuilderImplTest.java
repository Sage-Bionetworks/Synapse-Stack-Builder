package org.sagebionetworks.template.vpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PARAMETER_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PARAMETER_VPN_CIDR;
import static org.sagebionetworks.template.Constants.PEERING_ROLE_ARN_PREFIX;
import static org.sagebionetworks.template.Constants.PEER_ROLE_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_COLORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_AVAILABILITY_ZONES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_PEERING_ACCEPT_ROLE_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_VPN_CIDR;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.*;
import static org.sagebionetworks.template.Constants.VPC_CIDR;

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
import org.sagebionetworks.template.*;
import org.sagebionetworks.template.config.Configuration;

import com.amazonaws.services.cloudformation.model.Parameter;

import java.util.LinkedList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class VpcTemplateBuilderImplTest {

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
	VpcTemplateBuilderImpl builder;
	
	String[] colors;
	String subnetPrefix;
	String[] avialabilityZones;
	String[] publicZones;
	String vpnCider;
	String stack;
	String peeringRoleARN;
	String oldVpcId;
	String oldVpcCidr;

	List<Tag> expectedTags;

	@Before
	public void before() {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

		expectedTags = new LinkedList<>();
		Tag t = new Tag().withKey("aKey").withValue("aValue");
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

		builder = new VpcTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, mockConfig, mockLoggerFactory, mockStackTagsProvider);
		colors = new String[] {"Red","Green"};
		subnetPrefix = "10.21";
		avialabilityZones = new String[] {"us-east-1a","us-east-1b"};
		vpnCider = "10.1.0.0/16";
		stack = "dev";
		peeringRoleARN = PEERING_ROLE_ARN_PREFIX+"/someKey";
		oldVpcCidr = "10.2.0.0/16";
		oldVpcId = "vpc-123def";
		
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_COLORS)).thenReturn(colors);
		when(mockConfig.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX)).thenReturn(subnetPrefix);
		when(mockConfig.getProperty(PROPERTY_KEY_VPC_AVAILABILITY_ZONES)).thenReturn("us-east-1a,us-east-1b");
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_VPC_AVAILABILITY_ZONES)).thenReturn(avialabilityZones);
		when(mockConfig.getProperty(PROPERTY_KEY_VPC_VPN_CIDR)).thenReturn(vpnCider);
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(mockConfig.getProperty(PROPERTY_KEY_VPC_PEERING_ACCEPT_ROLE_ARN)).thenReturn(peeringRoleARN);
		when(mockConfig.getProperty(PROPERTY_KEY_OLD_VPC_ID)).thenReturn(oldVpcId);
		when(mockConfig.getProperty(PROPERTY_KEY_OLD_VPC_CIDR)).thenReturn(oldVpcCidr);
	}
	
	@Test
	public void testStackName() {
		// Call under test
		String name = builder.createStackName();
		assertEquals("synapse-dev-vpc-2020", name);
	}

	@Test
	public void testBuildAndDepoy() {
		// call under test
		builder.buildAndDeploy();
		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertEquals("synapse-dev-vpc-2020", request.getStackName());
		assertNotNull(request.getParameters());
		assertEquals(expectedTags, request.getTags());
		JSONObject templateJson = new JSONObject(request.getTemplateBody());
		System.out.println(templateJson.toString(JSON_INDENT));
		
		JSONObject resouces = templateJson.getJSONObject("Resources");
		assertNotNull(resouces);
		assertTrue(resouces.has("VPC"));
		assertTrue(resouces.has("VpcPeeringConnection"));
		assertTrue(resouces.has("InternetGateway"));
		assertTrue(resouces.has("InternetGatewayAttachment"));
		assertTrue(resouces.has("VpnSecurityGroup"));
		// color subnets
		// Red subnets
		assertTrue(resouces.has("RedPrivateUsEast1a"));
		assertTrue(resouces.has("RedPrivateUsEast1b"));
		assertTrue(resouces.has("RedPrivateUsEast1aRouteTableAssociation"));
		assertTrue(resouces.has("RedPrivateUsEast1bRouteTableAssociation"));
		// Green
		// Green subnets
		assertTrue(resouces.has("GreenPrivateUsEast1a"));
		assertTrue(resouces.has("GreenPrivateUsEast1b"));
		assertTrue(resouces.has("GreenPrivateUsEast1aRouteTableAssociation"));
		assertTrue(resouces.has("GreenPrivateUsEast1bRouteTableAssociation"));
	}
	
	@Test
	public void testCreateParameters() {
		String stackName = "stackName";
		// call under test
		Parameter[] parameters = builder.createParameters(stackName);
		assertNotNull(parameters);
		assertEquals(4, parameters.length);
		// keys
		assertEquals(PARAMETER_VPC_SUBNET_PREFIX,parameters[0].getParameterKey());
		assertEquals(PARAMETER_VPN_CIDR,parameters[1].getParameterKey());
		assertEquals(PARAMETER_OLD_VPC_ID, parameters[2].getParameterKey());
		assertEquals(PARAMETER_OLD_VPC_CIDR, parameters[3].getParameterKey());
		// values
		assertEquals(subnetPrefix, parameters[0].getParameterValue());
		assertEquals(vpnCider, parameters[1].getParameterValue());
		assertEquals(oldVpcId, parameters[2].getParameterValue());
		assertEquals(oldVpcCidr, parameters[3].getParameterValue());
	}
	
	@Test
	public void testGetColorsFromProperty() {
		// Call under test
		Color[] colors = builder.getColorsFromProperty();
		assertNotNull(colors);
		assertEquals(2, colors.length);
		assertEquals(Color.Red, colors[0]);
		assertEquals(Color.Green, colors[1]);
	}
	
	@Test
	public void testCreateContext() {
		// call under test
		VelocityContext context = builder.createContext();
		assertNotNull(context);
		Subnets subnets = (Subnets) context.get(SUBNETS);
		assertEquals(2, subnets.getPublicSubnets().length);
		assertEquals("PublicUsEast1a", subnets.getPublicSubnets()[0].getName());
		assertEquals("PublicUsEast1b", subnets.getPublicSubnets()[1].getName());
		
		assertEquals(2, subnets.getPrivateSubnetGroups().length);
		assertEquals("Red", subnets.getPrivateSubnetGroups()[0].getColor());
		assertEquals("Green", subnets.getPrivateSubnetGroups()[1].getColor());
		
		assertEquals("10.21.0.0/16", context.get(VPC_CIDR));
		assertEquals("dev", context.get(STACK));
		assertEquals(peeringRoleARN, context.get(PEER_ROLE_ARN));
	}
	
	@Test
	public void testGetPeeringRoleArn() {
		String arn = builder.getPeeringRoleArn();
		assertEquals(peeringRoleARN, arn);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetPeeringRoleArnWrongPrefix() {
		// value without the arn prefix
		when(mockConfig.getProperty(PROPERTY_KEY_VPC_PEERING_ACCEPT_ROLE_ARN)).thenReturn("no prefix");
		String arn = builder.getPeeringRoleArn();
		assertEquals(peeringRoleARN, arn);
	}
}
