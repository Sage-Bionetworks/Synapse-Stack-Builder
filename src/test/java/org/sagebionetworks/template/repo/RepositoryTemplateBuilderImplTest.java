package org.sagebionetworks.template.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.ADMIN_RULE_ACTION;
import static org.sagebionetworks.template.Constants.BEANSTALK_INSTANCES_SUBNETS;
import static org.sagebionetworks.template.Constants.CTXT_KEY_DATA_CDN_DOMAIN_NAME;
import static org.sagebionetworks.template.Constants.CTXT_KEY_DATA_CDN_PRIVATE_KEY_ID;
import static org.sagebionetworks.template.Constants.CTXT_KEY_DATA_DISCOVERY_DOCUMENT_URL;
import static org.sagebionetworks.template.Constants.DATABASE_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.DB_ENDPOINT_SUFFIX;
import static org.sagebionetworks.template.Constants.DELETION_POLICY;
import static org.sagebionetworks.template.Constants.EC2_INSTANCE_MEMORY;
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
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DATA_CDN_PRIVATE_KEY_ID;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_EC2_INSTANCE_MEMORY;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_EC2_INSTANCE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OPENSEARCH_INSTANCE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OPENSEARCH_MASTER_INSTANCE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OPENSEARCH_AVAILABILITY_ZONE_COUNT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_IMAGE_PIPELINE_ARN;
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
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_THROUGHPUT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ROUTE_53_HOSTED_ZONE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_INSTANCE_COUNT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_IOPS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_THROUGHPUT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_COLOR;
import static org.sagebionetworks.template.Constants.REPO_BEANSTALK_NUMBER;
import static org.sagebionetworks.template.Constants.SAGEBIO_COGNITO_APP_DISCOVERY_DOCUMENT;
import static org.sagebionetworks.template.Constants.SHARED_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.SHARED_RESOURCES_STACK_NAME;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.STACK_CMK_ALIAS;
import static org.sagebionetworks.template.Constants.CLOUDWATCH_LOGS_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.LOAD_BALANCER_ALARMS;
import static org.sagebionetworks.template.Constants.OAUTH_ENDPOINT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ECS_CONTAINER_PORT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ECS_TASK_CPU;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ECS_TASK_MEMORY;
import static org.sagebionetworks.template.Constants.TEMPLATE_BEANSTALK_ENVIRONMENT;
import static org.sagebionetworks.template.Constants.TEMPLATE_ECS_FARGATE_ENVIRONMENT;
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
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.Ec2ClientWrapper;
import org.sagebionetworks.template.ImageBuilderClient;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.config.TimeToLive;
import org.sagebionetworks.template.repo.agent.BedrockAgentContextProvider;
import org.sagebionetworks.template.repo.agent.BedrockGridAgentContextProvider;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopy;
import org.sagebionetworks.template.repo.beanstalk.ElasticBeanstalkSolutionStackNameProvider;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentDescriptor;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;
import org.sagebionetworks.template.repo.beanstalk.LoadBalancerAlarmsConfig;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsVelocityContextProvider;
import org.sagebionetworks.template.repo.ecs.DockerImageBuilder;
import org.sagebionetworks.template.repo.ecs.EcsEnvironmentDescriptor;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogDescriptor;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogType;
import org.sagebionetworks.template.repo.grid.GridContextProvider;
import org.sagebionetworks.template.vpc.Color;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.ListPlatformVersionsResponse;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformFilter;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformSummary;


@ExtendWith(MockitoExtension.class)
public class RepositoryTemplateBuilderImplTest {

	@Mock
	private CloudFormationClientWrapper mockCloudFormationClientWrapper;
	@Mock
	private Ec2ClientWrapper mockEc2ClientWrapper;
	@Mock
	private ElasticBeanstalkClient mockBeanstalkClient;
	@Mock
	private ImageBuilderClient mockImageBuilderClient;
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
	@Mock
	private S3Client mockS3Client;
	@Mock
	private DockerImageBuilder mockDockerImageBuilder;
	@Mock
	private LoadBalancerAlarmsConfig mockLoadBalancerAlarmsConfig;
	
	@Captor
	private ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;

	private VelocityEngine velocityEngine;
	private RepositoryTemplateBuilderImpl builder;
	private RepositoryTemplateBuilderImpl builderSpy;

	private String stack;
	private String instance;
	private String vpcSubnetColor;
	private String imagePipelineArn;
	private String imageId;
	
	private List<LogDescriptor> logDescriptors;

	private Stack sharedResouces;
	private String databaseEndpointSuffix;

	private SourceBundle secretsSouce;
	private String keyAlias;

	private List<Tag> expectedTags;
	private String gridQueueRef;


	@BeforeEach
	public void before() throws InterruptedException {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

		expectedTags = new LinkedList<>();
		Tag t = Tag.builder().key("aKey").value("aValue").build();
		expectedTags.add(t);

		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		
		gridQueueRef = "GridQueueRefQueue";
		
		builder = new RepositoryTemplateBuilderImpl(mockCloudFormationClientWrapper, velocityEngine, config, mockLoggerFactory,
				mockArtifactCopy, mockSecretBuilder,
				Sets.newHashSet(mockContextProvider1, mockContextProvider2,
						new BedrockAgentContextProvider(config, mockS3Client),
						new BedrockGridAgentContextProvider(config, mockS3Client),
						new GridContextProvider(gridQueueRef, config)),
				mockElasticBeanstalkSolutionStackNameProvider, mockStackTagsProvider, mockCwlContextProvider,
                mockEc2ClientWrapper, mockBeanstalkClient, mockImageBuilderClient, mockTimeToLive,
                mockDockerImageBuilder, mockLoadBalancerAlarmsConfig);
		
		builderSpy = Mockito.spy(builder);

		stack = "dev";
		instance = "101";
		vpcSubnetColor = Color.Green.name();
		imagePipelineArn="arn:aws:imagebuilder:us-east-1:867686887310:image/cis-for-eb";
		imageId = "ami-0123456789";

		databaseEndpointSuffix = "something.amazon.com";
		sharedResouces = Stack.builder().build();
		Output dbOut = Output.builder()
				.outputKey(stack + instance + OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT)
				.outputValue(stack + "-" + instance + "-db." + databaseEndpointSuffix)
				.build();
		// TableDB output
		Output tableDBOutput1 = Output.builder()
				.outputKey(stack + instance + "Table0" + OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT)
				.outputValue(stack + "-" + instance + "-table-0." + databaseEndpointSuffix)
				.build();
		Output tableDBOutput2 = Output.builder()
				.outputKey(stack + instance + "Table1" + OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT)
				.outputValue(stack + "-" + instance + "-table-1." + databaseEndpointSuffix)
				.build();
		sharedResouces = Stack.builder().outputs(dbOut, tableDBOutput1, tableDBOutput2).build();

		secretsSouce = new SourceBundle("secretBucket", "secretKey");
		keyAlias = "alias/some/alias";

		// CloudwatchLogs
		logDescriptors = this.generateLogDescriptors();
	}

