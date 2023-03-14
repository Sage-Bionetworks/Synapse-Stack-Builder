package org.sagebionetworks.template.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.ADMIN_RULE_ACTION;
import static org.sagebionetworks.template.Constants.BEANSTALK_INSTANCES_SUBNETS;
import static org.sagebionetworks.template.Constants.DATABASE_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.DB_ENDPOINT_SUFFIX;
import static org.sagebionetworks.template.Constants.DELETION_POLICY;
import static org.sagebionetworks.template.Constants.EC2_INSTANCE_TYPE;
import static org.sagebionetworks.template.Constants.ENVIRONMENT;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.NOSNAPSHOT;
import static org.sagebionetworks.template.Constants.OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT;
import static org.sagebionetworks.template.Constants.PARAMETER_MYSQL_PASSWORD;
import static org.sagebionetworks.template.Constants.PARAM_KEY_TIME_TO_LIVE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MAX_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MIN_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_NUMBER;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_SSL_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_VERSION;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_EC2_INSTANCE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OAUTH_ENDPOINT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS;
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
import static org.sagebionetworks.template.Constants.TEMPALTE_BEAN_STALK_ENVIRONMENT;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.VPC_SUBNET_COLOR;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.Ec2Client;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.config.TimeToLive;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopy;
import org.sagebionetworks.template.repo.beanstalk.ElasticBeanstalkSolutionStackNameProvider;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentDescriptor;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsVelocityContextProvider;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogDescriptor;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogType;
import org.sagebionetworks.template.vpc.Color;

