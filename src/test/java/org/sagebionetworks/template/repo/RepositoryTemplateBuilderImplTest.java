package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_COLOR;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONArray;
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

@RunWith(MockitoJUnitRunner.class)
public class RepositoryTemplateBuilderImplTest {

	@Mock
	CloudFormationClient mockCloudFormationClient;
	@Mock
	PropertyProvider mockPropertyProvider;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;

	VelocityEngine velocityEngine;
	RepositoryTemplateBuilderImpl builder;
	
	String stack;
	String instance;
	String vpcSubnetColor;
	String beanstalkNumber;

	@Before
	public void before() {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		
		builder = new RepositoryTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, mockPropertyProvider, mockLoggerFactory);
		
		stack = "dev";
		instance = "101";
		vpcSubnetColor = "Pink";
		beanstalkNumber = "2";
		
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);
	}
	
	@Test
	public void testBuildAndDeploy() {
		// call under test
		builder.buildAndDeploy();
		ArgumentCaptor<String> nameCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> bodyCapture = ArgumentCaptor.forClass(String.class);
		verify(mockCloudFormationClient).createOrUpdateStack(nameCapture.capture(), bodyCapture.capture());
		assertEquals("dev101SharedResources", nameCapture.getValue());
		String bodyJSONString = bodyCapture.getValue();
		assertNotNull(bodyJSONString);
		System.out.println(bodyJSONString);
		JSONObject templateJson = new JSONObject(bodyJSONString);
		JSONObject resources = templateJson.getJSONObject("Resources");
		assertNotNull(resources);
		validateResouceDatabaseSubnetGroup(resources);
	}
	

	public void validateResouceDatabaseSubnetGroup(JSONObject resources) {
		JSONObject subnetGroup = resources.getJSONObject("dev101DBSubnetGroup");
		assertNotNull(subnetGroup);
		JSONObject properties = subnetGroup.getJSONObject("Properties");
		JSONArray subnetArray = properties.getJSONArray("SubnetIds");
		JSONObject subnetOne = subnetArray.getJSONObject(0);
		assertEquals("us-east-1-synapse-stack-vpc-PinkPrivate1Subnet", subnetOne.getString("Fn::ImportValue"));
		JSONObject subnetTwo = subnetArray.getJSONObject(1);
		assertEquals("us-east-1-synapse-stack-vpc-PinkPrivate2Subnet", subnetTwo.getString("Fn::ImportValue"));
	}
}
