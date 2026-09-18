package org.sagebionetworks.template.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.ID_GENERATOR_DB_INSTANCE_CLASS;
import static org.sagebionetworks.template.Constants.PARAMETER_MYSQL_PASSWORD;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ID_GENERATOR_HOSTED_ZONE_ID;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_COLOR;
import static org.sagebionetworks.template.Constants.RDS_ENGINE;
import static org.sagebionetworks.template.Constants.RDS_ENGINE_VERSION;
import static org.sagebionetworks.template.Constants.RDS_MINIMUM_SUBNET_COUNT;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONArray;
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
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.RdsClientWrapper;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;
import software.amazon.awssdk.services.cloudformation.model.Parameter;

@ExtendWith(MockitoExtension.class)
public class IdGeneratorBuilderImplTest {

	@Mock
    CloudFormationClientWrapper mockCloudFormationClientWrapper;
	@Mock
	Configuration config;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;
	@Mock
	SecretBuilder mockSecretBuilder;
	@Mock
	RdsClientWrapper mockRdsClientWrapper;

	VelocityEngine velocityEngine;
	IdGeneratorBuilderImpl builder;

	List<String> availableSubnets;

	@Captor
	ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;

	@BeforeEach
	public void before() {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn("Green");
		when(mockSecretBuilder.getIdGeneratorPassword()).thenReturn("somePassword");
		when(config.getProperty(PROPERTY_KEY_ID_GENERATOR_HOSTED_ZONE_ID)).thenReturn("hostedZoneId");

		availableSubnets = List.of("subnet-a", "subnet-c");
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn("subnet-a, subnet-b, subnet-c");
		when(mockRdsClientWrapper.getAvailableSubnetsForDBInstanceClasses(any(), any(), any(), any(), anyInt()))
				.thenReturn(availableSubnets);

		builder = new IdGeneratorBuilderImpl(mockCloudFormationClientWrapper, velocityEngine, config, mockLoggerFactory,
				mockSecretBuilder, mockRdsClientWrapper);
	}

	@Test
	public void testBuildProd() {
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn("Green");
		when(mockSecretBuilder.getIdGeneratorPassword()).thenReturn("somePassword");
		builder = new IdGeneratorBuilderImpl(mockCloudFormationClientWrapper, velocityEngine, config, mockLoggerFactory,
				mockSecretBuilder, mockRdsClientWrapper);

		// call under test
		builder.buildAndDeploy();
		verify(mockCloudFormationClientWrapper).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertEquals("prod-id-generator-4-green", request.getStackName());
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
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.parameterKey());
		assertEquals("somePassword", param.parameterValue());
		assertEquals(1, params.length);
	}
	
	/**
	 * Alarms should be be set for a dev stack (PLFM-5768).
	 */
	@Test
	public void testBuildDev() {
		// call under test
		builder.buildAndDeploy();
		verify(mockCloudFormationClientWrapper).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertEquals("dev-id-generator-4-green", request.getStackName());
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
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.parameterKey());
		assertEquals("somePassword", param.parameterValue());
		assertEquals(1, params.length);
	}

	/**
	 * The subnet group must only contain the zones that can host the instance class, else RDS can put
	 * the Multi-AZ standby somewhere unsupported and silently give up (PLFM-9965).
	 */
	@Test
	public void testBuildSubnetGroupIsLimitedToAvailableSubnets() {
		// call under test
		builder.buildAndDeploy();

		verify(mockRdsClientWrapper).getAvailableSubnetsForDBInstanceClasses(RDS_ENGINE, RDS_ENGINE_VERSION,
				List.of(ID_GENERATOR_DB_INSTANCE_CLASS), List.of("subnet-a", "subnet-b", "subnet-c"),
				RDS_MINIMUM_SUBNET_COUNT);

		verify(mockCloudFormationClientWrapper).createOrUpdateStack(requestCaptor.capture());
		JSONObject resources = new JSONObject(requestCaptor.getValue().getTemplateBody())
				.getJSONObject("Resources");
		JSONArray subnetIds = resources.getJSONObject("devIdGeneratorDBSubnetGroup").getJSONObject("Properties")
				.getJSONArray("SubnetIds");
		assertEquals(availableSubnets, subnetIds.toList());
		// the database must still be Multi-AZ with the class the zones were resolved for
		JSONObject database = resources.getJSONObject("devIdGenerator").getJSONObject("Properties");
		assertTrue(database.getBoolean("MultiAZ"));
		assertEquals(ID_GENERATOR_DB_INSTANCE_CLASS, database.getString("DBInstanceClass"));
	}

	/**
	 * The engine and version used to resolve the zones must match what the template deploys, or the
	 * wrong set of zones is used.
	 */
	@Test
	public void testBuildEngineMatchesConstants() {
		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClientWrapper).createOrUpdateStack(requestCaptor.capture());
		JSONObject database = new JSONObject(requestCaptor.getValue().getTemplateBody()).getJSONObject("Resources")
				.getJSONObject("devIdGenerator").getJSONObject("Properties");
		assertEquals(RDS_ENGINE, database.getString("Engine").toLowerCase());
		assertEquals(RDS_ENGINE_VERSION, database.getString("EngineVersion"));
	}

}