	private void configureStack(String inputStack) throws InterruptedException {
		stack = inputStack;
		
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		
		databaseEndpointSuffix = "something.amazon.com";

		sharedResouces = Stack.builder().outputs(
			Output.builder()
				.outputKey(stack + instance + OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT)
				.outputValue(stack + "-" + instance + "-db." + databaseEndpointSuffix)
				.build()
		).build();
		when(mockCloudFormationClientWrapper.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(sharedResouces));
		
	}

	@Test
	public void testBuildAndDeployProd() throws InterruptedException {

		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("BEANSTALK");
		when(mockStackTagsProvider.getStackTags(config)).thenReturn(expectedTags);
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
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_THROUGHPUT)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(2);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.io1.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_THROUGHPUT)).thenReturn(15000);
		when(config.getProperty(PROPERTY_KEY_IMAGE_PIPELINE_ARN)).thenReturn(imagePipelineArn);
		
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
		when(config.getCommaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);
		setupValidBeanstalkConfig();
		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClientWrapper.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2ClientWrapper.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
		stack = "prod";
		configureStack(stack);
		when(config.getProperty(PROPERTY_KEY_OPENSEARCH_INSTANCE_TYPE)).thenReturn("r6g.xlarge");
		when(config.getProperty(PROPERTY_KEY_OPENSEARCH_MASTER_INSTANCE_TYPE)).thenReturn("m6g.large");
		when(config.getIntegerProperty(PROPERTY_KEY_OPENSEARCH_AVAILABILITY_ZONE_COUNT)).thenReturn(2);
		when(mockEc2ClientWrapper.getAvailableSubnetsForInstanceTypes(eq(List.of("r6g.xlarge", "m6g.large")), any(), eq(2)))
				.thenReturn(EXPECTED_SUBNETS);
		when(config.getProperty(PROPERTY_KEY_DATA_CDN_PRIVATE_KEY_ID)).thenReturn("CdnPrivateKeyId");
		when(config.getProperty(SAGEBIO_COGNITO_APP_DISCOVERY_DOCUMENT)).thenReturn("discoveryDocumentUrl");
		when(mockImageBuilderClient.getLatestImageIdForImagePipelineArn(imagePipelineArn)).thenReturn(imageId);
		
		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClientWrapper, times(4)).createOrUpdateStack(requestCaptor.capture());
		verify(mockCloudFormationClientWrapper).waitForStackToComplete("prod-101-shared-resources");
		
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
		// WebAcl
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
		assertFalse(dbProps.has("StorageThroughput"));

		assertTrue(resources.has("prod101Table0RepositoryDB"));
		JSONObject tableDB = (JSONObject) resources.get("prod101Table0RepositoryDB");
		JSONObject tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));
		assertEquals(15000, tDbProps.getInt("StorageThroughput"));

		assertTrue(resources.has("prod101Table1RepositoryDB"));
		tableDB = (JSONObject) resources.get("prod101Table1RepositoryDB");
		tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));
		assertEquals(15000, tDbProps.getInt("StorageThroughput"));
		
		assertFalse(resources.has("WebhookTestApi"));
		assertTrue(resources.has("SynapseSearchCollection"));
		assertTrue(resources.has("bedrockAgentRole"));
		assertTrue(resources.has("bedrockAgent"));
		assertTrue(resources.has("bedrockGridAgentRole"));
		assertTrue(resources.has("bedrockGridAgent"));
		
		assertTrue(resources.getJSONObject("bedrockAgentRole").toString().contains("arn:aws:s3:::prod-configuration.sagebase.org/chat/openapi/101.json"));
		
		JSONObject bedrockAgentProps = resources.getJSONObject("bedrockAgent").getJSONObject("Properties");
		
		assertEquals("prod-101-agent", bedrockAgentProps.get("AgentName"));
		
		validateOpenApiSchema(bedrockAgentProps);
		
		assertTrue(resources.getJSONObject("GridApiGatewaySQSRole").toString().contains(gridQueueRef));
		assertTrue(resources.getJSONObject("GridWebsocketApi").toString().contains("prod-101-grid-websocket"));
		
		assertEquals("prod", resources.getJSONObject("GridWebsocketStage").getJSONObject("Properties").get("StageName"));
		
		assertEquals("ENABLED", resources.getJSONObject("SynapseSearchCollection")
			.getJSONObject("Properties")
			.getString("StandbyReplicas")
		);
		
		assertTrue(
			resources.getJSONObject("SynapseSearchCollectionNetworkPolicy")
				.getJSONObject("Properties").getJSONObject("Policy").toString(2).contains("\\\"AllowFromPublic\\\": false")
		);
		
		assertTrue(
			resources.getJSONObject("SynapseSearchCollectionDataAccessPolicy")
				.getJSONObject("Properties").getJSONObject("Policy").toString(2).contains("prod101SynapesRepoWorkersServiceRole")
		);

		assertTrue(resources.has("SynapseSearchIndexDomain"));
		JSONObject prodDomainProps = resources.getJSONObject("SynapseSearchIndexDomain").getJSONObject("Properties");
		assertEquals("prod-101-synidx", prodDomainProps.getString("DomainName"));
		assertEquals("OpenSearch_3.5", prodDomainProps.getString("EngineVersion"));
		JSONObject prodClusterConfig = prodDomainProps.getJSONObject("ClusterConfig");
		assertEquals(2, prodClusterConfig.getInt("InstanceCount"));
		assertEquals("r6g.xlarge.search", prodClusterConfig.getString("InstanceType"));
		assertTrue(prodClusterConfig.getBoolean("DedicatedMasterEnabled"));
		assertEquals("m6g.large.search", prodClusterConfig.getString("DedicatedMasterType"));
		assertEquals(3, prodClusterConfig.getInt("DedicatedMasterCount"));
		assertTrue(prodClusterConfig.getBoolean("ZoneAwarenessEnabled"));
		assertEquals(2, prodClusterConfig.getJSONObject("ZoneAwarenessConfig").getInt("AvailabilityZoneCount"));
		JSONArray prodSubnetIds = prodDomainProps.getJSONObject("VPCOptions").getJSONArray("SubnetIds");
		assertEquals(2, prodSubnetIds.length());
		assertEquals("subnet1", prodSubnetIds.getString(0));
		assertEquals("subnet2", prodSubnetIds.getString(1));
		assertEquals("Retain", resources.getJSONObject("SynapseSearchIndexDomain").getString("DeletionPolicy"));
		assertTrue(prodDomainProps.getJSONObject("SoftwareUpdateOptions").getBoolean("AutoSoftwareUpdateEnabled"));
		assertTrue(
			prodDomainProps.getJSONObject("AccessPolicies").toString().contains("prod101SynapesRepoWorkersServiceRole")
		);
		assertTrue(resources.has("prod101SynapseSearchIndexSecurityGroup"));

	}

	void validateOpenApiSchema(JSONObject bedrockAgentProps) {

		JSONObject s3 = bedrockAgentProps.getJSONArray("ActionGroups").getJSONObject(1).getJSONObject("ApiSchema")
				.getJSONObject("S3");
		String openApiBucket = s3.getString("S3BucketName");
		assertEquals("prod-configuration.sagebase.org", openApiBucket);
		String openApiKey = s3.getString("S3ObjectKey");
		assertEquals("chat/openapi/101.json",s3.getString("S3ObjectKey"));
		verify(mockS3Client).putObject(
			(PutObjectRequest) argThat(req -> ((PutObjectRequest) req).bucket().equals(openApiBucket) && ((PutObjectRequest) req).key().equals(openApiKey)),
			any(RequestBody.class)
		);
	}

	@Test
	public void testBuildAndDeployProdNoMonitoring() throws InterruptedException {

		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("BEANSTALK");
		when(mockStackTagsProvider.getStackTags(config)).thenReturn(expectedTags);
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
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_THROUGHPUT)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(2);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.io1.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_THROUGHPUT)).thenReturn(-1);
		when(config.getProperty(PROPERTY_KEY_IMAGE_PIPELINE_ARN)).thenReturn(imagePipelineArn);
		
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
		when(config.getCommaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);
		setupValidBeanstalkConfig();
		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClientWrapper.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2ClientWrapper.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
		stack = "prod";
		configureStack(stack);
		when(config.getProperty(PROPERTY_KEY_OPENSEARCH_INSTANCE_TYPE)).thenReturn("r6g.xlarge");
		when(config.getProperty(PROPERTY_KEY_OPENSEARCH_MASTER_INSTANCE_TYPE)).thenReturn("m6g.large");
		when(config.getIntegerProperty(PROPERTY_KEY_OPENSEARCH_AVAILABILITY_ZONE_COUNT)).thenReturn(2);
		when(mockEc2ClientWrapper.getAvailableSubnetsForInstanceTypes(eq(List.of("r6g.xlarge", "m6g.large")), any(), eq(2)))
				.thenReturn(EXPECTED_SUBNETS);
		when(config.getProperty(PROPERTY_KEY_DATA_CDN_PRIVATE_KEY_ID)).thenReturn("CdnPrivateKeyId");
		when(config.getProperty(SAGEBIO_COGNITO_APP_DISCOVERY_DOCUMENT)).thenReturn("discoveryDocumentUrl");
		when(mockImageBuilderClient.getLatestImageIdForImagePipelineArn(imagePipelineArn)).thenReturn(imageId);


		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClientWrapper, times(4)).createOrUpdateStack(requestCaptor.capture());
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
		assertFalse(dbProps.has("StorageThroughput"));

		assertTrue(resources.has("prod101Table0RepositoryDB"));
		JSONObject tableDB = (JSONObject) resources.get("prod101Table0RepositoryDB");
		JSONObject tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));
		assertFalse(dbProps.has("StorageThroughput"));

		assertTrue(resources.has("prod101Table1RepositoryDB"));
		tableDB = (JSONObject) resources.get("prod101Table1RepositoryDB");
		tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));
		assertFalse(dbProps.has("StorageThroughput"));
	}
	
	@Test
	public void testBuildAndDeployDev() throws InterruptedException {

		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("BEANSTALK");
		when(mockTimeToLive.createTimeToLiveParameter()).thenReturn(
				Optional.of(Parameter.builder().parameterKey(PARAM_KEY_TIME_TO_LIVE).parameterValue("NONE").build()));
		
		when(mockStackTagsProvider.getStackTags(config)).thenReturn(expectedTags);
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
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_THROUGHPUT)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(2);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.io1.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_THROUGHPUT)).thenReturn(15000);
		when(config.getProperty(PROPERTY_KEY_IMAGE_PIPELINE_ARN)).thenReturn(imagePipelineArn);
		
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
		when(config.getCommaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);
		setupValidBeanstalkConfig();
		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClientWrapper.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2ClientWrapper.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
		stack = "dev";
		configureStack(stack);
		when(config.getProperty(PROPERTY_KEY_DATA_CDN_PRIVATE_KEY_ID)).thenReturn("CdnPrivateKeyId");
		when(config.getProperty(SAGEBIO_COGNITO_APP_DISCOVERY_DOCUMENT)).thenReturn("discoveryDocumentUrl");
		when(mockImageBuilderClient.getLatestImageIdForImagePipelineArn(imagePipelineArn)).thenReturn(imageId);

		
		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClientWrapper, times(4)).createOrUpdateStack(requestCaptor.capture());
		verify(mockCloudFormationClientWrapper).waitForStackToComplete("dev-101-shared-resources");
		
		List<CreateOrUpdateStackRequest> list = requestCaptor.getAllValues();
		CreateOrUpdateStackRequest request = list.get(0);
		assertEquals("dev-101-shared-resources", request.getStackName());
		assertEquals(expectedTags, request.getTags());
		assertEquals(false, request.getEnableTerminationProtection());
		assertNotNull(request.getParameters());
		assertEquals(2, request.getParameters().length);
		assertEquals("NONE", request.getParameters()[1].parameterValue());
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
		assertFalse(dbProps.has("StorageThroughput"));
		validateResouceDatabaseInstance(resources, stack, "true");

		assertTrue(resources.has("dev101Table0RepositoryDB"));
		JSONObject tableDB = (JSONObject) resources.get("dev101Table0RepositoryDB");
		JSONObject tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));
		assertEquals(15000, tDbProps.getInt("StorageThroughput"));

		assertTrue(resources.has("dev101Table1RepositoryDB"));
		tableDB = (JSONObject) resources.get("dev101Table1RepositoryDB");
		tDbProps = (JSONObject) tableDB.get("Properties");
		assertFalse(tDbProps.has("DBSnapshotIdentifier"));
		assertTrue(tDbProps.has("DBName"));
		assertEquals(15000, tDbProps.getInt("StorageThroughput"));

		assertTrue(resources.has("WebhookTestApi"));
		assertTrue(resources.has("SynapseSearchCollection"));
		assertTrue(resources.has("bedrockAgentRole"));
		assertTrue(resources.has("bedrockAgent"));
		
		assertEquals("dev-101-agent", resources.getJSONObject("bedrockAgent").getJSONObject("Properties").get("AgentName"));

		assertEquals("DISABLED", resources.getJSONObject("SynapseSearchCollection")
			.getJSONObject("Properties")
			.getString("StandbyReplicas")
		);
		
		assertTrue(
			resources.getJSONObject("SynapseSearchCollectionNetworkPolicy")
				.getJSONObject("Properties").getString("Policy").contains("\"AllowFromPublic\": true")
		);
		
		assertTrue(
			resources.getJSONObject("SynapseSearchCollectionDataAccessPolicy")
				.getJSONObject("Properties").getJSONObject("Policy").toString(2).contains("arn:aws:iam::${AWS::AccountId}:root")
		);

		assertTrue(resources.has("SynapseSearchIndexDomain"));
		JSONObject devDomainProps = resources.getJSONObject("SynapseSearchIndexDomain").getJSONObject("Properties");
		assertEquals("dev-101-synidx", devDomainProps.getString("DomainName"));
		assertEquals("OpenSearch_3.5", devDomainProps.getString("EngineVersion"));
		JSONObject devClusterConfig = devDomainProps.getJSONObject("ClusterConfig");
		assertEquals(1, devClusterConfig.getInt("InstanceCount"));
		assertEquals("t3.small.search", devClusterConfig.getString("InstanceType"));
		assertFalse(devClusterConfig.getBoolean("DedicatedMasterEnabled"));
		assertEquals(20, devDomainProps.getJSONObject("EBSOptions").getInt("VolumeSize"));
		assertFalse(devClusterConfig.getBoolean("ZoneAwarenessEnabled"));
		assertEquals("Delete", resources.getJSONObject("SynapseSearchIndexDomain").getString("DeletionPolicy"));
		assertTrue(devDomainProps.getJSONObject("SoftwareUpdateOptions").getBoolean("AutoSoftwareUpdateEnabled"));
		assertTrue(
			devDomainProps.getJSONObject("AccessPolicies").toString().contains("arn:aws:iam::${AWS::AccountId}:root")
		);
		// Dev renders a public domain (no VPCOptions, gated by the IAM AccessPolicies), so it
		// has no ENIs and the security group is not created.
		assertFalse(devDomainProps.has("VPCOptions"));
		assertFalse(resources.has("dev101SynapseSearchIndexSecurityGroup"));

	}

	@Test
	public void testBuildAndDeployDevFromSnapshot() throws InterruptedException {

		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("BEANSTALK");
		when(mockStackTagsProvider.getStackTags(config)).thenReturn(expectedTags);
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
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_THROUGHPUT)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(2);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.io1.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_THROUGHPUT)).thenReturn(1000);
		when(config.getProperty(PROPERTY_KEY_IMAGE_PIPELINE_ARN)).thenReturn(imagePipelineArn);
		
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
		when(mockCloudFormationClientWrapper.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2ClientWrapper.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn("repoSnapshotIdentifier");
		String[] tableSnaphotIdentifiers = { "table0SnapshotIdentifier", "table1SnapshotIdentifier" };
		when(config.getCommaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS))
				.thenReturn(tableSnaphotIdentifiers);
		when(config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING)).thenReturn("false");
		stack = "dev";
		configureStack(stack);
		when(config.getProperty(PROPERTY_KEY_DATA_CDN_PRIVATE_KEY_ID)).thenReturn("CdnPrivateKeyId");
		when(config.getProperty(SAGEBIO_COGNITO_APP_DISCOVERY_DOCUMENT)).thenReturn("discoveryDocumentUrl");
		when(mockImageBuilderClient.getLatestImageIdForImagePipelineArn(imagePipelineArn)).thenReturn(imageId);

		
		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClientWrapper, times(4)).createOrUpdateStack(requestCaptor.capture());
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
		assertEquals(13, rules.length());
			
		JSONObject adminRule = rules.getJSONObject(12);
		assertEquals("prod-101-Admin-Access-Rule",adminRule.get("Name"));
		assertEquals("{\"Block\":{}}",adminRule.getJSONObject("Action").toString());

		JSONObject sizeRestrictionsRule = rules.getJSONObject(3);
		assertEquals("prod-101-size-restrictions-rule", sizeRestrictionsRule.get("Name"));
		JSONObject statement = sizeRestrictionsRule.getJSONObject("Statement");
		JSONArray ruleSizeStatements = statement.getJSONObject("OrStatement").getJSONArray("Statements");
		JSONObject uriSizeRule = ruleSizeStatements.getJSONObject(2);
		assertEquals(1024, uriSizeRule.getJSONObject("SizeConstraintStatement").get("Size"));

		JSONObject webACLLoggingConfig = resources.getJSONObject("prod101WebAclLoggingConfiguration");
		JSONObject webACLCfgProps = webACLLoggingConfig.getJSONObject("Properties");
		JSONArray logDestConfigs = webACLCfgProps.getJSONArray("LogDestinationConfigs");
		assertEquals(1, logDestConfigs.length());
		JSONObject config = logDestConfigs.getJSONObject(0);
		String configVal = config.getString("Fn::ImportValue");
		assertEquals("us-east-1-synapse-prod-global-resources-WebAclCloudWatchLogGroupArn", configVal);
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
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.parameterKey());
		assertEquals("somePassword", param.parameterValue());

		verify(mockTimeToLive).createTimeToLiveParameter();
	}
	
	@Test
	public void testGetParamtersWithTimeToLive() {

		when(mockSecretBuilder.getRepositoryDatabasePassword()).thenReturn("somePassword");
		when(mockTimeToLive.createTimeToLiveParameter()).thenReturn(
				Optional.of(Parameter.builder().parameterKey(PARAM_KEY_TIME_TO_LIVE).parameterValue("NONE").build()));

		// call under test
		Parameter[] params = builder.createSharedParameters();
		assertNotNull(params);
		assertEquals(2, params.length);
		Parameter param = params[0];
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.parameterKey());
		assertEquals("somePassword", param.parameterValue());

		param = params[1];
		assertEquals(PARAM_KEY_TIME_TO_LIVE, param.parameterKey());
		assertEquals("NONE", param.parameterValue());

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

		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("BEANSTALK");
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);

		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE)).thenReturn(4);
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(8);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS)).thenReturn("db.t2.small");
		when(config.getBooleanProperty(PROPERTY_KEY_REPO_RDS_MULTI_AZ)).thenReturn(true);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.standard.name());
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_IOPS)).thenReturn(-1);
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_THROUGHPUT)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(2);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.gp3.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_THROUGHPUT)).thenReturn(1000);
		when(config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING)).thenReturn("true");
