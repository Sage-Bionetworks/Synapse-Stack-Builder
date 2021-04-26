package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.*;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import org.apache.logging.log4j.Logger;
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
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;

import com.amazonaws.services.cloudformation.model.Parameter;

@RunWith(MockitoJUnitRunner.class)
public class IdGeneratorBuilderImplTest {

	@Mock
	CloudFormationClient mockCloudFormationClient;
	@Mock
	Configuration config;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;
	@Mock
	SecretBuilder mockSecretBuilder;

	VelocityEngine velocityEngine;
	IdGeneratorBuilderImpl builder;

	@Captor
	ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;

	@Before
	public void before() {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn("Green");
		when(mockSecretBuilder.getIdGeneratorPassword()).thenReturn("somePassword");
		when(config.getProperty(PROPERTY_KEY_ID_GENERATOR_HOSTED_ZONE_ID)).thenReturn("hostedZoneId");
		when(config.getProperty(PROPERTY_KEY_OLD_VPC_CIDR)).thenReturn("1.2.3.4/16");

		builder = new IdGeneratorBuilderImpl(mockCloudFormationClient, velocityEngine, config, mockLoggerFactory, mockSecretBuilder);
	}

	@Test
	public void testBuildProd() {
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn("Green");
		when(mockSecretBuilder.getIdGeneratorPassword()).thenReturn("somePassword");
		builder = new IdGeneratorBuilderImpl(mockCloudFormationClient, velocityEngine, config, mockLoggerFactory, mockSecretBuilder);

		// call under test
		builder.buildAndDeploy();
		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertEquals("prod-id-generator-green", request.getStackName());
		JSONObject template = new JSONObject(request.getTemplateBody());
		JSONObject resources = template.getJSONObject("Resources");
		assertTrue(resources.has("prodIdGeneratorDBSubnetGroup"));
		assertTrue(resources.has("prodIdGeneratorDBSecurityGroup"));
		assertTrue(resources.has("prodIdGeneratorDBParameterGroup"));
		assertTrue(resources.has("prodIdGenerator"));
		assertTrue(resources.has("prodIdGeneratorAlarmSwapUsage"));
		assertTrue(resources.has("prodIdGeneratorHighWriteLatency"));
		assertTrue(resources.has("prodIdGeneratorHighCPUUtilization"));
		assertTrue(resources.has("prodIdGeneratorLowFreeStorageSpace"));
		Parameter[] params = request.getParameters();
		assertNotNull(params);
		Parameter param = params[0];
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.getParameterKey());
		assertEquals("somePassword", param.getParameterValue());
		assertEquals(1, params.length);
	}
	
	/**
	 * Alarms should be be set for a dev stack (PLFM-5768).
	 */
	@Test
	public void testBuildDev() {
		// call under test
		builder.buildAndDeploy();
		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertEquals("dev-id-generator-green", request.getStackName());
		JSONObject template = new JSONObject(request.getTemplateBody());
		JSONObject resources = template.getJSONObject("Resources");
		assertTrue(resources.has("devIdGeneratorDBSubnetGroup"));
		assertTrue(resources.has("devIdGeneratorDBSecurityGroup"));
		assertTrue(resources.has("devIdGeneratorDBParameterGroup"));
		assertTrue(resources.has("devIdGenerator"));
		assertFalse(resources.has("devIdGeneratorAlarmSwapUsage"));
		assertFalse(resources.has("devIdGeneratorHighWriteLatency"));
		assertFalse(resources.has("devIdGeneratorHighCPUUtilization"));
		assertFalse(resources.has("devIdGeneratorLowFreeStorageSpace"));
		Parameter[] params = request.getParameters();
		assertNotNull(params);
		Parameter param = params[0];
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.getParameterKey());
		assertEquals("somePassword", param.getParameterValue());
		assertEquals(1, params.length);
	}

}
