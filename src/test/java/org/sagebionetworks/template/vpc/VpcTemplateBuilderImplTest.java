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
import static org.sagebionetworks.template.Constants.SUBNET_GROUPS;
import static org.sagebionetworks.template.Constants.VPC_CIDR;

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
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.TemplateGuiceModule;

import com.amazonaws.services.cloudformation.model.Parameter;

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

	@Before
	public void before() {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		
		builder = new VpcTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, mockConfig, mockLoggerFactory);
		colors = new String[] {"Red","Green"};
		subnetPrefix = "10.21";
		avialabilityZones = new String[] {"us-east-1a","us-east-1b"};
		vpnCider = "10.1.0.0/16";
		stack = "dev";
		peeringRoleARN = PEERING_ROLE_ARN_PREFIX+"/someKey";
		
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_COLORS)).thenReturn(colors);
		when(mockConfig.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX)).thenReturn(subnetPrefix);
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_VPC_AVAILABILITY_ZONES)).thenReturn(avialabilityZones);
		when(mockConfig.getProperty(PROPERTY_KEY_VPC_VPN_CIDR)).thenReturn(vpnCider);
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(mockConfig.getProperty(PROPERTY_KEY_VPC_PEERING_ACCEPT_ROLE_ARN)).thenReturn(peeringRoleARN);
	}
	
	@Test
	public void testStackName() {
		// Call under test
		String name = builder.createStackName();
		assertEquals("synapse-dev-vpc", name);
	}

	@Test
	public void testBuildAndDepoy() {
		// call under test
		builder.buildAndDeploy();
		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertEquals("synapse-dev-vpc", request.getStackName());
		assertNotNull(request.getParameters());
		JSONObject templateJson = new JSONObject(request.getTemplateBody());
		
		JSONObject resouces = templateJson.getJSONObject("Resources");
		assertNotNull(resouces);
		assertTrue(resouces.has("VPC"));
		assertTrue(resouces.has("VpcPeeringConnection"));
		assertTrue(resouces.has("InternetGateway"));
		assertTrue(resouces.has("InternetGatewayAttachment"));
		assertTrue(resouces.has("VpnSecurityGroup"));
		// color subnets
		// Red ACL
		assertTrue(resouces.has("RedPublicNetworkAcl"));
		assertTrue(resouces.has("RedInboundPublicNetworkAclEntry"));
		assertTrue(resouces.has("RedOutboundPublicNetworkAclEntry"));
		assertTrue(resouces.has("RedPrivateNetworkAcl"));
		assertTrue(resouces.has("RedInboundPrivateSameGroupNetworkAclEntry"));
		assertTrue(resouces.has("RedInboundPrivateVpnNetworkAclEntry"));
		assertTrue(resouces.has("RedOutboundPrivateNetworkAclEntry"));
		// Red subnets
		assertTrue(resouces.has("RedPublicUsEast1aSubnet"));
		assertTrue(resouces.has("RedPublicUsEast1bSubnet"));
		assertTrue(resouces.has("RedPrivateUsEast1aSubnet"));
		assertTrue(resouces.has("RedPrivateUsEast1bSubnet"));
		assertTrue(resouces.has("RedPublicUsEast1aSubnetRouteTableAssociation"));
		assertTrue(resouces.has("RedPublicUsEast1bSubnetRouteTableAssociation"));
		assertTrue(resouces.has("RedPublicUsEast1aSubnetNetworkAclAssociation"));
		assertTrue(resouces.has("RedPublicUsEast1bSubnetNetworkAclAssociation"));
		assertTrue(resouces.has("RedPrivateUsEast1aSubnetRouteTableAssociation"));
		assertTrue(resouces.has("RedPrivateUsEast1bSubnetRouteTableAssociation"));
		// Green
		// Green ACL
		assertTrue(resouces.has("GreenPublicNetworkAcl"));
		assertTrue(resouces.has("GreenInboundPublicNetworkAclEntry"));
		assertTrue(resouces.has("GreenOutboundPublicNetworkAclEntry"));
		assertTrue(resouces.has("GreenPrivateNetworkAcl"));
		assertTrue(resouces.has("GreenInboundPrivateSameGroupNetworkAclEntry"));
		assertTrue(resouces.has("GreenInboundPrivateVpnNetworkAclEntry"));
		assertTrue(resouces.has("GreenOutboundPrivateNetworkAclEntry"));
		// Green subnets
		assertTrue(resouces.has("GreenPublicUsEast1aSubnet"));
		assertTrue(resouces.has("GreenPublicUsEast1bSubnet"));
		assertTrue(resouces.has("GreenPrivateUsEast1aSubnet"));
		assertTrue(resouces.has("GreenPrivateUsEast1bSubnet"));
		assertTrue(resouces.has("GreenPublicUsEast1aSubnetRouteTableAssociation"));
		assertTrue(resouces.has("GreenPublicUsEast1bSubnetRouteTableAssociation"));
		assertTrue(resouces.has("GreenPublicUsEast1aSubnetNetworkAclAssociation"));
		assertTrue(resouces.has("GreenPublicUsEast1bSubnetNetworkAclAssociation"));
		assertTrue(resouces.has("GreenPrivateUsEast1aSubnetRouteTableAssociation"));
		assertTrue(resouces.has("GreenPrivateUsEast1bSubnetRouteTableAssociation"));
	}
	
	@Test
	public void testCreateParameters() {
		String stackName = "stackName";
		// call under test
		Parameter[] parameters = builder.createParameters(stackName);
		assertNotNull(parameters);
		assertEquals(2, parameters.length);
		// keys
		assertEquals(PARAMETER_VPC_SUBNET_PREFIX,parameters[0].getParameterKey());
		assertEquals(PARAMETER_VPN_CIDR,parameters[1].getParameterKey());
		// values
		assertEquals(subnetPrefix, parameters[0].getParameterValue());
		assertEquals(vpnCider, parameters[1].getParameterValue());
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
		SubnetGroup[] subnets = (SubnetGroup[]) context.get(SUBNET_GROUPS);
		assertEquals(2, subnets.length);
		assertEquals("Red", subnets[0].getColor());
		assertEquals("Green", subnets[1].getColor());
		
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
