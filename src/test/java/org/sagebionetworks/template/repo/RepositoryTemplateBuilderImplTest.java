package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.*;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
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
import org.sagebionetworks.template.vpc.Color;

import com.amazonaws.services.cloudformation.model.Parameter;

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

		builder = new RepositoryTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, mockPropertyProvider,
				mockLoggerFactory);

		stack = "dev";
		instance = "101";
		vpcSubnetColor = Color.Green.name();
		beanstalkNumber = "2";

		when(mockPropertyProvider.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_MYSQL_PASSWORD)).thenReturn("somePassword");
	}

	@Test
	public void testBuildAndDeploy() {
		// call under test
		builder.buildAndDeploy();
		ArgumentCaptor<String> nameCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> bodyCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Parameter[]> parameterCatpure = ArgumentCaptor.forClass(Parameter[].class);
		verify(mockCloudFormationClient).createOrUpdateStack(nameCapture.capture(), bodyCapture.capture(),
				parameterCatpure.capture());
		assertEquals("dev-101-shared-resources", nameCapture.getValue());
		assertNotNull(parameterCatpure.getValue());
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
		assertEquals("us-east-1-synapse-dev-vpc-GreenPrivate0Subnet-ID", subnetOne.getString("Fn::ImportValue"));
		JSONObject subnetTwo = subnetArray.getJSONObject(1);
		assertEquals("us-east-1-synapse-dev-vpc-GreenPrivate1Subnet-ID", subnetTwo.getString("Fn::ImportValue"));
	}

	@Test
	public void testGetParamters() {
		// call under test
		Parameter[] params = builder.createParameters();
		assertNotNull(params);
		assertEquals(1, params.length);
		Parameter param = params[0];
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.getParameterKey());
		assertEquals("somePassword", param.getParameterValue());
	}

	@Test
	public void testcreateSharedResourcesStackName() {
		// call under test
		String name = builder.createSharedResourcesStackName();
		assertEquals("dev-101-shared-resources", name);
	}

	@Test
	public void testCreateVpcExportPrefix() {
		// call under test
		String vpcPrefix = builder.createVpcExportPrefix();
		assertEquals("us-east-1-synapse-dev-vpc", vpcPrefix);
	}

	@Test
	public void testCreateContext() {
		// call under test
		VelocityContext context = builder.createContext();
		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("dev-101-shared-resources", context.get(SHARED_RESOUCES_STACK_NAME));
		assertEquals("us-east-1-synapse-dev-vpc", context.get(VPC_EXPORT_PREFIX));
	}
}
