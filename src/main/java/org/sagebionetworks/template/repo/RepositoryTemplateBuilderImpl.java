package org.sagebionetworks.template.repo;


import static org.sagebionetworks.template.Constants.ADMIN_RULE_ACTION;
import static org.sagebionetworks.template.Constants.BEANSTALK_INSTANCES_SUBNETS;
import static org.sagebionetworks.template.Constants.CLOUDWATCH_LOGS_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.CLOUDWATCH_LOG_RETENTION_DAYS;
import static org.sagebionetworks.template.Constants.CTXT_ENABLE_ENHANCED_RDS_MONITORING;
import static org.sagebionetworks.template.Constants.CTXT_KEY_DATA_CDN_DOMAIN_NAME;
import static org.sagebionetworks.template.Constants.CTXT_KEY_DATA_CDN_PRIVATE_KEY_ID;
import static org.sagebionetworks.template.Constants.CTXT_KEY_DATA_DISCOVERY_DOCUMENT_URL;
import static org.sagebionetworks.template.Constants.DATABASE_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.DATA_CDN_DOMAIN_NAME_FMT;
import static org.sagebionetworks.template.Constants.DB_ENDPOINT_SUFFIX;
import static org.sagebionetworks.template.Constants.DELETION_POLICY;
import static org.sagebionetworks.template.Constants.DEPLOYMENT_TARGET;
import static org.sagebionetworks.template.Constants.EC2_INSTANCE_MEMORY;
import static org.sagebionetworks.template.Constants.EC2_INSTANCE_TYPE;
import static org.sagebionetworks.template.Constants.ENVIRONMENT;
import static org.sagebionetworks.template.Constants.EXCEPTION_THROWER;
import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.LOAD_BALANCER_ALARMS;
import static org.sagebionetworks.template.Constants.LOG_RETENTION_IN_DAYS;
import static org.sagebionetworks.template.Constants.MACHINE_TYPES;
import static org.sagebionetworks.template.Constants.NOSNAPSHOT;
import static org.sagebionetworks.template.Constants.OAUTH_ENDPOINT;
import static org.sagebionetworks.template.Constants.OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT;
import static org.sagebionetworks.template.Constants.PARAMETER_MYSQL_PASSWORD;
import static org.sagebionetworks.template.Constants.POOL_TYPES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MAX_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MIN_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_NUMBER;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_SSL_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_VERSION;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DATA_CDN_PRIVATE_KEY_ID;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_EC2_INSTANCE_MEMORY;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_EC2_INSTANCE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OPENSEARCH_INSTANCE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OPENSEARCH_MASTER_INSTANCE_TYPE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_OPENSEARCH_AVAILABILITY_ZONE_COUNT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ECS_CONTAINER_PORT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ECS_TASK_CPU;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ECS_TASK_MEMORY;
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
import static org.sagebionetworks.template.Constants.SOLUTION_STACK_NAME;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.STACK_CMK_ALIAS;
import static org.sagebionetworks.template.Constants.TEMPLATE_BEANSTALK_ENVIRONMENT;
import static org.sagebionetworks.template.Constants.TEMPLATE_ECS_FARGATE_ENVIRONMENT;
import static org.sagebionetworks.template.Constants.TEMPLATE_SHARED_RESOURCES_MAIN_JSON_VTP;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.VPC_SUBNET_COLOR;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.Ec2ClientWrapper;
import org.sagebionetworks.template.ImageBuilderClient;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.config.TimeToLive;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopy;
import org.sagebionetworks.template.repo.beanstalk.BeanstalkUtils;
import org.sagebionetworks.template.repo.beanstalk.ElasticBeanstalkSolutionStackNameProvider;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentDescriptor;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.LoadBalancerAlarm;
import org.sagebionetworks.template.repo.beanstalk.LoadBalancerAlarmsConfig;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;
import org.sagebionetworks.template.repo.beanstalk.ssl.TargetGroup;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsVelocityContextProvider;
import org.sagebionetworks.template.repo.ecs.DockerImageBuilder;
import org.sagebionetworks.template.repo.ecs.EcsEnvironmentDescriptor;

import com.google.inject.Inject;

import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient;
import software.amazon.awssdk.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.ListPlatformVersionsResponse;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformSummary;