//		
		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getCommaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);
		
		// call under test
		VelocityContext context = builder.createSharedContext();

		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("dev-101-shared-resources", context.get(SHARED_RESOURCES_STACK_NAME));
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
		assertEquals(-1, desc.getDbThroughput());

		// table zero
		desc = descriptors[1];
		assertEquals(3, desc.getAllocatedStorage());
		assertEquals(6, desc.getMaxAllocatedStorage());
		assertEquals("dev101", desc.getDbName());
		assertEquals("db.t2.micro", desc.getInstanceClass());
		assertEquals("dev-101-table-0", desc.getInstanceIdentifier());
		assertEquals("dev101Table0RepositoryDB", desc.getResourceName());
		assertEquals(DatabaseStorageType.gp3.name(), desc.getDbStorageType());
		assertEquals(1000, desc.getDbIops());
		assertEquals(1000, desc.getDbThroughput());
		// table one
		desc = descriptors[2];
		assertEquals(3, desc.getAllocatedStorage());
		assertEquals(6, desc.getMaxAllocatedStorage());
		assertEquals("dev101", desc.getDbName());
		assertEquals("db.t2.micro", desc.getInstanceClass());
		assertEquals("dev-101-table-1", desc.getInstanceIdentifier());
		assertEquals("dev101Table1RepositoryDB", desc.getResourceName());
		assertEquals(DatabaseStorageType.gp3.name(), desc.getDbStorageType());
		assertEquals(1000, desc.getDbIops());
		assertEquals(1000, desc.getDbThroughput());

		verify(mockContextProvider1).addToContext(context);
		verify(mockContextProvider2).addToContext(context);
	}
	
	@Test
	public void testCreateContextProd() {
		stack = "prod";
		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("BEANSTALK");
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);
		when(config.getProperty(PROPERTY_KEY_OPENSEARCH_INSTANCE_TYPE)).thenReturn("r6g.xlarge");
		when(config.getProperty(PROPERTY_KEY_OPENSEARCH_MASTER_INSTANCE_TYPE)).thenReturn("m6g.large");
		when(config.getIntegerProperty(PROPERTY_KEY_OPENSEARCH_AVAILABILITY_ZONE_COUNT)).thenReturn(2);
		List<String> openSearchSubnets = Arrays.asList("subnet-1a", "subnet-1c");
		when(mockCloudFormationClientWrapper.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", openSearchSubnets));
		when(mockEc2ClientWrapper.getAvailableSubnetsForInstanceTypes(eq(List.of("r6g.xlarge", "m6g.large")), any(), eq(2)))
				.thenReturn(openSearchSubnets);

		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE)).thenReturn(4);
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(8);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS)).thenReturn("db.t2.small");
		when(config.getBooleanProperty(PROPERTY_KEY_REPO_RDS_MULTI_AZ)).thenReturn(true);
		when(config.getProperty(PROPERTY_KEY_REPO_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.standard.name());
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_IOPS)).thenReturn(-1);
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_THROUGHPUT)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(2);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.io1.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_THROUGHPUT)).thenReturn(1000);
		when(config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING)).thenReturn("true");

		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getCommaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);
		
		// call under test
		VelocityContext context = builder.createSharedContext();

		assertNotNull(context);
		assertEquals("prod", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("prod-101-shared-resources", context.get(SHARED_RESOURCES_STACK_NAME));
		assertEquals("us-east-1-synapse-prod-vpc-2", context.get(VPC_EXPORT_PREFIX));
		
		assertEquals("Block:{}", context.get(ADMIN_RULE_ACTION));
		assertEquals("Retain", context.get(DELETION_POLICY));
		assertEquals("r6g.xlarge", context.get(Constants.OPENSEARCH_INSTANCE_TYPE));
		assertEquals("m6g.large", context.get(Constants.OPENSEARCH_MASTER_INSTANCE_TYPE));
		assertEquals(2, context.get(Constants.OPENSEARCH_AVAILABILITY_ZONE_COUNT));
		assertEquals(openSearchSubnets, context.get(Constants.OPENSEARCH_SUBNETS));
	}


	@Test
	public void testCreateEnvironments() {
		
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_IMAGE_PIPELINE_ARN)).thenReturn(imagePipelineArn);
		
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
		when(mockImageBuilderClient.getLatestImageIdForImagePipelineArn(imagePipelineArn)).thenReturn(imageId);
		
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
		assertEquals(imageId, desc.getImageId());

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
		assertEquals(imageId, desc.getImageId());
		
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
		assertEquals(imageId, desc.getImageId());
	}

	@Test
	public void testCreateEnvironments__missingPropertiesForEnvironment() {
		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		
		for (EnvironmentType type : EnvironmentType.values()) {
			String version = "version-" + type.getShortName();
			when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + type.getShortName())).thenReturn(0);
			when(config.getProperty(PROPERTY_KEY_IMAGE_PIPELINE_ARN)).thenReturn(null);
			
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

		// method under test
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
		when(config.getIntegerProperty(PROPERTY_KEY_EC2_INSTANCE_MEMORY)).thenReturn(2048);
		when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + EnvironmentType.REPOSITORY_SERVICES.getShortName())).thenReturn(0);

		when(mockSecretBuilder.getCMKAlias()).thenReturn(keyAlias);

		when(mockElasticBeanstalkSolutionStackNameProvider.getSolutionStackName(anyString(), anyString(), anyString()))
				.thenReturn("fake stack");
		when(mockCwlContextProvider.getLogDescriptors(any(EnvironmentType.class))).thenReturn(logDescriptors);

