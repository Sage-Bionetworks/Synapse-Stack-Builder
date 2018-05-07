package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.CONFIGURATION_URL;
import static org.sagebionetworks.template.Constants.DATABASE_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.PARAMETER_MYSQL_PASSWORD;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_AWS_ACCESS_KEY_ID;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_AWS_SECRET_KEY;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_ENCRYPTION_KEY;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MAX_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MIN_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_VERSION;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_MYSQL_PASSWORD;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_MULTI_AZ;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_INSTANCE_COUNT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_COLOR;
import static org.sagebionetworks.template.Constants.SHARED_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.SHARED_RESOUCES_STACK_NAME;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.VPC_SUBNET_COLOR;

import java.util.List;

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
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopy;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentConfiguration;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentDescriptor;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;
import org.sagebionetworks.template.vpc.Color;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryTemplateBuilderImplTest {

	@Mock
	CloudFormationClient mockCloudFormationClient;
	@Mock
	Configuration config;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;
	@Mock
	ArtifactCopy mockArtifactCopy;
	@Mock
	EnvironmentConfiguration mockEnvironConfig;
	@Captor
	ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;

	VelocityEngine velocityEngine;
	RepositoryTemplateBuilderImpl builder;

	String stack;
	String instance;
	String vpcSubnetColor;
	String beanstalkNumber;

	String bucket;
	Stack sharedStackResults;
	String configUrl;

	@Before
	public void before() throws InterruptedException {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

		builder = new RepositoryTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, config, mockLoggerFactory,
				mockArtifactCopy, mockEnvironConfig);

		stack = "dev";
		instance = "101";
		vpcSubnetColor = Color.Green.name();
		beanstalkNumber = "2";

		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);
		when(config.getProperty(PROPERTY_KEY_MYSQL_PASSWORD)).thenReturn("somePassword");

		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE)).thenReturn(4);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS)).thenReturn("db.t2.small");
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS)).thenReturn("db.t2.small");
		when(config.getBooleanProperty(PROPERTY_KEY_REPO_RDS_MULTI_AZ)).thenReturn(true);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(2);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");

		for (EnvironmentType type : EnvironmentType.values()) {
			String version = "version-" + type.getShortName();
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_VERSION + type.getShortName())).thenReturn(version);
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL + type.getShortName()))
					.thenReturn("url-" + type.getShortName());
			when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MIN_INSTANCES + type.getShortName())).thenReturn(1);
			when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MAX_INSTANCES + type.getShortName())).thenReturn(2);
			SourceBundle bundle = new SourceBundle("bucket", "key-" + type.getShortName());
			when(mockArtifactCopy.copyArtifactIfNeeded(type, version)).thenReturn(bundle);
		}

		when(config.getProperty(PROPERTY_KEY_AWS_ACCESS_KEY_ID)).thenReturn("aws-key");
		when(config.getProperty(PROPERTY_KEY_AWS_SECRET_KEY)).thenReturn("aws-secret");
		when(config.getProperty(PROPERTY_KEY_BEANSTALK_ENCRYPTION_KEY)).thenReturn("encryption-key");

		sharedStackResults = new Stack();
		when(mockCloudFormationClient.waitForStackToComplete(any(String.class))).thenReturn(sharedStackResults);
		configUrl = "http://amazon.com/bucket/key";
		when(mockEnvironConfig.createEnvironmentConfiguration(sharedStackResults)).thenReturn(configUrl);
	}

	@Test
	public void testBuildAndDeploy() throws InterruptedException {
		// call under test
		builder.buildAndDeploy();
		verify(mockCloudFormationClient, times(4)).createOrUpdateStack(requestCaptor.capture());
		List<CreateOrUpdateStackRequest> list = requestCaptor.getAllValues();
		CreateOrUpdateStackRequest request = list.get(0);
		assertEquals("dev-101-shared-resources", request.getStackName());
		assertNotNull(request.getParameters());
		String bodyJSONString = request.getTemplateBody();
		assertNotNull(bodyJSONString);
		System.out.println(bodyJSONString);
		JSONObject templateJson = new JSONObject(bodyJSONString);
		JSONObject resources = templateJson.getJSONObject("Resources");
		assertNotNull(resources);
		// database group
		validateResouceDatabaseSubnetGroup(resources);
		// database instance
		validateResouceDatabaseInstance(resources);
		// tables database
		validateResouceTablesDatabase(resources);
	}

	public void validateResouceDatabaseSubnetGroup(JSONObject resources) {
		JSONObject subnetGroup = resources.getJSONObject("dev101DBSubnetGroup");
		assertNotNull(subnetGroup);
		JSONObject properties = subnetGroup.getJSONObject("Properties");
		assertTrue(properties.has("SubnetIds"));
	}

	/**
	 * Validate for the repo AWS::RDS::DBInstance.
	 * 
	 * @param resources
	 */
	public void validateResouceDatabaseInstance(JSONObject resources) {
		JSONObject instance = resources.getJSONObject("dev101RepositoryDB");
		assertNotNull(instance);
		JSONObject properties = instance.getJSONObject("Properties");
		assertEquals("4", properties.get("AllocatedStorage"));
		assertEquals("db.t2.small", properties.get("DBInstanceClass"));
		assertEquals(Boolean.TRUE, properties.get("MultiAZ"));
	}

	/**
	 * Validate tables for AWS::RDS::DBInstance
	 * 
	 * @param resources
	 */
	public void validateResouceTablesDatabase(JSONObject resources) {
		// zero
		JSONObject instance = resources.getJSONObject("dev101Table0RepositoryDB");
		assertNotNull(instance);
		JSONObject properties = instance.getJSONObject("Properties");
		assertEquals("3", properties.get("AllocatedStorage"));
		assertEquals("db.t2.micro", properties.get("DBInstanceClass"));
		assertEquals(Boolean.FALSE, properties.get("MultiAZ"));
		assertEquals("dev-101-table-0", properties.get("DBInstanceIdentifier"));
		assertEquals("dev101", properties.get("DBName"));
		// one
		instance = resources.getJSONObject("dev101Table1RepositoryDB");
		assertNotNull(instance);
		properties = instance.getJSONObject("Properties");
		assertEquals("3", properties.get("AllocatedStorage"));
		assertEquals("db.t2.micro", properties.get("DBInstanceClass"));
		assertEquals(Boolean.FALSE, properties.get("MultiAZ"));
		assertEquals("dev-101-table-1", properties.get("DBInstanceIdentifier"));
		assertEquals("dev101", properties.get("DBName"));
	}

	@Test
	public void testGetParamters() {
		// call under test
		Parameter[] params = builder.createSharedParameters();
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
		VelocityContext context = builder.createSharedContext();
		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("dev-101-shared-resources", context.get(SHARED_RESOUCES_STACK_NAME));
		assertEquals("us-east-1-synapse-dev-vpc", context.get(VPC_EXPORT_PREFIX));

		DatabaseDescriptor[] descriptors = (DatabaseDescriptor[]) context.get(DATABASE_DESCRIPTORS);
		assertNotNull(descriptors);
		assertEquals(3, descriptors.length);
		// repo database
		DatabaseDescriptor desc = descriptors[0];
		assertEquals(4, desc.getAllocatedStorage());
		assertEquals("dev101", desc.getDbName());
		assertEquals("db.t2.small", desc.getInstanceClass());
		assertEquals("dev-101-db", desc.getInstanceIdentifier());
		assertEquals("dev101RepositoryDB", desc.getResourceName());

		// table zero
		desc = descriptors[1];
		assertEquals(3, desc.getAllocatedStorage());
		assertEquals("dev101", desc.getDbName());
		assertEquals("db.t2.micro", desc.getInstanceClass());
		assertEquals("dev-101-table-0", desc.getInstanceIdentifier());
		assertEquals("dev101Table0RepositoryDB", desc.getResourceName());
		// table one
		desc = descriptors[2];
		assertEquals(3, desc.getAllocatedStorage());
		assertEquals("dev101", desc.getDbName());
		assertEquals("db.t2.micro", desc.getInstanceClass());
		assertEquals("dev-101-table-1", desc.getInstanceIdentifier());
		assertEquals("dev101Table1RepositoryDB", desc.getResourceName());
	}

	@Test
	public void testCreateEnvironments() {
		// call under test
		EnvironmentDescriptor[] descriptors = builder.createEnvironments();
		assertNotNull(descriptors);
		assertEquals(3, descriptors.length);
		// repo
		EnvironmentDescriptor desc = descriptors[0];
		assertEquals("repo", desc.getType());
		assertEquals("repo-dev-101-0", desc.getName());
		assertEquals("RepoDev1010", desc.getRefName());
		assertEquals("url-repo", desc.getHealthCheckUrl());
		assertEquals(1, desc.getMinInstances());
		assertEquals(2, desc.getMaxInstances());
		SourceBundle bundle = desc.getSourceBundle();
		assertNotNull(bundle);
		assertEquals("bucket", bundle.getBucket());
		assertEquals("key-repo", bundle.getKey());

		// workers
		desc = descriptors[1];
		assertEquals("workers", desc.getType());
		assertEquals("workers-dev-101-0", desc.getName());
		assertEquals("WorkersDev1010", desc.getRefName());
		assertEquals("url-workers", desc.getHealthCheckUrl());
		assertEquals(1, desc.getMinInstances());
		assertEquals(2, desc.getMaxInstances());
		bundle = desc.getSourceBundle();
		assertNotNull(bundle);
		assertEquals("bucket", bundle.getBucket());
		assertEquals("key-workers", bundle.getKey());

		// portal
		desc = descriptors[2];
		assertEquals("portal", desc.getType());
		assertEquals("portal-dev-101-0", desc.getName());
		assertEquals("PortalDev1010", desc.getRefName());
		assertEquals("url-portal", desc.getHealthCheckUrl());
		assertEquals(1, desc.getMinInstances());
		assertEquals(2, desc.getMaxInstances());
		bundle = desc.getSourceBundle();
		assertNotNull(bundle);
		assertEquals("bucket", bundle.getBucket());
		assertEquals("key-portal", bundle.getKey());
	}

	@Test
	public void testCreateEnvironmentContext() {
		// call under test
		VelocityContext context = builder.createEnvironmentContext(sharedStackResults);
		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("us-east-1-synapse-dev-vpc", context.get(VPC_EXPORT_PREFIX));
		assertEquals("us-east-1-dev-101-shared-resources", context.get(SHARED_EXPORT_PREFIX));
		assertEquals("http://amazon.com/bucket/key", context.get(CONFIGURATION_URL));
	}

	@Test
	public void testCreateEnvironmentParameters() {
		Parameter[] params = builder.createEnvironmentParameters();
		assertNotNull(params);
		assertEquals(3, params.length);

		Parameter param = params[0];
		assertEquals("AwsKey", param.getParameterKey());
		assertEquals("aws-key", param.getParameterValue());
		param = params[1];
		assertEquals("AwsSecret", param.getParameterKey());
		assertEquals("aws-secret", param.getParameterValue());
		param = params[2];
		assertEquals("EncryptionKey", param.getParameterKey());
		assertEquals("encryption-key", param.getParameterValue());
	}
}