public class RepositoryTemplateBuilderImpl implements RepositoryTemplateBuilder {
	public static final List<String> MACHINE_TYPE_LIST = List.of("Workers", "Repository");
	public static final List<String> POOL_TYPE_LIST = List.of("Idgen", "Main", "Migration", "Tables");

	private final CloudFormationClientWrapper cloudFormationClientWrapper;
	private final Ec2ClientWrapper ec2ClientWrapper;
	private final VelocityEngine velocityEngine;
	private final RepoConfiguration config;
	private final Logger logger;
	private final ArtifactCopy artifactCopy;
	private final SecretBuilder secretBuilder;
	private final Set<VelocityContextProvider> contextProviders;
	private final ElasticBeanstalkSolutionStackNameProvider elasticBeanstalkSolutionStackNameProvider;
	private final StackTagsProvider stackTagsProvider;
	private final CloudwatchLogsVelocityContextProvider cwlContextProvider;
	private final ElasticBeanstalkClient beanstalkClient;
	private final ImageBuilderClient imageBuilderClient;
	private final TimeToLive timeToLive;
	private final DockerImageBuilder dockerImageBuilder;
	private final LoadBalancerAlarmsConfig loadBalancerAlarmsConfig;

	@Inject
	public RepositoryTemplateBuilderImpl(CloudFormationClientWrapper cloudFormationClientWrapper, VelocityEngine velocityEngine,
                                         RepoConfiguration configuration, LoggerFactory loggerFactory, ArtifactCopy artifactCopy,
                                         SecretBuilder secretBuilder, Set<VelocityContextProvider> contextProviders,
                                         ElasticBeanstalkSolutionStackNameProvider elasticBeanstalkDefaultAMIEncrypter,
                                         StackTagsProvider stackTagsProvider, CloudwatchLogsVelocityContextProvider cloudwatchLogsVelocityContextProvider,
                                         Ec2ClientWrapper ec2ClientWrapper, ElasticBeanstalkClient beanstalkClient, ImageBuilderClient imageBuilderClient, TimeToLive ttl,
                                         DockerImageBuilder dockerImageBuilder, LoadBalancerAlarmsConfig loadBalancerAlarmsConfig) {
		super();
		this.cloudFormationClientWrapper = cloudFormationClientWrapper;
		this.ec2ClientWrapper = ec2ClientWrapper;
		this.velocityEngine = velocityEngine;
		this.config = configuration;
		this.logger = loggerFactory.getLogger(RepositoryTemplateBuilderImpl.class);
		this.artifactCopy = artifactCopy;
		this.secretBuilder = secretBuilder;
		this.contextProviders = contextProviders;
		this.elasticBeanstalkSolutionStackNameProvider = elasticBeanstalkDefaultAMIEncrypter;
		this.stackTagsProvider = stackTagsProvider;
		this.cwlContextProvider = cloudwatchLogsVelocityContextProvider;
		this.beanstalkClient = beanstalkClient;
		this.imageBuilderClient = imageBuilderClient;
		this.timeToLive = ttl;
		this.dockerImageBuilder = dockerImageBuilder;
		this.loadBalancerAlarmsConfig = loadBalancerAlarmsConfig;
	}

