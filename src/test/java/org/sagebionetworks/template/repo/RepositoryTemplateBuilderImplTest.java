package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.DATABASE_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.DB_ENDPOINT_SUFFIX;
import static org.sagebionetworks.template.Constants.ENVIRONMENT;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT;
import static org.sagebionetworks.template.Constants.PARAMETER_MYSQL_PASSWORD;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MAX_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MIN_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_SSL_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_VERSION;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OAUTH_ENDPOINT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_IOPS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_MAX_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_MULTI_AZ;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_STORAGE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ROUTE_53_HOSTED_ZONE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_INSTANCE_COUNT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_IOPS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_COLOR;
import static org.sagebionetworks.template.Constants.REPO_BEANSTALK_NUMBER;
import static org.sagebionetworks.template.Constants.SHARED_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.SHARED_RESOUCES_STACK_NAME;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.STACK_CMK_ALIAS;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.VPC_SUBNET_COLOR;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopy;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentDescriptor;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;
import org.sagebionetworks.template.repo.beanstalk.image.encrypt.ElasticBeanstalkDefaultAMIEncrypter;
import org.sagebionetworks.template.repo.beanstalk.image.encrypt.ElasticBeanstalkEncryptedPlatformInfo;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsVelocityContextProvider;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogDescriptor;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogType;
import org.sagebionetworks.template.vpc.Color;