//		
		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClientWrapper.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(mockEc2ClientWrapper.getAvailableSubnetsForInstanceType(anyString(), any())).thenReturn(EXPECTED_SUBNETS);
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT)).thenReturn("9.0");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA)).thenReturn("11");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX)).thenReturn("latest");
		// This will make the call to getActualBeanstalkLinuxPlatform() return 3.4.7
		PlatformSummary expectedSummary = PlatformSummary.builder().platformVersion("3.4.7").build();
		List<PlatformSummary> expectedSummaries = Arrays.asList(expectedSummary);
		ListPlatformVersionsResponse expectedLpvr = ListPlatformVersionsResponse.builder().platformSummaryList(expectedSummaries).build();
		when(mockBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class))).thenReturn(expectedLpvr);
		when(config.getProperty("org.sagebionetworks.cloudfront.private.key.id")).thenReturn("dataCdnPrivateKeyId");
		when(config.getProperty("org.sagebionetworks.oauth2.sagebio.discoveryDocument")).thenReturn("discoveryDocumentUrl");

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
		assertEquals(2048, context.get(EC2_INSTANCE_MEMORY));
		assertEquals(String.join(",", EXPECTED_SUBNETS), context.get(BEANSTALK_INSTANCES_SUBNETS));
		assertEquals("data.dev.sagebase.org", context.get(CTXT_KEY_DATA_CDN_DOMAIN_NAME));
		assertEquals("dataCdnPrivateKeyId", context.get(CTXT_KEY_DATA_CDN_PRIVATE_KEY_ID));
		assertEquals("discoveryDocumentUrl", context.get(CTXT_KEY_DATA_DISCOVERY_DOCUMENT_URL));
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
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2023";
		PlatformFilter expectedFilter = PlatformFilter.builder().type("PlatformName").operator("=").values(expectedPlatformName).build();
		ListPlatformVersionsRequest expectedRequest = ListPlatformVersionsRequest.builder().filters(expectedFilter).build();

		// No platform found with that name
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		ListPlatformVersionsResponse expectedResult = ListPlatformVersionsResponse.builder().platformSummaryList(expectedSummaries).build();
		when(mockBeanstalkClient.listPlatformVersions(expectedRequest)).thenReturn(expectedResult);

		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			builder.getActualBeanstalkAmazonLinuxPlatform();
		});

	}

	@Test
	public void testGetActualBeanstalkBeanstalkPlatformOverrideNotLatest() {
		// we explicitly request 3.4.6, which is not the latest version, expected is
		// 3.4.6 and log msg
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA)).thenReturn("11");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT)).thenReturn("9.0");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX)).thenReturn("3.4.6");
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2023";
		PlatformFilter expectedFilter = PlatformFilter.builder()
				.type("PlatformName")
				.operator("=")
				.values(expectedPlatformName)
				.build();
		ListPlatformVersionsRequest expectedRequest = ListPlatformVersionsRequest.builder()
				.filters(expectedFilter)
				.build();
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		expectedSummaries.add(PlatformSummary.builder().platformVersion("3.4.6").build());
		expectedSummaries.add(PlatformSummary.builder().platformVersion("3.4.7").build());
		ListPlatformVersionsResponse expectedResult = ListPlatformVersionsResponse.builder()
				.platformSummaryList(expectedSummaries)
				.build();
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
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2023";
		PlatformFilter expectedFilter = PlatformFilter.builder()
				.type("PlatformName")
				.operator("=")
				.values(expectedPlatformName)
				.build();
		ListPlatformVersionsRequest expectedRequest = ListPlatformVersionsRequest.builder()
				.filters(expectedFilter)
				.build();
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		expectedSummaries.add(PlatformSummary.builder().platformVersion("3.4.5").build());
		expectedSummaries.add(PlatformSummary.builder().platformVersion("3.4.6").build());
		ListPlatformVersionsResponse expectedResult = ListPlatformVersionsResponse.builder()
				.platformSummaryList(expectedSummaries)
				.build();
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
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2023";
		PlatformFilter expectedFilter = PlatformFilter.builder()
				.type("PlatformName")
				.operator("=")
				.values(expectedPlatformName)
				.build();
		ListPlatformVersionsRequest expectedRequest = ListPlatformVersionsRequest.builder()
				.filters(expectedFilter)
				.build();
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		PlatformSummary summary = PlatformSummary.builder().platformVersion("3.4.5").build();
		expectedSummaries.add(summary);
		summary = PlatformSummary.builder().platformVersion("3.4.6").build();
		expectedSummaries.add(summary);
		ListPlatformVersionsResponse expectedResult = ListPlatformVersionsResponse.builder()
				.platformSummaryList(expectedSummaries)
				.build();
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
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_THROUGHPUT)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(1);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.gp3.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_THROUGHPUT)).thenReturn(1000);

		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getCommaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);

		// call under test
		DatabaseDescriptor[] results = builder.createDatabaseDescriptors();
		DatabaseDescriptor[] expected = new DatabaseDescriptor[] {
				// repo
				new DatabaseDescriptor().withAllocatedStorage(4).withBackupRetentionPeriodDays(7)
						.withDbIops(-1).withDbThroughput(-1)
						.withDbName("prod101").withDbStorageType(DatabaseStorageType.standard.name())
						.withInstanceClass("db.t2.small").withInstanceIdentifier("prod-101-db")
						.withMaxAllocatedStorage(8).withMultiAZ(true).withResourceName("prod101RepositoryDB")
						.withSnapshotIdentifier(null).withDeletionPolicy(DeletionPolicy.Snapshot),
				// tables
				new DatabaseDescriptor().withAllocatedStorage(3).withBackupRetentionPeriodDays(1)
						.withDbIops(1000).withDbThroughput(1000)
						.withDbName("prod101").withDbStorageType(DatabaseStorageType.gp3.name())
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
		when(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_THROUGHPUT)).thenReturn(-1);

		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE)).thenReturn(3);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE)).thenReturn(6);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT)).thenReturn(1);
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).thenReturn("db.t2.micro");
		when(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE)).thenReturn(DatabaseStorageType.gp3.name());
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS)).thenReturn(1000);
		when(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_THROUGHPUT)).thenReturn(1000);

		when(config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER)).thenReturn(NOSNAPSHOT);
		String[] noSnapshots = new String[] { NOSNAPSHOT };
		when(config.getCommaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS)).thenReturn(noSnapshots);

		// call under test
		DatabaseDescriptor[] results = builder.createDatabaseDescriptors();
		DatabaseDescriptor[] expected = new DatabaseDescriptor[] {
				// repo
				new DatabaseDescriptor().withAllocatedStorage(4).withBackupRetentionPeriodDays(7)
						.withDbIops(-1).withDbThroughput(-1)
						.withDbName("dev101").withDbStorageType(DatabaseStorageType.standard.name())
						.withInstanceClass("db.t2.small").withInstanceIdentifier("dev-101-db")
						.withMaxAllocatedStorage(8).withMultiAZ(true).withResourceName("dev101RepositoryDB")
						.withSnapshotIdentifier(null).withDeletionPolicy(DeletionPolicy.Delete),
				// tables
				new DatabaseDescriptor().withAllocatedStorage(3).withBackupRetentionPeriodDays(0)
						.withDbIops(1000).withDbThroughput(1000)
						.withDbName("dev101").withDbStorageType(DatabaseStorageType.gp3.name())
						.withInstanceClass("db.t2.micro").withInstanceIdentifier("dev-101-table-0")
						.withMaxAllocatedStorage(6).withMultiAZ(false).withResourceName("dev101Table0RepositoryDB")
						.withSnapshotIdentifier(null).withDeletionPolicy(DeletionPolicy.Delete) };
		assertEquals(2, results.length);
		assertEquals(expected[0], results[0]);
		assertEquals(expected[1], results[1]);
	}
	
	@Test
	public void testBuildEnvironmentsWithoutTTL() throws InterruptedException {

		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("BEANSTALK");
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
		verify(builderSpy).buildAndDeployStack(mockContext, e1.getName(), TEMPLATE_BEANSTALK_ENVIRONMENT, null);
		verify(builderSpy).buildAndDeployStack(mockContext, e2.getName(), TEMPLATE_BEANSTALK_ENVIRONMENT, null);
	}

	@Test
	public void testBuildEnvironmentsWithTTL() throws InterruptedException {

		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("BEANSTALK");
		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		Parameter ttl = Parameter.builder().parameterKey("ttl").parameterValue("value").build();
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
		verify(builderSpy).buildAndDeployStack(mockContext, e1.getName(), TEMPLATE_BEANSTALK_ENVIRONMENT, ttl);
		verify(builderSpy).buildAndDeployStack(mockContext, e2.getName(), TEMPLATE_BEANSTALK_ENVIRONMENT, ttl);
	}
	
	@Test
	public void testCreateEcsEnvironments() {

		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getIntegerProperty(PROPERTY_KEY_ECS_TASK_CPU)).thenReturn(1024);
		when(config.getIntegerProperty(PROPERTY_KEY_ECS_TASK_MEMORY)).thenReturn(4096);
		when(config.getIntegerProperty(PROPERTY_KEY_ECS_CONTAINER_PORT)).thenReturn(8443);

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

		when(mockDockerImageBuilder.buildAndPushImage(any(), any(), anyInt(), any(), any())).thenReturn("123456.dkr.ecr.us-east-1.amazonaws.com/dev-synapse-repo:dev-101-version-repo-0");

		// call under test
		List<EcsEnvironmentDescriptor> descriptors = builder.createEcsEnvironments(secretsSouce);
		assertNotNull(descriptors);
		assertEquals(3, descriptors.size());
		// repo
		EcsEnvironmentDescriptor desc = descriptors.get(0);
		assertEquals("repo", desc.getType());
		assertEquals("repo-dev-101-0", desc.getName());
		assertEquals("RepoDev1010", desc.getRefName());
		assertEquals("url-repo", desc.getHealthCheckUrl());
		assertEquals(1, desc.getMinTasks());
		assertEquals(2, desc.getMaxTasks());
		assertEquals(1024, desc.getCpu());
		assertEquals(4096, desc.getMemory());
		assertEquals(8443, desc.getContainerPort());
		assertEquals("synapes.org", desc.getHostedZone());
		assertEquals("the:ssl:arn", desc.getSslCertificateARN());
		// secrets should be passed to repo
		assertEquals(secretsSouce, desc.getSecretsSource());

		// workers
		desc = descriptors.get(1);
		assertEquals("workers", desc.getType());
		assertEquals("workers-dev-101-0", desc.getName());
		assertEquals("WorkersDev1010", desc.getRefName());
		assertEquals("url-workers", desc.getHealthCheckUrl());
		// secrets should be passed to workers
		assertEquals(secretsSouce, desc.getSecretsSource());

		// portal
		desc = descriptors.get(2);
		assertEquals("portal", desc.getType());
		assertEquals("portal-dev-101-0", desc.getName());
		assertEquals("PortalDev1010", desc.getRefName());
		assertEquals("url-portal", desc.getHealthCheckUrl());
		// empty secrets should be passed to portal
		assertEquals(null, desc.getSecretsSource());
	}

	@Test
	public void testCreateEcsEnvironmentContext() {

		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);

		when(config.getProperty((PROPERTY_KEY_OAUTH_ENDPOINT))).thenReturn("https://oauthendpoint");

		when(config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + EnvironmentType.REPOSITORY_SERVICES.getShortName())).thenReturn(0);

		when(mockSecretBuilder.getCMKAlias()).thenReturn(keyAlias);

		when(mockCwlContextProvider.getLogDescriptors(any(EnvironmentType.class))).thenReturn(logDescriptors);

		List<String> EXPECTED_SUBNETS = Arrays.asList("subnet1", "subnet2", "subnet4");
		when(mockCloudFormationClientWrapper.getOutput(anyString(), anyString()))
				.thenReturn(String.join(",", EXPECTED_SUBNETS));
		when(config.getProperty("org.sagebionetworks.cloudfront.private.key.id")).thenReturn("dataCdnPrivateKeyId");
		when(config.getProperty("org.sagebionetworks.oauth2.sagebio.discoveryDocument")).thenReturn("discoveryDocumentUrl");
		when(mockLoadBalancerAlarmsConfig.getOrDefault(any(), any())).thenReturn(java.util.Collections.emptyList());

		EcsEnvironmentDescriptor environment = new EcsEnvironmentDescriptor()
				.withType(EnvironmentType.REPOSITORY_SERVICES)
				.withName("repo-dev-101-0")
				.withRefName("RepoDev1010")
				.withNumber(0)
				.withCpu(1024).withMemory(4096).withContainerPort(8443);

		// call under test
		VelocityContext context = builder.createEcsEnvironmentContext(sharedResouces, environment);

		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("us-east-1-synapse-dev-vpc-2", context.get(VPC_EXPORT_PREFIX));
		assertEquals("us-east-1-dev-101-shared-resources", context.get(SHARED_EXPORT_PREFIX));
		assertEquals("us-east-1-synapse-dev-global-resources", context.get(GLOBAL_RESOURCES_EXPORT_PREFIX));
		assertEquals(environment, context.get(ENVIRONMENT));
		assertEquals(0, context.get(REPO_BEANSTALK_NUMBER));
		assertEquals(keyAlias, context.get(STACK_CMK_ALIAS));
		assertEquals(databaseEndpointSuffix, context.get(DB_ENDPOINT_SUFFIX));
		assertEquals("https://oauthendpoint", context.get(OAUTH_ENDPOINT));
		assertEquals("data.dev.sagebase.org", context.get(CTXT_KEY_DATA_CDN_DOMAIN_NAME));
		assertEquals("dataCdnPrivateKeyId", context.get(CTXT_KEY_DATA_CDN_PRIVATE_KEY_ID));
		assertEquals("discoveryDocumentUrl", context.get(CTXT_KEY_DATA_DISCOVERY_DOCUMENT_URL));
		assertEquals(EXPECTED_SUBNETS, context.get("ecsSubnetsList"));
		assertNotNull(context.get("targetGroup"));
		assertNotNull(context.get(CLOUDWATCH_LOGS_DESCRIPTORS));
		assertNotNull(context.get(LOAD_BALANCER_ALARMS));
	}

	@Test
	public void testBuildEcsEnvironmentsWithoutTTL() throws InterruptedException {

		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("ECS_FARGATE");
		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		when(mockTimeToLive.createTimeToLiveParameter()).thenReturn(Optional.empty());

		EcsEnvironmentDescriptor e1 = new EcsEnvironmentDescriptor().withName("repo");
		EcsEnvironmentDescriptor e2 = new EcsEnvironmentDescriptor().withName("portal");
		doReturn(List.of(e1, e2)).when(builderSpy).createEcsEnvironments(any());

		VelocityContext mockContext = Mockito.mock(VelocityContext.class);
		doReturn(mockContext).when(builderSpy).createEcsEnvironmentContext(any(), any());

		doNothing().when(builderSpy).buildAndDeployStack(any(), any(), any(), any());
		when(mockCloudFormationClientWrapper.waitForStackToComplete(anyString())).thenReturn(Optional.of(sharedResouces));

		// call under test
		builderSpy.buildEnvironments(sharedResouces);

		verify(mockSecretBuilder).createSecrets();
		verify(mockTimeToLive).createTimeToLiveParameter();
		verify(builderSpy).createEcsEnvironments(secretsSouce);
		verify(builderSpy, times(2)).buildAndDeployStack(any(), any(), any(), any());
		verify(builderSpy).buildAndDeployStack(mockContext, e1.getName(), TEMPLATE_ECS_FARGATE_ENVIRONMENT, null);
		verify(builderSpy).buildAndDeployStack(mockContext, e2.getName(), TEMPLATE_ECS_FARGATE_ENVIRONMENT, null);
		// ECS waits for stacks to complete
		verify(mockCloudFormationClientWrapper).waitForStackToComplete("repo");
		verify(mockCloudFormationClientWrapper).waitForStackToComplete("portal");
	}

	@Test
	public void testBuildEcsEnvironmentsWithTTL() throws InterruptedException {

		when(config.getProperty(Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS)).thenReturn("ECS_FARGATE");
		when(mockSecretBuilder.createSecrets()).thenReturn(secretsSouce);
		Parameter ttl = Parameter.builder().parameterKey("ttl").parameterValue("value").build();
		when(mockTimeToLive.createTimeToLiveParameter()).thenReturn(Optional.of(ttl));

		EcsEnvironmentDescriptor e1 = new EcsEnvironmentDescriptor().withName("repo");
		EcsEnvironmentDescriptor e2 = new EcsEnvironmentDescriptor().withName("portal");
		doReturn(List.of(e1, e2)).when(builderSpy).createEcsEnvironments(any());

		VelocityContext mockContext = Mockito.mock(VelocityContext.class);
		doReturn(mockContext).when(builderSpy).createEcsEnvironmentContext(any(), any());

		doNothing().when(builderSpy).buildAndDeployStack(any(), any(), any(), any());
		when(mockCloudFormationClientWrapper.waitForStackToComplete(anyString())).thenReturn(Optional.of(sharedResouces));

		// call under test
		builderSpy.buildEnvironments(sharedResouces);

		verify(mockSecretBuilder).createSecrets();
		verify(mockTimeToLive).createTimeToLiveParameter();
		verify(builderSpy).createEcsEnvironments(secretsSouce);
		verify(builderSpy, times(2)).buildAndDeployStack(any(), any(), any(), any());
		verify(builderSpy).buildAndDeployStack(mockContext, e1.getName(), TEMPLATE_ECS_FARGATE_ENVIRONMENT, ttl);
		verify(builderSpy).buildAndDeployStack(mockContext, e2.getName(), TEMPLATE_ECS_FARGATE_ENVIRONMENT, ttl);
		// ECS waits for stacks to complete
		verify(mockCloudFormationClientWrapper).waitForStackToComplete("repo");
		verify(mockCloudFormationClientWrapper).waitForStackToComplete("portal");
	}

	private void setupValidBeanstalkConfig() {
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA)).thenReturn("11");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT)).thenReturn("9.0");
		when(config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX)).thenReturn("3.4.7");
		String expectedPlatformName = "Tomcat 9.0 with Corretto 11 running on 64bit Amazon Linux 2023";
		PlatformFilter expectedFilter = PlatformFilter.builder().type("PlatformName").operator("=").values(expectedPlatformName).build();
		ListPlatformVersionsRequest expectedRequest = ListPlatformVersionsRequest.builder().filters(expectedFilter).build();
		List<PlatformSummary> expectedSummaries = new LinkedList<>();
		PlatformSummary summary = PlatformSummary.builder().platformVersion("3.4.7").build();
		expectedSummaries.add(summary);
		ListPlatformVersionsResponse expectedResult = ListPlatformVersionsResponse.builder().platformSummaryList(expectedSummaries).build();
		when(mockBeanstalkClient.listPlatformVersions(expectedRequest)).thenReturn(expectedResult);
	}
}