import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.PlatformFilter;
import com.amazonaws.services.elasticbeanstalk.model.PlatformSummary;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class RepositoryTemplateBuilderImplTest {

	@Mock
	private CloudFormationClient mockCloudFormationClient;
	@Mock
	private Ec2Client mockEc2Client;
	@Mock
	private AWSElasticBeanstalk mockBeanstalkClient;
	@Mock
	private RepoConfiguration config;
	@Mock
	private LoggerFactory mockLoggerFactory;
	@Mock
	private Logger mockLogger;
	@Mock
	private ArtifactCopy mockArtifactCopy;
	@Mock
	private SecretBuilder mockSecretBuilder;
	@Mock
	private VelocityContextProvider mockContextProvider1;
	@Mock
	private VelocityContextProvider mockContextProvider2;
	@Mock
	private ElasticBeanstalkSolutionStackNameProvider mockElasticBeanstalkSolutionStackNameProvider;
	@Mock
	private StackTagsProvider mockStackTagsProvider;
	@Mock
	private CloudwatchLogsVelocityContextProvider mockCwlContextProvider;
	@Mock
	private TimeToLive mockTimeToLive;
	@Captor
	private ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;

	private VelocityEngine velocityEngine;
	private RepositoryTemplateBuilderImpl builder;
	private RepositoryTemplateBuilderImpl builderSpy;

	private String stack;
	private String instance;
	private String vpcSubnetColor;
	
	private List<LogDescriptor> logDescriptors;

	private Stack sharedResouces;
	private String databaseEndpointSuffix;

	private SourceBundle secretsSouce;
	private String keyAlias;

	private List<Tag> expectedTags;

	@BeforeEach
	public void before() throws InterruptedException {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

		expectedTags = new LinkedList<>();
		Tag t = new Tag().withKey("aKey").withValue("aValue");
		expectedTags.add(t);

		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		builder = new RepositoryTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, config, mockLoggerFactory,
				mockArtifactCopy, mockSecretBuilder, Sets.newHashSet(mockContextProvider1, mockContextProvider2),
				mockElasticBeanstalkSolutionStackNameProvider, mockStackTagsProvider, mockCwlContextProvider,
				mockEc2Client, mockBeanstalkClient, mockTimeToLive);
		builderSpy = Mockito.spy(builder);

		stack = "dev";
		instance = "101";
		vpcSubnetColor = Color.Green.name();

		sharedResouces = new Stack();
		Output dbOut = new Output();
		dbOut.withOutputKey(stack + instance + OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		databaseEndpointSuffix = "something.amazon.com";
		dbOut.withOutputValue(stack + "-" + instance + "-db." + databaseEndpointSuffix);
		// TableDB output
		Output tableDBOutput1 = new Output();
		tableDBOutput1.withOutputKey(stack + instance + "Table0" + OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		tableDBOutput1.withOutputValue(stack + "-" + instance + "-table-0." + databaseEndpointSuffix);
		Output tableDBOutput2 = new Output();
		tableDBOutput2.withOutputKey(stack + instance + "Table1" + OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		tableDBOutput2.withOutputValue(stack + "-" + instance + "-table-1." + databaseEndpointSuffix);

		sharedResouces.withOutputs(dbOut, tableDBOutput1, tableDBOutput2);

		secretsSouce = new SourceBundle("secretBucket", "secretKey");
		keyAlias = "alias/some/alias";

		// CloudwatchLogs
		logDescriptors = this.generateLogDescriptors();
	}

	private void configureStack(String inputStack) throws InterruptedException {
		stack = inputStack;
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		sharedResouces = new Stack();
		Output dbOut = new Output();
		dbOut.withOutputKey(stack + instance + OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		databaseEndpointSuffix = "something.amazon.com";
		dbOut.withOutputValue(stack + "-" + instance + "-db." + databaseEndpointSuffix);
		sharedResouces.withOutputs(dbOut);

		when(mockCloudFormationClient.waitForStackToComplete(any(String.class)))
				.thenReturn(Optional.of(sharedResouces));
	}

	@Test
	public void testBuildAndDeployProd() throws InterruptedException {
		
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);
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
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN + type.getShortName())).thenReturn("the:ssl:arn");
			when(config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE + type.getShortName())).thenReturn("synapes.org");
		}

		when(mockArtifactCopy.copyArtifactIfNeeded(any(), any(), anyInt()))
				.thenReturn(new SourceBundle("bucket", "key-one"));

		when(config.getProperty((PROPERTY_KEY_OAUTH_ENDPOINT))).thenReturn("https://oauthendpoint");

		when(config.getProperty(PROPERTY_KEY_EC2_INSTANCE_TYPE)).thenReturn("t2.medium");

		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		when(mockSecretBuilder.getCMKAlias()).thenReturn(keyAlias);

		when(mockElasticBeanstalkSolutionStackNameProvider.getSolutionStackName(anyString(), anyString(), anyString()))
				.thenReturn("fake stack");
		when(mockCwlContextProvider.getLogDescriptors(any(EnvironmentType.class))).thenReturn(logDescriptors);
		
		/////
		when(config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING)).thenReturn("true");
		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getComaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);
		setupValidBeanstalkConfig();
		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClient.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2Client.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
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
		JSONObject templateJson = new JSONObject(bodyJSONString);
		
		JSONObject resources = templateJson.getJSONObject("Resources");
		assertNotNull(resources);
		// database group
		validateResouceDatabaseSubnetGroup(resources, stack);
		// database instance
		validateResouceDatabaseInstance(resources, stack, "true");
		// tables database
		validateResouceTablesDatabase(resources, stack, "true");
		validateWebAcl(resources);

		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES);
		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_WORKERS);
		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.PORTAL);

		// prod should have alarms.
		assertTrue(resources.has("prod101Table1RepositoryDBAlarmSwapUsage"));
		assertTrue(resources.has("prod101Table1RepositoryDBAlarmSwapUsage"));
		assertTrue(resources.has("prod101Table1RepositoryDBHighWriteLatency"));
		assertTrue(resources.has("prod101Table1RepositoryDBHighCPUUtilization"));
		assertTrue(resources.has("prod101Table1RepositoryDBLowFreeStorageSpace"));

		assertTrue(resources.has("prod101RepositoryDB"));
		JSONObject repoDB = (JSONObject) resources.get("prod101RepositoryDB");
		JSONObject dbProps = (JSONObject) repoDB.get("Properties");
		assertFalse(dbProps.has("DBSnapshotIdentifier"));
		assertTrue(dbProps.has("DBName"));

		assertTrue(resources.has("prod101Table0RepositoryDB"));
		JSONObject tableDB = (JSONObject) resources.get("prod101Table0RepositoryDB");
		JSONObject tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));

		assertTrue(resources.has("prod101Table1RepositoryDB"));
		tableDB = (JSONObject) resources.get("prod101Table1RepositoryDB");
		tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));
	}

	@Test
	public void testBuildAndDeployProdNoMonitoring() throws InterruptedException {
		
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);
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
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN + type.getShortName())).thenReturn("the:ssl:arn");
			when(config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE + type.getShortName())).thenReturn("synapes.org");
		}

		when(mockArtifactCopy.copyArtifactIfNeeded(any(), any(), anyInt()))
				.thenReturn(new SourceBundle("bucket", "key-one"));

		when(config.getProperty((PROPERTY_KEY_OAUTH_ENDPOINT))).thenReturn("https://oauthendpoint");

		when(config.getProperty(PROPERTY_KEY_EC2_INSTANCE_TYPE)).thenReturn("t2.medium");

		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		when(mockSecretBuilder.getCMKAlias()).thenReturn(keyAlias);

		when(mockElasticBeanstalkSolutionStackNameProvider.getSolutionStackName(anyString(), anyString(), anyString()))
				.thenReturn("fake stack");
		when(mockCwlContextProvider.getLogDescriptors(any(EnvironmentType.class))).thenReturn(logDescriptors);
		
		when(config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING)).thenReturn("false");
		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getComaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);
		setupValidBeanstalkConfig();
		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClient.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2Client.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
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
		JSONObject templateJson = new JSONObject(bodyJSONString);
		JSONObject resources = templateJson.getJSONObject("Resources");
		assertNotNull(resources);
		// database group
		validateResouceDatabaseSubnetGroup(resources, stack);
		// database instance
		validateResouceDatabaseInstance(resources, stack, "false");
		// tables database
		validateResouceTablesDatabase(resources, stack, "false");

		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES);
		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_WORKERS);
		verify(mockCwlContextProvider).getLogDescriptors(EnvironmentType.PORTAL);

		List<String> evironmentNames = Lists.newArrayList("repo-prod-101-0", "workers-prod-101-0", "portal-prod-101-0");
		// prod should have alarms.
		assertTrue(resources.has("prod101Table1RepositoryDBAlarmSwapUsage"));
		assertTrue(resources.has("prod101Table1RepositoryDBAlarmSwapUsage"));
		assertTrue(resources.has("prod101Table1RepositoryDBHighWriteLatency"));
		assertTrue(resources.has("prod101Table1RepositoryDBHighCPUUtilization"));
		assertTrue(resources.has("prod101Table1RepositoryDBLowFreeStorageSpace"));

		assertTrue(resources.has("prod101RepositoryDB"));
		JSONObject repoDB = (JSONObject) resources.get("prod101RepositoryDB");
		JSONObject dbProps = (JSONObject) repoDB.get("Properties");
		assertFalse(dbProps.has("DBSnapshotIdentifier"));
		assertTrue(dbProps.has("DBName"));

		assertTrue(resources.has("prod101Table0RepositoryDB"));
		JSONObject tableDB = (JSONObject) resources.get("prod101Table0RepositoryDB");
		JSONObject tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));

		assertTrue(resources.has("prod101Table1RepositoryDB"));
		tableDB = (JSONObject) resources.get("prod101Table1RepositoryDB");
		tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));
	}

	@Test
	public void testBuildAndDeployDev() throws InterruptedException {
		
		when(mockTimeToLive.createTimeToLiveParameter()).thenReturn(
				Optional.of(new Parameter().withParameterKey(PARAM_KEY_TIME_TO_LIVE).withParameterValue("NONE")));
		
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);
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
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN + type.getShortName())).thenReturn("the:ssl:arn");
			when(config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE + type.getShortName())).thenReturn("synapes.org");
		}

		when(mockArtifactCopy.copyArtifactIfNeeded(any(), any(), anyInt()))
				.thenReturn(new SourceBundle("bucket", "key-one"));

		when(config.getProperty((PROPERTY_KEY_OAUTH_ENDPOINT))).thenReturn("https://oauthendpoint");

		when(config.getProperty(PROPERTY_KEY_EC2_INSTANCE_TYPE)).thenReturn("t2.medium");

		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		when(mockSecretBuilder.getCMKAlias()).thenReturn(keyAlias);

		when(mockElasticBeanstalkSolutionStackNameProvider.getSolutionStackName(anyString(), anyString(), anyString()))
				.thenReturn("fake stack");
		when(mockCwlContextProvider.getLogDescriptors(any(EnvironmentType.class))).thenReturn(logDescriptors);	
		when(config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING)).thenReturn("true"); // does not matter if
																									// dev
		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getComaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);
		setupValidBeanstalkConfig();
		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClient.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2Client.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
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
		assertEquals(2, request.getParameters().length);
		assertEquals("NONE", request.getParameters()[1].getParameterValue());
		String bodyJSONString = request.getTemplateBody();
		assertNotNull(bodyJSONString);
		JSONObject templateJson = new JSONObject(bodyJSONString);
		
		assertEquals("NONE", templateJson.getJSONObject("Parameters").getJSONObject("TimeToLive").get("Default"));
				
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

		assertTrue(resources.has("dev101RepositoryDB"));
		JSONObject repoDB = (JSONObject) resources.get("dev101RepositoryDB");
		JSONObject dbProps = (JSONObject) repoDB.get("Properties");
		assertFalse(dbProps.has("DBSnapshotIdentifier"));
		assertTrue(dbProps.has("DBName"));
		validateResouceDatabaseInstance(resources, stack, "true");

		assertTrue(resources.has("dev101Table0RepositoryDB"));
		JSONObject tableDB = (JSONObject) resources.get("dev101Table0RepositoryDB");
		JSONObject tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));

		assertTrue(resources.has("dev101Table1RepositoryDB"));
		tableDB = (JSONObject) resources.get("dev101Table1RepositoryDB");
		tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));

	}

	@Test
	public void testBuildAndDeployDevFromSnapshot() throws InterruptedException {
	
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);
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
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN + type.getShortName())).thenReturn("the:ssl:arn");
			when(config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE + type.getShortName())).thenReturn("synapes.org");
		}

		when(mockArtifactCopy.copyArtifactIfNeeded(any(), any(), anyInt()))
				.thenReturn(new SourceBundle("bucket", "key-one"));

		when(config.getProperty((PROPERTY_KEY_OAUTH_ENDPOINT))).thenReturn("https://oauthendpoint");

		when(config.getProperty(PROPERTY_KEY_EC2_INSTANCE_TYPE)).thenReturn("t2.medium");

		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		when(mockSecretBuilder.getCMKAlias()).thenReturn(keyAlias);

		when(mockElasticBeanstalkSolutionStackNameProvider.getSolutionStackName(anyString(), anyString(), anyString()))
				.thenReturn("fake stack");
		when(mockCwlContextProvider.getLogDescriptors(any(EnvironmentType.class))).thenReturn(logDescriptors);

		setupValidBeanstalkConfig();
		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClient.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2Client.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn("repoSnapshotIdentifier");
		String[] tableSnaphotIdentifiers = { "table0SnapshotIdentifier", "table1SnapshotIdentifier" };
		when(config.getComaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS))
				.thenReturn(tableSnaphotIdentifiers);
		when(config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING)).thenReturn("false");
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

		assertTrue(resources.has("dev101RepositoryDB"));
		JSONObject repoDB = (JSONObject) resources.get("dev101RepositoryDB");
		JSONObject dbProps = (JSONObject) repoDB.get("Properties");
		assertTrue(dbProps.has("DBSnapshotIdentifier"));
		assertFalse(dbProps.has("DBName"));

		assertTrue(resources.has("dev101Table0RepositoryDB"));
		JSONObject tableDB = (JSONObject) resources.get("dev101Table0RepositoryDB");
		JSONObject tDbProps = (JSONObject) tableDB.get("Properties");
		assertTrue(tDbProps.has("DBSnapshotIdentifier"));
		assertEquals("table0SnapshotIdentifier", tDbProps.get("DBSnapshotIdentifier"));
		assertFalse(tDbProps.has("DBName"));

		assertTrue(resources.has("dev101Table1RepositoryDB"));
		tableDB = (JSONObject) resources.get("dev101Table1RepositoryDB");
		tDbProps = (JSONObject) tableDB.get("Properties");
		assertTrue(tDbProps.has("DBSnapshotIdentifier"));
		assertEquals("table1SnapshotIdentifier", tDbProps.get("DBSnapshotIdentifier"));
		assertFalse(tDbProps.has("DBName"));
	}

	public void validateResouceDatabaseSubnetGroup(JSONObject resources, String stack) {
		JSONObject subnetGroup = resources.getJSONObject(stack + "101DBSubnetGroup");
		assertNotNull(subnetGroup);
		JSONObject properties = subnetGroup.getJSONObject("Properties");
		assertTrue(properties.has("SubnetIds"));
	}

	/**
	 * Validate for the repo AWS::RDS::DBInstance.
	 * 
	 * @param resources
	 */
	public void validateResouceDatabaseInstance(JSONObject resources, String stack, String enableEnhancedMonitoring) {
		JSONObject instance = resources.getJSONObject(stack + "101RepositoryDB");
		assertNotNull(instance);
		DeletionPolicy expectedPolicy = Constants.isProd(stack)? DeletionPolicy.Snapshot: DeletionPolicy.Delete;
		assertEquals(expectedPolicy.name(), instance.get("DeletionPolicy"));
		JSONObject properties = instance.getJSONObject("Properties");
		assertEquals("4", properties.get("AllocatedStorage"));
		assertEquals("8", properties.get("MaxAllocatedStorage"));
		assertEquals("db.t2.small", properties.get("DBInstanceClass"));
		assertEquals(Boolean.TRUE, properties.get("MultiAZ"));
		assertNotNull(properties.get("BackupRetentionPeriod"));
		validateEnhancedMonitoring(properties, enableEnhancedMonitoring);
	}

	/**
	 * Validate tables for AWS::RDS::DBInstance
	 * 
	 * @param resources
	 */
	public void validateResouceTablesDatabase(JSONObject resources, String stack, String enableEnhancedMonitoring) {
		// zero
		JSONObject instance = resources.getJSONObject(stack + "101Table0RepositoryDB");
		DeletionPolicy expectedPolicy = Constants.isProd(stack)? DeletionPolicy.Snapshot: DeletionPolicy.Delete;
		assertEquals(expectedPolicy.name(), instance.get("DeletionPolicy"));
		assertNotNull(instance);
		JSONObject properties = instance.getJSONObject("Properties");
		assertEquals("3", properties.get("AllocatedStorage"));
		assertEquals("6", properties.get("MaxAllocatedStorage"));
		assertEquals("db.t2.micro", properties.get("DBInstanceClass"));
		assertEquals(Boolean.FALSE, properties.get("MultiAZ"));
		assertEquals(stack + "-101-table-0", properties.get("DBInstanceIdentifier"));
		assertEquals(stack + "101", properties.get("DBName"));
		assertEquals(1, properties.get("BackupRetentionPeriod"));
		validateEnhancedMonitoring(properties, enableEnhancedMonitoring);
		// one
		instance = resources.getJSONObject(stack + "101Table1RepositoryDB");
		assertNotNull(instance);
		properties = instance.getJSONObject("Properties");
		assertEquals("3", properties.get("AllocatedStorage"));
		assertEquals("6", properties.get("MaxAllocatedStorage"));
		assertEquals("db.t2.micro", properties.get("DBInstanceClass"));
		assertEquals(Boolean.FALSE, properties.get("MultiAZ"));
		assertEquals(stack + "-101-table-1", properties.get("DBInstanceIdentifier"));
		assertEquals(stack + "101", properties.get("DBName"));
		validateEnhancedMonitoring(properties, enableEnhancedMonitoring);
	}

	public static void validateWebAcl(JSONObject resources) {
		JSONObject webAcl = resources.getJSONObject("prod101WebACL");
		JSONObject props = webAcl.getJSONObject("Properties");
		JSONArray rules = props.getJSONArray("Rules");
		assertEquals(10, rules.length());
		
		JSONObject adminRule = rules.getJSONObject(9);
		assertEquals("prod-101-Admin-Access-Rule",adminRule.get("Name"));
		assertEquals("{\"Block\":{}}",adminRule.getJSONObject("Action").toString());
		
		assertEquals("Retain", resources.getJSONObject("prod101WebAclLogGroup").get("DeletionPolicy"));
	}

	public void validateEnhancedMonitoring(JSONObject props, String enableEnhancedMonitoring) {
		assertTrue(props.has("EnablePerformanceInsights"));
		assertEquals(Boolean.parseBoolean(enableEnhancedMonitoring), props.get("EnablePerformanceInsights"));
		if ("true".equals(enableEnhancedMonitoring)) {
			assertTrue(props.has("MonitoringInterval"));
			assertTrue(props.has("MonitoringRoleArn"));
		} else {
			assertFalse(props.has("MonitoringInterval"));
			assertFalse(props.has("MonitoringRoleArn"));
		}
	}

	@Test
	public void testGetParamters() {

		when(mockSecretBuilder.getRepositoryDatabasePassword()).thenReturn("somePassword");
		when(mockTimeToLive.createTimeToLiveParameter()).thenReturn(Optional.empty());

		// call under test
		Parameter[] params = builder.createSharedParameters();
		assertNotNull(params);
		assertEquals(1, params.length);
		Parameter param = params[0];
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.getParameterKey());
		assertEquals("somePassword", param.getParameterValue());

		verify(mockTimeToLive).createTimeToLiveParameter();
	}
	
	@Test
	public void testGetParamtersWithTimeToLive() {

		when(mockSecretBuilder.getRepositoryDatabasePassword()).thenReturn("somePassword");
		when(mockTimeToLive.createTimeToLiveParameter()).thenReturn(
				Optional.of(new Parameter().withParameterKey(PARAM_KEY_TIME_TO_LIVE).withParameterValue("NONE")));

		// call under test
		Parameter[] params = builder.createSharedParameters();
		assertNotNull(params);
		assertEquals(2, params.length);
		Parameter param = params[0];
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.getParameterKey());
		assertEquals("somePassword", param.getParameterValue());

		param = params[1];
		assertEquals(PARAM_KEY_TIME_TO_LIVE, param.getParameterKey());
		assertEquals("NONE", param.getParameterValue());

		verify(mockTimeToLive).createTimeToLiveParameter();
	}

	@Test
	public void testcreateSharedResourcesStackName() {
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
	
		// call under test
		String name = builder.createSharedResourcesStackName();
		assertEquals("dev-101-shared-resources", name);
	}

	@Test
	public void testCreateContextInvalidSnapshotState() {
		
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);

		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE)).thenReturn(4);
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(8);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(2);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			builder.createSharedContext();
		});
	}

	@Test
	public void testCreateContext() {
		
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);

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
		when(config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING)).thenReturn("true");

