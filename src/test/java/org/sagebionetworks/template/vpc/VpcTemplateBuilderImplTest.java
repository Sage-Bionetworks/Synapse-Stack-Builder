package org.sagebionetworks.template.vpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PARAMETER_PRIVATE_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PARAMETER_PUBLIC_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PARAMETER_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PARAMETER_VPN_CIDR;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_COLORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_PRIVATE_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_PUBLIC_SUBNET_ZONES;
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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.PropertyProvider;
import org.sagebionetworks.template.TemplateGuiceModule;

import com.amazonaws.services.cloudformation.model.Parameter;

@RunWith(MockitoJUnitRunner.class)
public class VpcTemplateBuilderImplTest {

	@Mock
	CloudFormationClient mockCloudFormationClient;
	@Mock
	PropertyProvider mockPropertyProvider;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;

	VelocityEngine velocityEngine;
	VpcTemplateBuilderImpl builder;
	
	String colors;
	String subnetPrefix;
	String privateZones;
	String publicZones;
	String vpnCider;
	String stack;

	@Before
	public void before() {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		
		builder = new VpcTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, mockPropertyProvider, mockLoggerFactory);
		colors = " Red , Green ";
		subnetPrefix = "10.21";
		privateZones = "us-east-1a,us-east-1b";
		publicZones = "us-east-1c,us-east-1e";
		vpnCider = "10.1.0.0/16";
		stack = "dev";
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_COLORS)).thenReturn(colors);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX)).thenReturn(subnetPrefix);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_VPC_PRIVATE_SUBNET_ZONES)).thenReturn(privateZones);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_VPC_PUBLIC_SUBNET_ZONES)).thenReturn(publicZones);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_VPC_VPN_CIDR)).thenReturn(vpnCider);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
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
		ArgumentCaptor<String> nameCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> bodyCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Parameter[]> parameterCatpure = ArgumentCaptor.forClass(Parameter[].class);
		verify(mockCloudFormationClient).createOrUpdateStack(nameCapture.capture(), bodyCapture.capture(), parameterCatpure.capture());
		assertEquals("synapse-dev-vpc", nameCapture.getValue());
		assertNotNull(parameterCatpure.getValue());
		System.out.println(bodyCapture.getValue());
		JSONObject templateJson = new JSONObject(bodyCapture.getValue());
		
		JSONObject resouces = templateJson.getJSONObject("Resources");
		assertNotNull(resouces);
		assertTrue(resouces.has("VPC"));
		assertTrue(resouces.has("InternetGateway"));
		assertTrue(resouces.has("GatewayToInternet"));
		assertTrue(resouces.has("PublicRouteTable"));
		assertTrue(resouces.has("PublicRoute"));
		assertTrue(resouces.has("ElasticIP"));
		assertTrue(resouces.has("PrivateRouteTable"));
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
		assertTrue(resouces.has("RedPublic0Subnet"));
		assertTrue(resouces.has("RedPublic1Subnet"));
		assertTrue(resouces.has("RedPrivate0Subnet"));
		assertTrue(resouces.has("RedPrivate1Subnet"));
		assertTrue(resouces.has("RedPublic0SubnetRouteTableAssociation"));
		assertTrue(resouces.has("RedPublic1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("RedPublic0SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("RedPublic1SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("RedPrivate0SubnetRouteTableAssociation"));
		assertTrue(resouces.has("RedPrivate1SubnetRouteTableAssociation"));
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
		assertTrue(resouces.has("GreenPublic0Subnet"));
		assertTrue(resouces.has("GreenPublic1Subnet"));
		assertTrue(resouces.has("GreenPrivate0Subnet"));
		assertTrue(resouces.has("GreenPrivate1Subnet"));
		assertTrue(resouces.has("GreenPublic0SubnetRouteTableAssociation"));
		assertTrue(resouces.has("GreenPublic1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("GreenPublic0SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("GreenPublic1SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("GreenPrivate0SubnetRouteTableAssociation"));
		assertTrue(resouces.has("GreenPrivate1SubnetRouteTableAssociation"));
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
		assertEquals(PARAMETER_PRIVATE_SUBNET_ZONES,parameters[1].getParameterKey());
		assertEquals(PARAMETER_PUBLIC_SUBNET_ZONES,parameters[2].getParameterKey());
		assertEquals(PARAMETER_VPN_CIDR,parameters[3].getParameterKey());
		// values
		assertEquals(subnetPrefix, parameters[0].getParameterValue());
		assertEquals(privateZones, parameters[1].getParameterValue());
		assertEquals(publicZones, parameters[2].getParameterValue());
		assertEquals(vpnCider, parameters[3].getParameterValue());
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
	}
}
