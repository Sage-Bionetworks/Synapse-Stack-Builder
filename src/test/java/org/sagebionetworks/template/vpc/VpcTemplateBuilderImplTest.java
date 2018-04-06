package org.sagebionetworks.template.vpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.COLORS;
import static org.sagebionetworks.template.Constants.PARAMETER_PRIVATE_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PARAMETER_PUBLIC_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PARAMETER_VPC_NAME;
import static org.sagebionetworks.template.Constants.PARAMETER_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PARAMETER_VPN_CIDR;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_COLORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_PRIVATE_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_PUBLIC_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.*;

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
		colors = " Orange , Green ";
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
//		System.out.println(bodyCapture.getValue());
		JSONObject templateJson = new JSONObject(bodyCapture.getValue());
		
		JSONObject resouces = templateJson.getJSONObject("Resources");
		assertNotNull(resouces);
		assertTrue(resouces.has("VPC"));
		assertTrue(resouces.has("InternetGateway"));
		assertTrue(resouces.has("GatewayToInternet"));
		assertTrue(resouces.has("PublicRouteTable"));
		assertTrue(resouces.has("PublicNetworkAcl"));
		assertTrue(resouces.has("PublicRoute"));
		assertTrue(resouces.has("InboundHTTPPublicNetworkAclEntry"));
		assertTrue(resouces.has("OutboundPublicNetworkAclEntry"));
		assertTrue(resouces.has("ElasticIP"));
		assertTrue(resouces.has("PrivateRouteTable"));
		assertTrue(resouces.has("VpnSecurityGroup"));
		// color subnets
		// Orange
		assertTrue(resouces.has("OrangePublic1Subnet"));
		assertTrue(resouces.has("OrangePublic2Subnet"));
		assertTrue(resouces.has("OrangePrivate1Subnet"));
		assertTrue(resouces.has("OrangePrivate2Subnet"));
		assertTrue(resouces.has("OrangePublic1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("OrangePublic2SubnetRouteTableAssociation"));
		assertTrue(resouces.has("OrangePublic1SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("OrangePublic2SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("OrangePrivate1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("OrangePrivate2SubnetRouteTableAssociation"));
		// Green
		assertTrue(resouces.has("GreenPublic1Subnet"));
		assertTrue(resouces.has("GreenPublic2Subnet"));
		assertTrue(resouces.has("GreenPrivate1Subnet"));
		assertTrue(resouces.has("GreenPrivate2Subnet"));
		assertTrue(resouces.has("GreenPublic1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("GreenPublic2SubnetRouteTableAssociation"));
		assertTrue(resouces.has("GreenPublic1SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("GreenPublic2SubnetNetworkAclAssociation"));
		assertTrue(resouces.has("GreenPrivate1SubnetRouteTableAssociation"));
		assertTrue(resouces.has("GreenPrivate2SubnetRouteTableAssociation"));
	}
	
	@Test
	public void testCreateParameters() {
		String stackName = "stackName";
		// call under test
		Parameter[] parameters = builder.createParameters(stackName);
		assertNotNull(parameters);
		assertEquals(5, parameters.length);
		// keys
		assertEquals(PARAMETER_VPC_NAME,parameters[0].getParameterKey());
		assertEquals(PARAMETER_VPC_SUBNET_PREFIX,parameters[1].getParameterKey());
		assertEquals(PARAMETER_PRIVATE_SUBNET_ZONES,parameters[2].getParameterKey());
		assertEquals(PARAMETER_PUBLIC_SUBNET_ZONES,parameters[3].getParameterKey());
		assertEquals(PARAMETER_VPN_CIDR,parameters[4].getParameterKey());
		// values
		assertEquals(stackName, parameters[0].getParameterValue());
		assertEquals(subnetPrefix, parameters[1].getParameterValue());
		assertEquals(privateZones, parameters[2].getParameterValue());
		assertEquals(publicZones, parameters[3].getParameterValue());
		assertEquals(vpnCider, parameters[4].getParameterValue());
	}
	
	@Test
	public void testCreateContext() {
		// call under test
		VelocityContext context = builder.createContext();
		assertNotNull(context);
		String[] colors = (String[]) context.get(COLORS);;
		assertEquals(2, colors.length);
		assertEquals("Orange", colors[0]);
		assertEquals("Green", colors[1]);
	}
}