//		
		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getComaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);

		// call under test
		VelocityContext context = builder.createSharedContext();

		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("dev-101-shared-resources", context.get(SHARED_RESOUCES_STACK_NAME));
		assertEquals("us-east-1-synapse-dev-vpc-2", context.get(VPC_EXPORT_PREFIX));
		
		assertEquals("Count:{}", context.get(ADMIN_RULE_ACTION));
		assertEquals("Delete", context.get(DELETION_POLICY));

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
	public void testCreateContextProd() {
		stack = "prod";
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);

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
		when(config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING)).thenReturn("true");

		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getComaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);

		// call under test
		VelocityContext context = builder.createSharedContext();

		assertNotNull(context);
		assertEquals("prod", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("prod-101-shared-resources", context.get(SHARED_RESOUCES_STACK_NAME));
		assertEquals("us-east-1-synapse-prod-vpc-2", context.get(VPC_EXPORT_PREFIX));
		
		assertEquals("Block:{}", context.get(ADMIN_RULE_ACTION));
		assertEquals("Retain", context.get(DELETION_POLICY));
	}


	@Test
	public void testCreateEnvironments() {
		
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);

		for (EnvironmentType type : EnvironmentType.values()) {
			String version = "version-" + type.getShortName();
			when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + type.getShortName())).thenReturn(0);
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_VERSION + type.getShortName())).thenReturn(version);
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL + type.getShortName()))
					.thenReturn("url-" + type.getShortName());
			when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MIN_INSTANCES + type.getShortName())).thenReturn(1);
			when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MAX_INSTANCES + type.getShortName())).thenReturn(2);
			when(config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN + type.getShortName())).thenReturn("the:ssl:arn");
			when(config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE + type.getShortName())).thenReturn("synapes.org");
		}

		when(mockArtifactCopy.copyArtifactIfNeeded(any(), any(), anyInt()))
				.thenReturn(new SourceBundle("bucket", "key-one"));

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
		assertEquals("key-one", bundle.getKey());
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
		assertEquals("key-one", bundle.getKey());
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
		assertEquals("key-one", bundle.getKey());
		assertEquals("SynapesPortalInstanceProfile", desc.getInstanceProfileSuffix());
		// empty secrets should be passed to portal
		assertEquals(null, desc.getSecretsSource());
	}

	@Test
	public void testCreateEnvironments__missingPropertiesForEnvironment() {
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);

		for (EnvironmentType type : EnvironmentType.values()) {
			String version = "version-" + type.getShortName();
			when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + type.getShortName())).thenReturn(0);
			if (EnvironmentType.REPOSITORY_WORKERS.equals(type)) {
				// do not include the "workers" environment by making the config throw an
				// exception
				when(config.getProperty(
						PROPERTY_KEY_BEANSTALK_VERSION + EnvironmentType.REPOSITORY_WORKERS.getShortName()))
								.thenThrow(new ConfigurationPropertyNotFound("test.key"));
			} else {
				when(config.getProperty(PROPERTY_KEY_BEANSTALK_VERSION + type.getShortName())).thenReturn(version);
				when(config.getProperty(PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL + type.getShortName()))
						.thenReturn("url-" + type.getShortName());
				when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MIN_INSTANCES + type.getShortName())).thenReturn(1);
				when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MAX_INSTANCES + type.getShortName())).thenReturn(2);
				when(config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN + type.getShortName())).thenReturn("the:ssl:arn");
				when(config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE + type.getShortName())).thenReturn("synapes.org");
			}
		}

		when(mockArtifactCopy.copyArtifactIfNeeded(any(), any(), anyInt()))
				.thenReturn(new SourceBundle("bucket", "key-one"));	

		List<EnvironmentDescriptor> descriptors = builder.createEnvironments(secretsSouce);

		verify(mockLogger).warn(anyString());
		assertEquals(2, descriptors.size());
		Set<String> createEnvironmentTypes = descriptors.stream().map(EnvironmentDescriptor::getType)
				.collect(Collectors.toSet());
		assertEquals(Sets.newHashSet("repo", "portal"), createEnvironmentTypes);
	}

	@Test
	public void testCreateEnvironmentContext() {
		
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);

		when(config.getProperty((PROPERTY_KEY_OAUTH_ENDPOINT))).thenReturn("https://oauthendpoint");

		when(config.getProperty(PROPERTY_KEY_EC2_INSTANCE_TYPE)).thenReturn("t2.medium");

		when(mockSecretBuilder.getCMKAlias()).thenReturn(keyAlias);

		when(mockElasticBeanstalkSolutionStackNameProvider.getSolutionStackName(anyString(), anyString(), anyString()))
				.thenReturn("fake stack");
		when(mockCwlContextProvider.getLogDescriptors(any(EnvironmentType.class))).thenReturn(logDescriptors);