	public String getActualBeanstalkAmazonLinuxPlatform() {
		final String LATEST = "latest";	// default is to request latest version
		// Check AWS Beanstalk current platform vs what we have in config
		String javaVersion = config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA);
		String tomcatVersion = config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT);
		String requestedPlatformVersion = config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX);
		ListPlatformVersionsRequest lpvReq = BeanstalkUtils.buildListPlatformVersionsRequest(javaVersion, tomcatVersion, null);
		ListPlatformVersionsResponse lpvRes = this.beanstalkClient.listPlatformVersions(lpvReq);
		List<PlatformSummary> summaries = lpvRes.platformSummaryList();
		String latestPlatformVersion = BeanstalkUtils.getLatestPlatformVersion(summaries);
		String actualVersion = requestedPlatformVersion;
		if (LATEST.equals(requestedPlatformVersion)) {
			actualVersion = latestPlatformVersion;
		} else {
			if (! latestPlatformVersion.equals(actualVersion)) { // The version specified is not the latest, log
				logger.info(String.format("The latest platform version is %s. Please update the default configuration.", latestPlatformVersion));
			}
		}
		return actualVersion;
	}

	@Override
	public void buildAndDeploy() throws InterruptedException {

		// Create the context from the input
		VelocityContext context = createSharedContext();

		Parameter[] sharedParameters = createSharedParameters();
		// Create the shared-resource stack
		String sharedResourceStackName = createSharedResourcesStackName();

		buildAndDeployStack(context, sharedResourceStackName, TEMPLATE_SHARED_RESOURCES_MAIN_JSON_VTP, sharedParameters);
		// Wait for the shared resources to complete
		Stack sharedStackResults = cloudFormationClientWrapper.waitForStackToComplete(sharedResourceStackName).orElseThrow(()->new IllegalStateException("Stack does not exist: "+sharedResourceStackName));
				
		// Build each bean stalk environment.
		List<String> environmentNames = buildEnvironments(sharedStackResults);
	}
	
	/**
	 * Build all of the environments
	 * @param sharedStackResults
	 */
	public List<String> buildEnvironments(Stack sharedStackResults) throws InterruptedException {
		// Create the repo/worker secrets
		SourceBundle secretsSource = secretBuilder.createSecrets();

		DeploymentTarget target = DeploymentTarget.valueOf(
				config.getProperty(PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS));

		switch (target) {
			case ECS_FARGATE:
				return buildEcsEnvironments(sharedStackResults, secretsSource);
			case BEANSTALK:
				return buildBeanstalkEnvironments(sharedStackResults, secretsSource);
			default:
				throw new IllegalArgumentException("Unknown deployment target: " + target);
		}
	}

	/**
	 * Build Beanstalk environments (existing behavior).
	 */
	List<String> buildBeanstalkEnvironments(Stack sharedStackResults, SourceBundle secrets) {
		Parameter ttl = timeToLive.createTimeToLiveParameter().orElse(null);

		List<String> environmentNames = new LinkedList<>();
		for (EnvironmentDescriptor environment : createEnvironments(secrets)) {
			VelocityContext context = createEnvironmentContext(sharedStackResults, environment);
			environmentNames.add(environment.getName());
			buildAndDeployStack(context, environment.getName(), TEMPLATE_BEANSTALK_ENVIRONMENT, ttl);
		}
		return environmentNames;
	}

	/**
	 * Build ECS Fargate environments and wait for all to complete.
	 */
	List<String> buildEcsEnvironments(Stack sharedStackResults, SourceBundle secrets) throws InterruptedException {
		Parameter ttl = timeToLive.createTimeToLiveParameter().orElse(null);

		List<String> environmentNames = new LinkedList<>();
		// Submit all environment stacks
		for (EcsEnvironmentDescriptor environment : createEcsEnvironments(secrets)) {
			VelocityContext context = createEcsEnvironmentContext(sharedStackResults, environment);
			environmentNames.add(environment.getName());
			buildAndDeployStack(context, environment.getName(), TEMPLATE_ECS_FARGATE_ENVIRONMENT, ttl);
		}
		// Wait for all environment stacks to complete
		for (String stackName : environmentNames) {
			cloudFormationClientWrapper.waitForStackToComplete(stackName)
					.orElseThrow(() -> new IllegalStateException("Stack does not exist: " + stackName));
		}
		return environmentNames;
	}

	/**
	 * Create ECS environment descriptors for repo, workers, and portal.
	 */
	public List<EcsEnvironmentDescriptor> createEcsEnvironments(SourceBundle secrets) {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		int cpu = config.getIntegerProperty(PROPERTY_KEY_ECS_TASK_CPU);
		int memory = config.getIntegerProperty(PROPERTY_KEY_ECS_TASK_MEMORY);
		int containerPort = config.getIntegerProperty(PROPERTY_KEY_ECS_CONTAINER_PORT);

		List<EcsEnvironmentDescriptor> descriptors = new LinkedList<>();
		for (EnvironmentType type : EnvironmentType.values()) {
			try {
				int number = config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + type.getShortName());
				String name = new StringJoiner("-").add(type.getShortName()).add(stack).add(instance).add("" + number)
						.toString();
				String refName = Constants.createCamelCaseName(name, "-");
				String version = config.getProperty(PROPERTY_KEY_BEANSTALK_VERSION + type.getShortName());
				String healthCheckUrl = config.getProperty(PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL + type.getShortName());
				int minTasks = config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MIN_INSTANCES + type.getShortName());
				int maxTasks = config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MAX_INSTANCES + type.getShortName());
				String sslCertificateARN = config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN + type.getShortName());
				String hostedZone = config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE + type.getShortName());

				// Build and push Docker image
				String dockerImageUri = dockerImageBuilder.buildAndPushImage(type, version, number, stack, instance);

				// Environment secrets
				SourceBundle environmentSecrets = type.shouldIncludeSecrets() ? secrets : null;

				descriptors.add(new EcsEnvironmentDescriptor()
						.withName(name).withRefName(refName).withNumber(number)
						.withType(type)
						.withDockerImageUri(dockerImageUri)
						.withHealthCheckUrl(healthCheckUrl)
						.withMinTasks(minTasks).withMaxTasks(maxTasks)
						.withCpu(cpu).withMemory(memory).withContainerPort(containerPort)
						.withSslCertificateARN(sslCertificateARN)
						.withHostedZone(hostedZone)
						.withSecretsSource(environmentSecrets));
			} catch (ConfigurationPropertyNotFound e) {
				logger.warn("The ECS Environment " + type + " was not created because " + e.getMissingKey() + " was not found");
			}
		}
		return descriptors;
	}

	/**
	 * Create the Velocity context for an ECS Fargate environment.
	 */
	VelocityContext createEcsEnvironmentContext(Stack sharedStackResults, EcsEnvironmentDescriptor environment) {
		VelocityContext context = new VelocityContext();
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		context.put(STACK, stack);
		context.put(INSTANCE, instance);
		context.put(VPC_SUBNET_COLOR, config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR));
		context.put(GLOBAL_RESOURCES_EXPORT_PREFIX, Constants.createGlobalResourcesExportPrefix(stack));
		context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
		context.put(SHARED_EXPORT_PREFIX, createSharedExportPrefix());
		context.put(REPO_BEANSTALK_NUMBER, config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + EnvironmentType.REPOSITORY_SERVICES.getShortName()));
		context.put(DB_ENDPOINT_SUFFIX, extractDatabaseSuffix(sharedStackResults));
		context.put(ENVIRONMENT, environment);
		context.put(STACK_CMK_ALIAS, secretBuilder.getCMKAlias());

		// oauth
		context.put(OAUTH_ENDPOINT, config.getProperty(PROPERTY_KEY_OAUTH_ENDPOINT));

		// Data CDN props
		String cdnPrivateKeyId = config.getProperty(PROPERTY_KEY_DATA_CDN_PRIVATE_KEY_ID);
		context.put(CTXT_KEY_DATA_CDN_PRIVATE_KEY_ID, cdnPrivateKeyId);
		context.put(CTXT_KEY_DATA_CDN_DOMAIN_NAME, String.format(DATA_CDN_DOMAIN_NAME_FMT, stack));
		context.put(CTXT_KEY_DATA_DISCOVERY_DOCUMENT_URL, config.getProperty(SAGEBIO_COGNITO_APP_DISCOVERY_DOCUMENT));

		// Subnets for ECS tasks and ALB
		List<String> vpcSubnets = getPrivateSubnets(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR));
		context.put("ecsSubnetsList", vpcSubnets);

		// Target group for ALB ARN export (must match the naming used by dns-record-to-stack-mapping)
		int number = environment.getNumber();
		TargetGroup targetGroup = new TargetGroup(environment.getEnvironmentType(), stack, instance, number);
		context.put("targetGroup", targetGroup);

		// CloudWatch log descriptors for sidecar containers
		EnvironmentType envType = environment.getEnvironmentType();
		context.put(CLOUDWATCH_LOGS_DESCRIPTORS, cwlContextProvider.getLogDescriptors(envType));
		context.put(CLOUDWATCH_LOG_RETENTION_DAYS, LOG_RETENTION_IN_DAYS);

		// Load balancer alarms
		java.util.List<LoadBalancerAlarm> alarms = loadBalancerAlarmsConfig.getOrDefault(envType, java.util.Collections.emptyList());
		context.put(LOAD_BALANCER_ALARMS, alarms);

		return context;
	}
	

	/**
	 * Create the context used for each environment
	 * @param sharedStackResults
	 * @param environment 
	 * 
	 * @return
	 */
	VelocityContext createEnvironmentContext(Stack sharedStackResults, EnvironmentDescriptor environment) {
		VelocityContext context = new VelocityContext();
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		context.put(STACK, stack);
		context.put(INSTANCE, config.getProperty(PROPERTY_KEY_INSTANCE));
		context.put(VPC_SUBNET_COLOR, config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR));
		context.put(GLOBAL_RESOURCES_EXPORT_PREFIX, Constants.createGlobalResourcesExportPrefix(stack));
		context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
		context.put(SHARED_EXPORT_PREFIX, createSharedExportPrefix());
		context.put(REPO_BEANSTALK_NUMBER, config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + EnvironmentType.REPOSITORY_SERVICES.getShortName()));
		// Extract the database suffix
		context.put(DB_ENDPOINT_SUFFIX, extractDatabaseSuffix(sharedStackResults));
		context.put(ENVIRONMENT, environment);
		context.put(STACK_CMK_ALIAS, secretBuilder.getCMKAlias());

		//use encrypted copies of the default elasticbeanstalk AMI
		String javaVersion = config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA);
		String tomcatVersion = config.getProperty(PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT);
		String linuxVersion = getActualBeanstalkAmazonLinuxPlatform();
		String solutionStackName = elasticBeanstalkSolutionStackNameProvider.getSolutionStackName(tomcatVersion, javaVersion, linuxVersion);
		context.put(SOLUTION_STACK_NAME, solutionStackName);

		// oauth
		context.put(OAUTH_ENDPOINT, config.getProperty(PROPERTY_KEY_OAUTH_ENDPOINT));

		// CloudwatchLogs
		context.put(CLOUDWATCH_LOGS_DESCRIPTORS, cwlContextProvider.getLogDescriptors(EnvironmentType.valueOfPrefix(environment.getType())));
		context.put(CLOUDWATCH_LOG_RETENTION_DAYS, LOG_RETENTION_IN_DAYS);

		// EC2 instance type and memory
		String ec2InstanceType = config.getProperty(PROPERTY_KEY_EC2_INSTANCE_TYPE);
		context.put(EC2_INSTANCE_TYPE, ec2InstanceType);
		context.put(EC2_INSTANCE_MEMORY, config.getIntegerProperty(PROPERTY_KEY_EC2_INSTANCE_MEMORY));
		
		// Determine Beanstalk subnets for instances
		List<String> vpcSubnets = getPrivateSubnets(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR));
		List<String> beanstalkSubnets = ec2ClientWrapper.getAvailableSubnetsForInstanceType(ec2InstanceType, vpcSubnets);
		String beanstalkSubnetsAsString = String.join(",", beanstalkSubnets);
		context.put(BEANSTALK_INSTANCES_SUBNETS, beanstalkSubnetsAsString);

		// Data CDN props (
		String cdnPrivateKeyId = config.getProperty(PROPERTY_KEY_DATA_CDN_PRIVATE_KEY_ID);
		context.put(CTXT_KEY_DATA_CDN_PRIVATE_KEY_ID, cdnPrivateKeyId);
		context.put(CTXT_KEY_DATA_CDN_DOMAIN_NAME, String.format(DATA_CDN_DOMAIN_NAME_FMT, stack));

		context.put(CTXT_KEY_DATA_DISCOVERY_DOCUMENT_URL, config.getProperty(SAGEBIO_COGNITO_APP_DISCOVERY_DOCUMENT));

		return context;
	}

	/**
	 * Build and deploy a stack using the provided context and template.
	 * 
	 * @param context
	 * @param stackName
	 * @param templatePath
	 */
	void buildAndDeployStack(VelocityContext context, String stackName, String templatePath, Parameter... parameters) {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		boolean enableTerminationProtection = ("prod".equals(stack)); // enable on prod stack
		List<Tag> stackTags = stackTagsProvider.getStackTags(config);

		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate(templatePath);
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String resultJSON = stringWriter.toString();
		JSONObject templateJson = new JSONObject(resultJSON);
		// Format the JSON
		resultJSON = templateJson.toString(JSON_INDENT);
		this.logger.info("Template for stack: " + stackName);
		this.logger.info(resultJSON);
		// create or update the template
		this.cloudFormationClientWrapper.createOrUpdateStack(new CreateOrUpdateStackRequest()
				.withStackName(stackName)
				.withTemplateBody(resultJSON)
				.withParameters(parameters)
				.withCapabilities(Capability.CAPABILITY_NAMED_IAM)
				.withTags(stackTags)
				.withEnableTerminationProtection(enableTerminationProtection));
	}

	/**
	 * Create the template context.
	 * 
	 * @return
	 */
	VelocityContext createSharedContext() {
		VelocityContext context = new VelocityContext();
		
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		
		context.put(STACK, stack);
		context.put(INSTANCE, config.getProperty(PROPERTY_KEY_INSTANCE));
		context.put(MACHINE_TYPES, MACHINE_TYPE_LIST);
		context.put(POOL_TYPES, POOL_TYPE_LIST);
		context.put(VPC_SUBNET_COLOR, config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR));
		context.put(SHARED_RESOURCES_STACK_NAME, createSharedResourcesStackName());
		context.put(GLOBAL_RESOURCES_EXPORT_PREFIX, Constants.createGlobalResourcesExportPrefix(stack));
		context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
		context.put(SHARED_EXPORT_PREFIX, createSharedExportPrefix());
		context.put(EXCEPTION_THROWER, new VelocityExceptionThrower());
		context.put(CTXT_ENABLE_ENHANCED_RDS_MONITORING, config.getProperty(PROPERTY_KEY_ENABLE_RDS_ENHANCED_MONITORING));
		
		context.put(ADMIN_RULE_ACTION, Constants.isProd(stack) ? "Block:{}" : "Count:{}");
		context.put(DELETION_POLICY, Constants.isProd(stack) ? DeletionPolicy.Retain.name() : DeletionPolicy.Delete.name());
		
		// Create the descriptors for all of the database.
		context.put(DATABASE_DESCRIPTORS, createDatabaseDescriptors());

		context.put(CLOUDWATCH_LOG_RETENTION_DAYS, LOG_RETENTION_IN_DAYS);

		for(VelocityContextProvider provider : contextProviders){
			provider.addToContext(context);
		}
		
		RegularExpressions.bindRegexToContext(context);

		// Deployment target for conditional resources in shared template
		String deploymentTarget = config.getProperty(PROPERTY_KEY_DEPLOYMENT_BEANSTALK_OR_ECS);
		context.put(DEPLOYMENT_TARGET, deploymentTarget);

		// The prod OpenSearch domain is VPC-attached and pinned to specific data and dedicated
		// master instance types, which are not offered in every AZ. Resolve the color's private
		// subnets and keep only those in AZs that offer both types, so the domain is never placed
		// in an AZ that cannot host one of its node types.
		if (Constants.isProd(stack)) {
			String openSearchInstanceType = config.getProperty(PROPERTY_KEY_OPENSEARCH_INSTANCE_TYPE);
			String openSearchMasterInstanceType = config.getProperty(PROPERTY_KEY_OPENSEARCH_MASTER_INSTANCE_TYPE);
			int openSearchAvailabilityZoneCount = config.getIntegerProperty(PROPERTY_KEY_OPENSEARCH_AVAILABILITY_ZONE_COUNT);
			context.put(Constants.OPENSEARCH_INSTANCE_TYPE, openSearchInstanceType);
			context.put(Constants.OPENSEARCH_MASTER_INSTANCE_TYPE, openSearchMasterInstanceType);
			context.put(Constants.OPENSEARCH_AVAILABILITY_ZONE_COUNT, openSearchAvailabilityZoneCount);
			List<String> vpcSubnets = getPrivateSubnets(config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR));
			List<String> availableSubnets = ec2ClientWrapper.getAvailableSubnetsForInstanceTypes(
					List.of(openSearchInstanceType, openSearchMasterInstanceType), vpcSubnets, openSearchAvailabilityZoneCount);
			context.put(Constants.OPENSEARCH_SUBNETS, availableSubnets.subList(0, openSearchAvailabilityZoneCount));
		}

		return context;
	}

	/**
	 * Create a descriptor for each database to be created.
	 * 
	 * @return
	 */
	public DatabaseDescriptor[] createDatabaseDescriptors() {
		int numberOfTablesDatabase = config.getIntegerProperty(PROPERTY_KEY_TABLES_INSTANCE_COUNT);
		// one repository database and multiple tables database.
		DatabaseDescriptor[] results = new DatabaseDescriptor[numberOfTablesDatabase + 1];

		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);

		// Describe the repository database.
		DatabaseDescriptor repoDbDescriptor = new DatabaseDescriptor().withResourceName(stack + instance + "RepositoryDB")
				.withAllocatedStorage(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE))
				.withMaxAllocatedStorage(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_MAX_ALLOCATED_STORAGE))
				.withInstanceIdentifier(stack + "-" + instance + "-db").withDbName(stack + instance)
				.withInstanceClass(config.getProperty(PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS))
				.withDbStorageType(config.getProperty(PROPERTY_KEY_REPO_RDS_STORAGE_TYPE))
				.withDbIops(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_IOPS))
				.withDbThroughput(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_THROUGHPUT))
				.withMultiAZ(config.getBooleanProperty(PROPERTY_KEY_REPO_RDS_MULTI_AZ))
				// 0 indicates no automated backups will be created.
				.withBackupRetentionPeriodDays(7)
				.withDeletionPolicy(Constants.isProd(stack)? DeletionPolicy.Snapshot: DeletionPolicy.Delete);
		

		String repoSnapshotIdentifier = config.getProperty(PROPERTY_KEY_RDS_REPO_SNAPSHOT_IDENTIFIER);
		boolean useSnapshotForRepoDB = ! NOSNAPSHOT.equals(repoSnapshotIdentifier);
		if (useSnapshotForRepoDB) {
			repoDbDescriptor = repoDbDescriptor.withSnapshotIdentifier(repoSnapshotIdentifier);
		}
		results[0] = repoDbDescriptor;

		String[] repoTableSnapshotIdentifiers = config.getCommaSeparatedProperty(PROPERTY_KEY_RDS_TABLES_SNAPSHOT_IDENTIFIERS);
		boolean useSnapshotsForTablesDbs = !(repoTableSnapshotIdentifiers.length == 1 && NOSNAPSHOT.equals(repoTableSnapshotIdentifiers[0]));
		if (useSnapshotForRepoDB != useSnapshotsForTablesDbs) {
			throw new IllegalStateException("The repo database is set to use a snapshot but the tables database are not set to use snapshots, or vice-versa");
		}

		// Describe each table database
		for (int i = 0; i < numberOfTablesDatabase; i++) {
			DatabaseDescriptor tableDbDescriptor = new DatabaseDescriptor()
				.withResourceName(stack + instance + "Table" + i + "RepositoryDB")
				.withAllocatedStorage(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE))
				.withMaxAllocatedStorage(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE))
				.withInstanceIdentifier(stack + "-" + instance + "-table-" + i).withDbName(stack + instance)
				.withDbStorageType(config.getProperty(PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE))
				.withDbIops(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_IOPS))
				.withDbThroughput(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_THROUGHPUT))
				.withInstanceClass(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).withMultiAZ(false)
				// 0 indicates no automated backups will be created.
				.withBackupRetentionPeriodDays(Constants.isProd(stack)? 1 : 0)
				.withDeletionPolicy(Constants.isProd(stack)? DeletionPolicy.Snapshot: DeletionPolicy.Delete);
			if (useSnapshotForRepoDB) {
				String snapshotIdentifier = repoTableSnapshotIdentifiers[i];
				tableDbDescriptor = tableDbDescriptor.withSnapshotIdentifier(snapshotIdentifier);
			}
			results[i+1] = tableDbDescriptor;
		}
		return results;
	}

	/**
	 * Create an Environment descriptor for reop, workers, and portal if they are defined.
	 * @param secrets 
	 * 
	 * @return
	 */
	public List<EnvironmentDescriptor> createEnvironments(SourceBundle secrets) {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		List<EnvironmentDescriptor> environmentDescriptors = new LinkedList<>();
		// create each type.
		for (EnvironmentType type : EnvironmentType.values()) {
			try {
				int number = config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + type.getShortName());
				String name = new StringJoiner("-").add(type.getShortName()).add(stack).add(instance).add("" + number)
						.toString();
				String refName = Constants.createCamelCaseName(name, "-");
				String version = config.getProperty(PROPERTY_KEY_BEANSTALK_VERSION + type.getShortName());
				String healthCheckUrl = config.getProperty(PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL + type.getShortName());
				int minInstances = config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MIN_INSTANCES + type.getShortName());
				int maxInstances = config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MAX_INSTANCES + type.getShortName());
				String sslCertificateARN = config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN + type.getShortName());
				String hostedZone = config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE + type.getShortName());
				String cnamePrefix = name + "-" + hostedZone.replaceAll("\\.", "-");
				String imageId=null;
				try {
					String imagePipelineArn = config.getProperty(PROPERTY_KEY_IMAGE_PIPELINE_ARN);
					imageId = imageBuilderClient.getLatestImageIdForImagePipelineArn(imagePipelineArn);
				} catch (ConfigurationPropertyNotFound e)  {
					imageId=null; // if no image pipeline is specified, just use the default image
				}

				// Environment secrets
				SourceBundle environmentSecrets = type.shouldIncludeSecrets() ? secrets : null;

				// Copy the version from artifactory to S3.
				SourceBundle bundle = artifactCopy.copyArtifactIfNeeded(type, version, number);
				environmentDescriptors.add(new EnvironmentDescriptor().withName(name).withRefName(refName).withNumber(number)
						.withHealthCheckUrl(healthCheckUrl).withSourceBundle(bundle).withType(type)
						.withMinInstances(minInstances).withMaxInstances(maxInstances)
						.withVersionLabel(version)
						.withSslCertificateARN(sslCertificateARN)
						.withHostedZone(hostedZone)
						.withCnamePrefix(cnamePrefix)
						.withSecretsSource(environmentSecrets)
						.withImageId(imageId));
			} catch (ConfigurationPropertyNotFound e){
				//The necessary properties to build up the Environment was not fully defined so we choose not to create a stack for it.
				logger.warn("The Environment " + type + " was not created because " + e.getMissingKey() + " was not found");
			}
		}
		return environmentDescriptors;
	}

	/**
	 * Create the parameters to be passed to the template at runtime.
	 * 
	 * @return
	 */
	Parameter[] createSharedParameters() {
		List<Parameter> params = new ArrayList<>(2);
		String passwordValue = secretBuilder.getRepositoryDatabasePassword();
		Parameter databasePassword = Parameter.builder()
				.parameterKey(PARAMETER_MYSQL_PASSWORD)
				.parameterValue(passwordValue)
				.build();
		params.add(databasePassword);
		timeToLive.createTimeToLiveParameter().ifPresent(ttl -> {
			params.add(ttl);
		});
		return params.toArray(new Parameter[params.size()]);
	}

	/**
	 * Create the name of the stack from the input.
	 * 
	 * @return
	 */
	String createSharedResourcesStackName() {
		StringJoiner joiner = new StringJoiner("-");
		joiner.add(config.getProperty(PROPERTY_KEY_STACK));
		joiner.add(config.getProperty(PROPERTY_KEY_INSTANCE));
		joiner.add("shared-resources");
		return joiner.toString();
	}

	/**
	 * Create the prefix used for all of the VPC stack exports;
	 * 
	 * @return
	 */
	String createSharedExportPrefix() {
		StringJoiner joiner = new StringJoiner("-");
		joiner.add("us-east-1");
		joiner.add(createSharedResourcesStackName());
		return joiner.toString();
	}
	
	/**
	 * Extract the database end point suffix from the shared resources output.
	 * @param sharedResources
	 * @return
	 */
	String extractDatabaseSuffix(Stack sharedResources) {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		String outputName = stack+instance+OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT;
		// find the database end point suffix
		for(Output output: sharedResources.outputs()) {
			if(outputName.equals(output.outputKey())){
				String[] split = output.outputValue().split(stack+"-"+instance+"-db.");
				return split[1];
			}
		}
		throw new RuntimeException("Failed to find shared resources output: "+outputName);
	}

	/***
	 * Return the private subnet ids for a color
	 * @param color
	 * @return
	 */
	List<String> getPrivateSubnets(String color) {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String privateSubnets = cloudFormationClientWrapper.getOutput(
				Constants.createVpcPrivateSubnetsStackName(stack, color),
				Constants.VPC_PRIVATE_SUBNETS_STACK_PRIVATE_SUBNETS_OUPUT_KEY);
		String[] privateSubnetIds = privateSubnets.split(",");
		List<String> trimmedIds = Arrays.stream(privateSubnetIds).map(v -> v.trim()).collect(Collectors.toList());
		return trimmedIds;
	}

}
