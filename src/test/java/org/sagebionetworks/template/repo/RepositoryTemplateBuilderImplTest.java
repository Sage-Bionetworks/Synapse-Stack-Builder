package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.DATABASE_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.DB_ENDPOINT_SUFFIX;
import static org.sagebionetworks.template.Constants.ENVIRONMENT;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT;
import static org.sagebionetworks.template.Constants.PARAMETER_MYSQL_PASSWORD;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_AWS_ACCESS_KEY_ID;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_AWS_SECRET_KEY;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_ENCRYPTION_KEY;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MAX_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MIN_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_SSL_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_VERSION;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_MULTI_AZ;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ROUTE_53_HOSTED_ZONE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_INSTANCE_COUNT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_COLOR;
import static org.sagebionetworks.template.Constants.REPO_BEANSTALK_NUMBER;
import static org.sagebionetworks.template.Constants.SHARED_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.SHARED_RESOUCES_STACK_NAME;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.STACK_CMK_ALIAS;
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
import org.sagebionetworks.template.repo.beanstalk.EnvironmentDescriptor;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;
import org.sagebionetworks.template.vpc.Color;

import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.google.common.collect.Lists;

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
	SecretBuilder mockSecretBuilder;
	@Mock
	WebACLBuilder mockACLBuilder;
	@Captor
	ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;

	VelocityEngine velocityEngine;
	RepositoryTemplateBuilderImpl builder;

	String stack;
	String instance;
	String vpcSubnetColor;
	String beanstalkNumber;

	String bucket;

	Stack sharedResouces;
	String databaseEndpointSuffix;
	
	SourceBundle secretsSouce;
	String keyAlias;

	@Before
	public void before() throws InterruptedException {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

		builder = new RepositoryTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, config, mockLoggerFactory,
				mockArtifactCopy, mockSecretBuilder, mockACLBuilder);

		stack = "dev";
		instance = "101";
		vpcSubnetColor = Color.Green.name();
		beanstalkNumber = "2";

		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);
		when(mockSecretBuilder.getRepositoryDatabasePassword()).thenReturn("somePassword");

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
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN+ type.getShortName())).thenReturn("the:ssl:arn");
			when(config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE+ type.getShortName())).thenReturn("synapes.org");
			
			SourceBundle bundle = new SourceBundle("bucket", "key-" + type.getShortName());
			when(mockArtifactCopy.copyArtifactIfNeeded(type, version)).thenReturn(bundle);
		}

		when(config.getProperty(PROPERTY_KEY_AWS_ACCESS_KEY_ID)).thenReturn("aws-key");
		when(config.getProperty(PROPERTY_KEY_AWS_SECRET_KEY)).thenReturn("aws-secret");
		when(config.getProperty(PROPERTY_KEY_BEANSTALK_ENCRYPTION_KEY)).thenReturn("encryption-key");

		sharedResouces = new Stack();
		Output dbOut = new Output();
		dbOut.withOutputKey(stack+instance+OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		databaseEndpointSuffix = "something.amazon.com";
		dbOut.withOutputValue(stack+"-"+instance+"-db."+databaseEndpointSuffix);
		sharedResouces.withOutputs(dbOut);
		
		when(mockCloudFormationClient.waitForStackToComplete(any(String.class))).thenReturn(sharedResouces);
		
		secretsSouce = new SourceBundle("secretBucket", "secretKey");
		keyAlias = "alias/some/alias";
		
		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		when(mockSecretBuilder.getCMKAlias()).thenReturn(keyAlias);
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
		List<String> evironmentNames = Lists.newArrayList("repo-dev-101-0", "workers-dev-101-0", "portal-dev-101-0");
		verify(mockACLBuilder).buildWebACL(evironmentNames);
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
		EnvironmentDescriptor[] descriptors = builder.createEnvironments(secretsSouce);
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
		assertEquals("version-repo", desc.getVersionLabel());
		SourceBundle bundle = desc.getSourceBundle();
		assertNotNull(bundle);
		assertEquals("bucket", bundle.getBucket());
		assertEquals("key-repo", bundle.getKey());
		assertEquals("synapes.org", desc.getHostedZone());
		assertEquals("repo-dev-101-0-synapes-org", desc.getCnamePrefix());
		assertEquals("the:ssl:arn", desc.getSslCertificateARN());
		assertEquals("SynapesRepoWorkersInstanceProfile", desc.getInstanceProfileSuffix());
		// secrets should be passed to reop
		assertEquals(secretsSouce, desc.getSecretsSource());

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
		assertEquals("SynapesRepoWorkersInstanceProfile", desc.getInstanceProfileSuffix());
		// secrets should be passed to workers
		assertEquals(secretsSouce, desc.getSecretsSource());

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
		assertEquals("SynapesPortalInstanceProfile", desc.getInstanceProfileSuffix());
		// empty secrets should be passed to portal
		assertEquals(null, desc.getSecretsSource());
	}

	@Test
	public void testCreateEnvironmentContext() {
		EnvironmentDescriptor environment = new EnvironmentDescriptor();
		// call under test
		VelocityContext context = builder.createEnvironmentContext(sharedResouces, environment);
		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("us-east-1-synapse-dev-vpc", context.get(VPC_EXPORT_PREFIX));
		assertEquals("us-east-1-dev-101-shared-resources", context.get(SHARED_EXPORT_PREFIX));
		assertEquals(environment, context.get(ENVIRONMENT));
		assertEquals(0, context.get(REPO_BEANSTALK_NUMBER));
		assertEquals(keyAlias, context.get(STACK_CMK_ALIAS));
		assertEquals(databaseEndpointSuffix, context.get(DB_ENDPOINT_SUFFIX));
	}
	
	@Test
	public void testExtractDatabaseSuffix() {
		// call under test
		String suffix = builder.extractDatabaseSuffix(sharedResouces);
		assertEquals(databaseEndpointSuffix, suffix);
	}
}