//		
		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClient.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2Client.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT)).thenReturn("9.0");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA)).thenReturn("11");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX)).thenReturn("latest");
		// This will make the call to getActualBeanstalkLinuxPlatform() return 3.4.7
		PlatformSummary expectedSummary = new PlatformSummary().withPlatformVersion("3.4.7");
		List<PlatformSummary> expectedSummaries = Arrays.asList(expectedSummary);
		ListPlatformVersionsResult expectedLpvr = new ListPlatformVersionsResult()
				.withPlatformSummaryList(expectedSummaries);
		when(mockBeanstalkClient.listPlatformVersions(any())).thenReturn(expectedLpvr);

		EnvironmentDescriptor environment = new EnvironmentDescriptor().withType(EnvironmentType.REPOSITORY_SERVICES);

		// call under test
		VelocityContext context = builder.createEnvironmentContext(sharedResouces, environment);

		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("us-east-1-synapse-dev-vpc-2", context.get(VPC_EXPORT_PREFIX));
		assertEquals("us-east-1-dev-101-shared-resources", context.get(SHARED_EXPORT_PREFIX));
		assertEquals(environment, context.get(ENVIRONMENT));
		assertEquals(0, context.get(REPO_BEANSTALK_NUMBER));
		assertEquals(keyAlias, context.get(STACK_CMK_ALIAS));
		assertEquals(databaseEndpointSuffix, context.get(DB_ENDPOINT_SUFFIX));
		assertEquals("t2.medium", context.get(EC2_INSTANCE_TYPE));
		assertEquals(String.join(",", EXPECTED_SUBNETS), context.get(BEANSTALK_INSTANCES_SUBNETS));
	}

	@Test
	public void testExtractDatabaseSuffix() {
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
	
		// call under test
		String suffix = builder.extractDatabaseSuffix(sharedResouces);
		assertEquals(databaseEndpointSuffix, suffix);
	}

	private List<LogDescriptor> generateLogDescriptors() {
		List<LogDescriptor> descriptors = new LinkedList<>();
		for (LogType t : LogType.values()) {
			LogDescriptor d = new LogDescriptor();
			d.setLogType(t);
			d.setLogPath("/var/log/mypath.log");
			d.setDateFormat("YYYY-MM-DD");
			d.setDeletionPolicy(DeletionPolicy.Retain);
			descriptors.add(d);
		}
		return descriptors;
	}

	@Test
	public void testValidateConfigPlatformNotFound() {

		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA)).thenReturn("11");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT)).thenReturn("9.0");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX)).thenReturn("4.5.6");
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2";
		PlatformFilter expectedFilter = new PlatformFilter().withType("PlatformName").withOperator("=")
				.withValues(expectedPlatformName);
		ListPlatformVersionsRequest expectedRequest = new ListPlatformVersionsRequest().withFilters(expectedFilter);
		// No plaform found with that name
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		ListPlatformVersionsResult expectedResult = new ListPlatformVersionsResult()
				.withPlatformSummaryList(expectedSummaries);
		when(mockBeanstalkClient.listPlatformVersions(expectedRequest)).thenReturn(expectedResult);

		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			builder.getActualBeanstalkAmazonLinuxPlatform();
		});

	}

	@Test
	public void testGetActualBeanstalkBeanstalkPlatformOverrideNotLatest() {
		// we explicitely request 3.4.6, which is not the latest version, expected is
		// 3.4.6 and log msg
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA)).thenReturn("11");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT)).thenReturn("9.0");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX)).thenReturn("3.4.6");
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2";
		PlatformFilter expectedFilter = new PlatformFilter().withType("PlatformName").withOperator("=")
				.withValues(expectedPlatformName);
		ListPlatformVersionsRequest expectedRequest = new ListPlatformVersionsRequest().withFilters(expectedFilter);
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		PlatformSummary summary = new PlatformSummary().withPlatformVersion("3.4.6");
		expectedSummaries.add(summary);
		summary = new PlatformSummary().withPlatformVersion("3.4.7");
		expectedSummaries.add(summary);
		ListPlatformVersionsResult expectedResult = new ListPlatformVersionsResult()
				.withPlatformSummaryList(expectedSummaries);
		when(mockBeanstalkClient.listPlatformVersions(expectedRequest)).thenReturn(expectedResult);

		// call under test
		String actualVersion = builder.getActualBeanstalkAmazonLinuxPlatform();
		assertEquals("3.4.6", actualVersion);
		verify(mockLogger).info(anyString());
	}

	@Test
	public void testGetActualBeanstalkBeanstalkPlatformOverrideLatest() {
		// we explicitely request 3.4.6, which is the latest version, expected is 3.4.6
		// and do not log msg
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA)).thenReturn("11");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT)).thenReturn("9.0");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX)).thenReturn("3.4.6");
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2";
		PlatformFilter expectedFilter = new PlatformFilter().withType("PlatformName").withOperator("=")
				.withValues(expectedPlatformName);
		ListPlatformVersionsRequest expectedRequest = new ListPlatformVersionsRequest().withFilters(expectedFilter);
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		PlatformSummary summary = new PlatformSummary().withPlatformVersion("3.4.5");
		expectedSummaries.add(summary);
		summary = new PlatformSummary().withPlatformVersion("3.4.6");
		expectedSummaries.add(summary);
		ListPlatformVersionsResult expectedResult = new ListPlatformVersionsResult()
				.withPlatformSummaryList(expectedSummaries);
		when(mockBeanstalkClient.listPlatformVersions(expectedRequest)).thenReturn(expectedResult);

		// call under test
		String actualVersion = builder.getActualBeanstalkAmazonLinuxPlatform();
		assertEquals("3.4.6", actualVersion);
		verify(mockLogger, never()).info(anyString());
	}

	@Test
	public void testGetActualBeanstalkBeanstalkPlatformLatest() {
		// we request latest, expected is 3.4.6 and do not log msg
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA)).thenReturn("11");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT)).thenReturn("9.0");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX)).thenReturn("latest");
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2";
		PlatformFilter expectedFilter = new PlatformFilter().withType("PlatformName").withOperator("=")
				.withValues(expectedPlatformName);
		ListPlatformVersionsRequest expectedRequest = new ListPlatformVersionsRequest().withFilters(expectedFilter);
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		PlatformSummary summary = new PlatformSummary().withPlatformVersion("3.4.5");
		expectedSummaries.add(summary);
		summary = new PlatformSummary().withPlatformVersion("3.4.6");
		expectedSummaries.add(summary);
		ListPlatformVersionsResult expectedResult = new ListPlatformVersionsResult()
				.withPlatformSummaryList(expectedSummaries);
		when(mockBeanstalkClient.listPlatformVersions(expectedRequest)).thenReturn(expectedResult);

		// call under test
		String actualVersion = builder.getActualBeanstalkAmazonLinuxPlatform();
		assertEquals("3.4.6", actualVersion);
		verify(mockLogger, never()).info(anyString());
	}

	@Test
	public void testValidateConfig() {
		setupValidBeanstalkConfig();
		// call under test
		builder.getActualBeanstalkAmazonLinuxPlatform();
	}

	@Test
	public void testCreateDatabaseDescriptorsWithProd() {
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);

		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE)).thenReturn(4);
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(8);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS)).thenReturn("db.t2.small");
		when(config.getBooleanProperty(PROPERTY_KEY_REPO_RDS_MULTI_AZ)).thenReturn(true);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.standard.name());
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_IOPS)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(1);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.io1.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);

		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getComaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);

		// call under test
		DatabaseDescriptor[] results = builder.createDatabaseDescriptors();
		DatabaseDescriptor[] expected = new DatabaseDescriptor[] {
				// repo
				new DatabaseDescriptor().withAllocatedStorage(4).withBackupRetentionPeriodDays(7).withDbIops(-1)
						.withDbName("prod101").withDbStorageType(DatabaseStorageType.standard.name())
						.withInstanceClass("db.t2.small").withInstanceIdentifier("prod-101-db")
						.withMaxAllocatedStorage(8).withMultiAZ(true).withResourceName("prod101RepositoryDB")
						.withSnapshotIdentifier(null).withDeletionPolicy(DeletionPolicy.Snapshot),
				// tables
				new DatabaseDescriptor().withAllocatedStorage(3).withBackupRetentionPeriodDays(1).withDbIops(1000)
						.withDbName("prod101").withDbStorageType(DatabaseStorageType.io1.name())
						.withInstanceClass("db.t2.micro").withInstanceIdentifier("prod-101-table-0")
						.withMaxAllocatedStorage(6).withMultiAZ(false).withResourceName("prod101Table0RepositoryDB")
						.withSnapshotIdentifier(null).withDeletionPolicy(DeletionPolicy.Snapshot)
		};
		assertEquals(2, results.length);
		assertEquals(expected[0], results[0]);
		assertEquals(expected[1], results[1]);
	}
	
	@Test
	public void testCreateDatabaseDescriptorsWithDev() {
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);

		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE)).thenReturn(4);
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(8);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS)).thenReturn("db.t2.small");
		when(config.getBooleanProperty(PROPERTY_KEY_REPO_RDS_MULTI_AZ)).thenReturn(true);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.standard.name());
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_IOPS)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(1);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.io1.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);

		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getComaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);

		// call under test
		DatabaseDescriptor[] results = builder.createDatabaseDescriptors();
		DatabaseDescriptor[] expected = new DatabaseDescriptor[] {
				// repo
				new DatabaseDescriptor().withAllocatedStorage(4).withBackupRetentionPeriodDays(0).withDbIops(-1)
						.withDbName("dev101").withDbStorageType(DatabaseStorageType.standard.name())
						.withInstanceClass("db.t2.small").withInstanceIdentifier("dev-101-db")
						.withMaxAllocatedStorage(8).withMultiAZ(true).withResourceName("dev101RepositoryDB")
						.withSnapshotIdentifier(null).withDeletionPolicy(DeletionPolicy.Delete),
				// tables
				new DatabaseDescriptor().withAllocatedStorage(3).withBackupRetentionPeriodDays(0).withDbIops(1000)
						.withDbName("dev101").withDbStorageType(DatabaseStorageType.io1.name())
						.withInstanceClass("db.t2.micro").withInstanceIdentifier("dev-101-table-0")
						.withMaxAllocatedStorage(6).withMultiAZ(false).withResourceName("dev101Table0RepositoryDB")
						.withSnapshotIdentifier(null).withDeletionPolicy(DeletionPolicy.Delete) };
		assertEquals(2, results.length);
		assertEquals(expected[0], results[0]);
		assertEquals(expected[1], results[1]);
	}
	
	@Test
	public void testBuildEnvironmentsWithoutTTL() {

		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		when(mockTimeToLive.createTimeToLiveParameter()).thenReturn(Optional.empty());

		EnvironmentDescriptor e1 = new EnvironmentDescriptor().withName("repo");
		EnvironmentDescriptor e2 = new EnvironmentDescriptor().withName("portal");
		doReturn(List.of(e1, e2)).when(builderSpy).createEnvironments(any());

		VelocityContext mockContext = Mockito.mock(VelocityContext.class);
		doReturn(mockContext).when(builderSpy).createEnvironmentContext(any(), any());

		doNothing().when(builderSpy).buildAndDeployStack(any(), any(), any(), any());

		// call under test
		builderSpy.buildEnvironments(sharedResouces);

		verify(mockSecretBuilder).createSecrets();
		verify(mockTimeToLive).createTimeToLiveParameter();
		verify(builderSpy).createEnvironments(secretsSouce);
		verify(builderSpy, times(2)).buildAndDeployStack(any(), any(), any(), any());
		verify(builderSpy).buildAndDeployStack(mockContext, e1.getName(), TEMPALTE_BEAN_STALK_ENVIRONMENT, null);
		verify(builderSpy).buildAndDeployStack(mockContext, e2.getName(), TEMPALTE_BEAN_STALK_ENVIRONMENT, null);
	}
	
	@Test
	public void testBuildEnvironmentsWithTTL() {

		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		Parameter ttl = new Parameter().withParameterKey("ttl").withParameterValue("value");
		when(mockTimeToLive.createTimeToLiveParameter()).thenReturn(Optional.of(ttl));

		EnvironmentDescriptor e1 = new EnvironmentDescriptor().withName("repo");
		EnvironmentDescriptor e2 = new EnvironmentDescriptor().withName("portal");
		doReturn(List.of(e1, e2)).when(builderSpy).createEnvironments(any());

		VelocityContext mockContext = Mockito.mock(VelocityContext.class);
		doReturn(mockContext).when(builderSpy).createEnvironmentContext(any(), any());

		doNothing().when(builderSpy).buildAndDeployStack(any(), any(), any(), any());

		// call under test
		builderSpy.buildEnvironments(sharedResouces);

		verify(mockSecretBuilder).createSecrets();
		verify(mockTimeToLive).createTimeToLiveParameter();
		verify(builderSpy).createEnvironments(secretsSouce);
		verify(builderSpy, times(2)).buildAndDeployStack(any(), any(), any(), any());
		verify(builderSpy).buildAndDeployStack(mockContext, e1.getName(), TEMPALTE_BEAN_STALK_ENVIRONMENT, ttl);
		verify(builderSpy).buildAndDeployStack(mockContext, e2.getName(), TEMPALTE_BEAN_STALK_ENVIRONMENT, ttl);
	}
	

	private void setupValidBeanstalkConfig() {
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA)).thenReturn("11");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT)).thenReturn("9.0");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX)).thenReturn("3.4.7");
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2";
		PlatformFilter expectedFilter = new PlatformFilter().withType("PlatformName").withOperator("=")
				.withValues(expectedPlatformName);
		ListPlatformVersionsRequest expectedRequest = new ListPlatformVersionsRequest().withFilters(expectedFilter);
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		PlatformSummary summary = new PlatformSummary().withPlatformVersion("3.4.7");
		expectedSummaries.add(summary);
		ListPlatformVersionsResult expectedResult = new ListPlatformVersionsResult()
				.withPlatformSummaryList(expectedSummaries);
		when(mockBeanstalkClient.listPlatformVersions(expectedRequest)).thenReturn(expectedResult);
	}

}