import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryTemplateBuilderImplTest {

	@Mock
	CloudFormationClient mockCloudFormationClient;
	@Mock
	RepoConfiguration config;
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
	@Mock
	VelocityContextProvider mockContextProvider1;
	@Mock
	VelocityContextProvider mockContextProvider2;
	@Mock
	ElasticBeanstalkDefaultAMIEncrypter mockElasticBeanstalkDefaultAMIEncrypter;
	@Mock
	StackTagsProvider mockStackTagsProvider;
	@Mock
	CloudwatchLogsVelocityContextProvider mockCwlContextProvider;
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

	List<Tag> expectedTags;

	@Before
	public void before() throws InterruptedException {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

		expectedTags = new LinkedList<>();
		Tag t = new Tag().withKey("aKey").withValue("aValue");
		expectedTags.add(t);
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

		builder = new RepositoryTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, config, mockLoggerFactory,
				mockArtifactCopy, mockSecretBuilder, mockACLBuilder, Sets.newHashSet(mockContextProvider1, mockContextProvider2),
				mockElasticBeanstalkDefaultAMIEncrypter, mockStackTagsProvider, mockCwlContextProvider);

		stack = "dev";
		instance = "101";
		vpcSubnetColor = Color.Green.name();
		beanstalkNumber = "2";

		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);
		when(mockSecretBuilder.getRepositoryDatabasePassword()).thenReturn("somePassword");

		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE)).thenReturn(4);
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(8);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS)).thenReturn("db.t2.small");
		when(config.getBooleanProperty(PROPERTY_KEY_REPO_RDS_MULTI_AZ)).thenReturn(true);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.standard.name());
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_IOPS)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(2);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.io1.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);

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

		when(config.getProperty((PROPERTY_KEY_OAUTH_ENDPOINT))).thenReturn("https://oauthendpoint");

		sharedResouces = new Stack();
		Output dbOut = new Output();
		dbOut.withOutputKey(stack+instance+OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		databaseEndpointSuffix = "something.amazon.com";
		dbOut.withOutputValue(stack+"-"+instance+"-db."+databaseEndpointSuffix);
		// TableDB output
		Output tableDBOutput1 = new Output();
		tableDBOutput1.withOutputKey(stack+instance+"Table0"+OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		tableDBOutput1.withOutputValue(stack+"-"+instance+"-table-0."+databaseEndpointSuffix);
		Output tableDBOutput2 = new Output();
		tableDBOutput2.withOutputKey(stack+instance+"Table1"+OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		tableDBOutput2.withOutputValue(stack+"-"+instance+"-table-1."+databaseEndpointSuffix);

		sharedResouces.withOutputs(dbOut, tableDBOutput1, tableDBOutput2);
		
		secretsSouce = new SourceBundle("secretBucket", "secretKey");
		keyAlias = "alias/some/alias";
		
		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		when(mockSecretBuilder.getCMKAlias()).thenReturn(keyAlias);

		when(mockElasticBeanstalkDefaultAMIEncrypter.getEncryptedElasticBeanstalkAMI())
				.thenReturn(new ElasticBeanstalkEncryptedPlatformInfo("ami-123", "fake stack"));

		// CloudwatchLogs
		List<LogDescriptor> logDescriptors = this.generateLogDescriptors();
		when(mockCwlContextProvider.getLogDescriptors(any(EnvironmentType.class))).thenReturn(logDescriptors);
		
	}
	
	private void configureStack(String inputStack) throws InterruptedException {
		stack = inputStack;
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		sharedResouces = new Stack();
		Output dbOut = new Output();
		dbOut.withOutputKey(stack+instance+OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		databaseEndpointSuffix = "something.amazon.com";
		dbOut.withOutputValue(stack+"-"+instance+"-db."+databaseEndpointSuffix);
		sharedResouces.withOutputs(dbOut);
		
		when(mockCloudFormationClient.waitForStackToComplete(any(String.class))).thenReturn(sharedResouces);
	}

	@Test
	public void testBuildAndDeployProd() throws InterruptedException {
		stack = "prod";
		configureStack(stack);
		// call under test
		builder.buildAndDeploy();
		verify(mockCloudFormationClient, times(4)).createOrUpdateStack(requestCaptor.capture());
		List<CreateOrUpdateStackRequest> list = requestCaptor.getAllValues();
		CreateOrUpdateStackRequest request = list.get(0);
		assertEquals("prod-101-shared-resources", request.getStackName());
		assertEquals(expectedTags, request.getTags());
		assertEquals(true, request.getEnableTerminationProtection());
		assertNotNull(request.getParameters());
		String bodyJSONString = request.getTemplateBody();
		assertNotNull(bodyJSONString);
		System.out.println(bodyJSONString);
		JSONObject templateJson = new JSONObject(bodyJSONString);
		JSONObject resources = templateJson.getJSONObject("Resources");
		assertNotNull(resources);
		// database group
		validateResouceDatabaseSubnetGroup(resources, stack);
		// database instance
		validateResouceDatabaseInstance(resources, stack);
		// tables database
		validateResouceTablesDatabase(resources, stack);

		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES);
		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_WORKERS);
		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.PORTAL);

		List<String> evironmentNames = Lists.newArrayList("repo-prod-101-0", "workers-prod-101-0", "portal-prod-101-0");
		verify(mockACLBuilder).buildWebACL(evironmentNames);
		// prod should have alarms.
		assertTrue(resources.has("prod101Table1RepositoryDBAlarmSwapUsage"));
		assertTrue(resources.has("prod101Table1RepositoryDBAlarmSwapUsage"));
		assertTrue(resources.has("prod101Table1RepositoryDBHighWriteLatency"));
		assertTrue(resources.has("prod101Table1RepositoryDBHighCPUUtilization"));
		assertTrue(resources.has("prod101Table1RepositoryDBLowFreeStorageSpace"));
	}
	
	@Test
	public void testBuildAndDeployDev() throws InterruptedException {
		stack = "dev";
		configureStack(stack);
		// call under test
		builder.buildAndDeploy();
		verify(mockCloudFormationClient, times(4)).createOrUpdateStack(requestCaptor.capture());
		List<CreateOrUpdateStackRequest> list = requestCaptor.getAllValues();
		CreateOrUpdateStackRequest request = list.get(0);
		assertEquals("dev-101-shared-resources", request.getStackName());
		assertEquals(expectedTags, request.getTags());
		assertEquals(false, request.getEnableTerminationProtection());
		assertNotNull(request.getParameters());
		String bodyJSONString = request.getTemplateBody();
		assertNotNull(bodyJSONString);
		System.out.println(bodyJSONString);
		JSONObject templateJson = new JSONObject(bodyJSONString);
		JSONObject resources = templateJson.getJSONObject("Resources");
		assertNotNull(resources);

		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES);
		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_WORKERS);
		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.PORTAL);
		
		// dev should not have alarms
		assertFalse(resources.has("dev101Table1RepositoryDBAlarmSwapUsage"));
		assertFalse(resources.has("dev101Table1RepositoryDBAlarmSwapUsage"));
		assertFalse(resources.has("devd101Table1RepositoryDBHighWriteLatency"));
		assertFalse(resources.has("dev101Table1RepositoryDBHighCPUUtilization"));
		assertFalse(resources.has("dev101Table1RepositoryDBLowFreeStorageSpace"));
	}


	public void validateResouceDatabaseSubnetGroup(JSONObject resources, String stack) {
		JSONObject subnetGroup = resources.getJSONObject(stack+"101DBSubnetGroup");
		assertNotNull(subnetGroup);
		JSONObject properties = subnetGroup.getJSONObject("Properties");
		assertTrue(properties.has("SubnetIds"));
	}

	/**
	 * Validate for the repo AWS::RDS::DBInstance.
	 * 
	 * @param resources
	 */
	public void validateResouceDatabaseInstance(JSONObject resources, String stack) {
		JSONObject instance = resources.getJSONObject(stack+"101RepositoryDB");
		assertNotNull(instance);
		JSONObject properties = instance.getJSONObject("Properties");
		assertEquals("4", properties.get("AllocatedStorage"));
		assertEquals("8", properties.get("MaxAllocatedStorage"));
		assertEquals("db.t2.small", properties.get("DBInstanceClass"));
		assertEquals(Boolean.TRUE, properties.get("MultiAZ"));
	}

	/**
	 * Validate tables for AWS::RDS::DBInstance
	 * 
	 * @param resources
	 */
	public void validateResouceTablesDatabase(JSONObject resources, String stack) {
		// zero
		JSONObject instance = resources.getJSONObject(stack+"101Table0RepositoryDB");
		assertNotNull(instance);
		JSONObject properties = instance.getJSONObject("Properties");
		assertEquals("3", properties.get("AllocatedStorage"));
		assertEquals("6", properties.get("MaxAllocatedStorage"));
		assertEquals("db.t2.micro", properties.get("DBInstanceClass"));
		assertEquals(Boolean.FALSE, properties.get("MultiAZ"));
		assertEquals(stack+"-101-table-0", properties.get("DBInstanceIdentifier"));
		assertEquals(stack+"101", properties.get("DBName"));
		// one
		instance = resources.getJSONObject(stack+"101Table1RepositoryDB");
		assertNotNull(instance);
		properties = instance.getJSONObject("Properties");
		assertEquals("3", properties.get("AllocatedStorage"));
		assertEquals("6", properties.get("MaxAllocatedStorage"));
		assertEquals("db.t2.micro", properties.get("DBInstanceClass"));
		assertEquals(Boolean.FALSE, properties.get("MultiAZ"));
		assertEquals(stack+"-101-table-1", properties.get("DBInstanceIdentifier"));
		assertEquals(stack+"101", properties.get("DBName"));
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
		assertEquals("us-east-1-synapse-dev-vpc-2020", context.get(VPC_EXPORT_PREFIX));

		DatabaseDescriptor[] descriptors = (DatabaseDescriptor[]) context.get(DATABASE_DESCRIPTORS);
		assertNotNull(descriptors);
		assertEquals(3, descriptors.length);
		// repo database
		DatabaseDescriptor desc = descriptors[0];
		assertEquals(4, desc.getAllocatedStorage());
		assertEquals(8, desc.getMaxAllocatedStorage());
		assertEquals("dev101", desc.getDbName());
		assertEquals("db.t2.small", desc.getInstanceClass());
		assertEquals("dev-101-db", desc.getInstanceIdentifier());
		assertEquals("dev101RepositoryDB", desc.getResourceName());
		assertEquals(DatabaseStorageType.standard.name(), desc.getDbStorageType());
		assertEquals(-1, desc.getDbIops());

		// table zero
		desc = descriptors[1];
		assertEquals(3, desc.getAllocatedStorage());
		assertEquals(6, desc.getMaxAllocatedStorage());
		assertEquals("dev101", desc.getDbName());
		assertEquals("db.t2.micro", desc.getInstanceClass());
		assertEquals("dev-101-table-0", desc.getInstanceIdentifier());
		assertEquals("dev101Table0RepositoryDB", desc.getResourceName());
		assertEquals(DatabaseStorageType.io1.name(), desc.getDbStorageType());
		assertEquals(1000, desc.getDbIops());
		// table one
		desc = descriptors[2];
		assertEquals(3, desc.getAllocatedStorage());
		assertEquals(6, desc.getMaxAllocatedStorage());
		assertEquals("dev101", desc.getDbName());
		assertEquals("db.t2.micro", desc.getInstanceClass());
		assertEquals("dev-101-table-1", desc.getInstanceIdentifier());
		assertEquals("dev101Table1RepositoryDB", desc.getResourceName());
		assertEquals(DatabaseStorageType.io1.name(), desc.getDbStorageType());
		assertEquals(1000, desc.getDbIops());

		verify(mockContextProvider1).addToContext(context);
		verify(mockContextProvider2).addToContext(context);
	}

	@Test
	public void testCreateEnvironments() {
		// call under test
		List<EnvironmentDescriptor> descriptors = builder.createEnvironments(secretsSouce);
		assertNotNull(descriptors);
		assertEquals(3, descriptors.size());
		// repo
		EnvironmentDescriptor desc = descriptors.get(0);
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
		desc = descriptors.get(1);
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
		desc = descriptors.get(2);
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
	public void testCreateEnvironments__missingPropertiesForEnvironment(){
		//do not include the "workers" environment by making the config throw an exception
		when(config.getProperty(PROPERTY_KEY_BEANSTALK_VERSION + EnvironmentType.REPOSITORY_WORKERS.getShortName()))
				.thenThrow(new ConfigurationPropertyNotFound("test.key"));

		List<EnvironmentDescriptor> descriptors = builder.createEnvironments(secretsSouce);

		verify(mockLogger).warn(anyString());
		assertEquals(2, descriptors.size());
		Set<String> createEnvironmentTypes = descriptors.stream().map(EnvironmentDescriptor::getType).collect(Collectors.toSet());
		assertEquals(Sets.newHashSet("repo", "portal"), createEnvironmentTypes);
	}

	@Test
	public void testCreateEnvironmentContext() {
		EnvironmentDescriptor environment = new EnvironmentDescriptor().withType(EnvironmentType.REPOSITORY_SERVICES);

		// call under test
		VelocityContext context = builder.createEnvironmentContext(sharedResouces, environment);

		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("us-east-1-synapse-dev-vpc-2020", context.get(VPC_EXPORT_PREFIX));
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

	private List<LogDescriptor> generateLogDescriptors() {
		List<LogDescriptor> descriptors = new LinkedList<>();
		for (LogType t: LogType.values()) {
			LogDescriptor d = new LogDescriptor();
			d.setLogType(t);
			d.setLogPath("/var/log/mypath.log");
			d.setDateFormat("YYYY-MM-DD");
			descriptors.add(d);
		}
		return descriptors;
	}
		
}
