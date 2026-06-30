package org.sagebionetworks.template;

import static org.sagebionetworks.template.Constants.APPCONFIG_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ATHENA_QUERIES_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.CLOUDWATCH_LOGS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.DATAWAREHOUSE_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.KINESIS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.LOAD_BALANCER_ALARM_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_IMAGE_CENTRAL_ROLE_ARN;
import static org.sagebionetworks.template.Constants.S3_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.SNS_AND_SQS_CONFIG_FILE;
import static org.sagebionetworks.template.TemplateUtils.loadFromJsonFile;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.template.cdn.CdnBuilder;
import org.sagebionetworks.template.cdn.CdnBuilderImpl;
import org.sagebionetworks.template.cdn.webacl.CdnWebAclBuilder;
import org.sagebionetworks.template.cdn.webacl.CdnWebAclBuilderImpl;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.config.ConfigurationImpl;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.config.RepoConfigurationImpl;
import org.sagebionetworks.template.config.SynapseAdminClientFactory;
import org.sagebionetworks.template.config.SynapseAdminClientFactoryImpl;
import org.sagebionetworks.template.config.TimeToLive;
import org.sagebionetworks.template.config.TimeToLiveImpl;
import org.sagebionetworks.template.cron.ExpiredStackTeardown;
import org.sagebionetworks.template.cron.ExpiredStackTeardownImpl;
import org.sagebionetworks.template.datawarehouse.DataWarehouseBuilder;
import org.sagebionetworks.template.datawarehouse.DataWarehouseBuilderImpl;
import org.sagebionetworks.template.datawarehouse.DataWarehouseConfig;
import org.sagebionetworks.template.datawarehouse.DataWarehouseConfigValidator;
import org.sagebionetworks.template.datawarehouse.backfill.BackfillDataWarehouseBuilder;
import org.sagebionetworks.template.datawarehouse.backfill.BackfillDataWarehouseBuilderImpl;
import org.sagebionetworks.template.docs.SynapseDocsBuilder;
import org.sagebionetworks.template.docs.SynapseDocsBuilderImpl;
import org.sagebionetworks.template.global.GlobalResourcesBuilder;
import org.sagebionetworks.template.global.GlobalResourcesBuilderImpl;
import org.sagebionetworks.template.global.waitconditions.SynapseHelpCollectionIndexCreation;
import org.sagebionetworks.template.global.waitconditions.SynapseHelpKnowledgeBaseDataSourceSync;
import org.sagebionetworks.template.ip.address.IpAddressPoolBuilder;
import org.sagebionetworks.template.ip.address.IpAddressPoolBuilderImpl;
import org.sagebionetworks.template.jobs.AsynchAdminJobExecutor;
import org.sagebionetworks.template.jobs.AsynchAdminJobExecutorImpl;
import org.sagebionetworks.template.markdownit.MarkDownItLambdaBuilder;
import org.sagebionetworks.template.markdownit.MarkDownItLambdaBuilderImpl;
import org.sagebionetworks.template.nlb.BindNetworkLoadBalancerBuilder;
import org.sagebionetworks.template.nlb.BindNetworkLoadBalancerBuilderImpl;
import org.sagebionetworks.template.nlb.NetworkLoadBalancerBuilder;
import org.sagebionetworks.template.nlb.NetworkLoadBalancerBuilderImpl;
import org.sagebionetworks.template.redirectors.userdocs.UserDocsRedirectorBuilder;
import org.sagebionetworks.template.redirectors.userdocs.UserDocsRedirectorBuilderImpl;
import org.sagebionetworks.template.repo.IdGeneratorBuilder;
import org.sagebionetworks.template.repo.IdGeneratorBuilderImpl;
import org.sagebionetworks.template.repo.RepositoryTemplateBuilder;
import org.sagebionetworks.template.repo.RepositoryTemplateBuilderImpl;
import org.sagebionetworks.template.repo.VelocityContextProvider;
import org.sagebionetworks.template.repo.agent.BedrockAgentContextProvider;
import org.sagebionetworks.template.repo.agent.BedrockGridAgentContextProvider;
import org.sagebionetworks.template.repo.appconfig.AppConfigConfig;
import org.sagebionetworks.template.repo.appconfig.AppConfigConfigValidator;
import org.sagebionetworks.template.repo.appconfig.AppConfigVelocityContextProvider;
import org.sagebionetworks.template.repo.athena.RecurrentAthenaQueryConfig;
import org.sagebionetworks.template.repo.athena.RecurrentAthenaQueryConfigValidator;
import org.sagebionetworks.template.repo.athena.RecurrentAthenaQueryContextProvider;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopy;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopyImpl;
import org.sagebionetworks.template.repo.beanstalk.ElasticBeanstalkSolutionStackNameProvider;
import org.sagebionetworks.template.repo.beanstalk.ElasticBeanstalkSolutionStackNameProviderImpl;
import org.sagebionetworks.template.repo.beanstalk.LoadBalancerAlarmsConfig;
import org.sagebionetworks.template.repo.beanstalk.LoadBalancerAlarmsConfigValidator;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilderImpl;
import org.sagebionetworks.template.repo.beanstalk.ssl.CertificateBuilder;
import org.sagebionetworks.template.repo.beanstalk.ssl.CertificateBuilderImpl;
import org.sagebionetworks.template.repo.beanstalk.ssl.ElasticBeanstalkExtentionBuilder;
import org.sagebionetworks.template.repo.beanstalk.ssl.ElasticBeanstalkExtentionBuilderImpl;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsConfig;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsConfigValidator;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsVelocityContextProvider;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsVelocityContextProviderImpl;
import org.sagebionetworks.template.repo.ecs.DockerImageBuilder;
import org.sagebionetworks.template.repo.ecs.DockerImageBuilderImpl;
import org.sagebionetworks.template.repo.grid.GridContextProvider;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseConfig;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseConfigValidator;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseVelocityContextProvider;
import org.sagebionetworks.template.repo.queues.SnsAndSqsConfig;
import org.sagebionetworks.template.repo.queues.SnsAndSqsVelocityContextProvider;
import org.sagebionetworks.template.repo.queues.SqsQueueDescriptor;
import org.sagebionetworks.template.s3.S3BucketBuilder;
import org.sagebionetworks.template.s3.S3BucketBuilderImpl;
import org.sagebionetworks.template.s3.S3Config;
import org.sagebionetworks.template.s3.S3ConfigValidator;
import org.sagebionetworks.template.s3.S3TransferManagerFactory;
import org.sagebionetworks.template.s3.S3TransferManagerFactoryImpl;
import org.sagebionetworks.template.utils.ArtifactDownload;
import org.sagebionetworks.template.utils.ArtifactDownloadImpl;
import org.sagebionetworks.template.vpc.SubnetTemplateBuilder;
import org.sagebionetworks.template.vpc.SubnetTemplateBuilderImpl;
import org.sagebionetworks.template.vpc.VpcTemplateBuilder;
import org.sagebionetworks.template.vpc.VpcTemplateBuilderImpl;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.DefaultClock;
import org.sagebionetworks.war.WarAppender;
import org.sagebionetworks.war.WarAppenderImpl;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.imagebuilder.ImagebuilderClient;
import software.amazon.awssdk.services.imagebuilder.ImagebuilderClientBuilder;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;


public class TemplateGuiceModule extends com.google.inject.AbstractModule {

	private static final String RUNTIME_REFERENCES_STRICT = "runtime.references.strict";
	private static final String CLASSPATH_AND_FILE = "classpath,file";
	private static final String CLASSPATH_RESOURCE_LOADER_CLASS = "classpath.resource.loader.class";
	private static final String FILE_RESOURCE_LOADER_CLASS = "file.resource.loader.class";
	private static final String IMAGE_CENTRAL_SESSION_NAME = "image-central-session";

	@Override
	protected void configure() {
		bind(CloudFormationClientWrapper.class).to(CloudFormationClientWrapperImpl.class);
		bind(VpcTemplateBuilder.class).to(VpcTemplateBuilderImpl.class);
		bind(SubnetTemplateBuilder.class).to(SubnetTemplateBuilderImpl.class);
		bind(Configuration.class).to(ConfigurationImpl.class);
		bind(RepoConfiguration.class).to(RepoConfigurationImpl.class);
		bind(LoggerFactory.class).to(LoggerFactoryImpl.class);
		bind(RepositoryTemplateBuilder.class).to(RepositoryTemplateBuilderImpl.class);
		bind(ArtifactDownload.class).to(ArtifactDownloadImpl.class);
		bind(ArtifactCopy.class).to(ArtifactCopyImpl.class);
		bind(FileProvider.class).to(FileProviderImpl.class);
		bind(ThreadProvider.class).to(ThreadProviderImp.class);
		bind(IdGeneratorBuilder.class).to(IdGeneratorBuilderImpl.class);
		bind(SecretBuilder.class).to(SecretBuilderImpl.class);
		bind(CertificateBuilder.class).to(CertificateBuilderImpl.class);
		bind(ElasticBeanstalkExtentionBuilder.class).to(ElasticBeanstalkExtentionBuilderImpl.class);
		bind(WarAppender.class).to(WarAppenderImpl.class);
		bind(ElasticBeanstalkSolutionStackNameProvider.class).to(ElasticBeanstalkSolutionStackNameProviderImpl.class);
		bind(StackTagsProvider.class).to(StackTagsProviderImpl.class);
		bind(S3BucketBuilder.class).to(S3BucketBuilderImpl.class);
		bind(SesClientWrapper.class).to(SesClientWrapperImpl.class);
		bind(GlobalResourcesBuilder.class).to(GlobalResourcesBuilderImpl.class);
		bind(CloudwatchLogsVelocityContextProvider.class).to(CloudwatchLogsVelocityContextProviderImpl.class);
		bind(Ec2ClientWrapper.class).to(Ec2ClientWrapperImpl.class);
		bind(SynapseAdminClientFactory.class).to(SynapseAdminClientFactoryImpl.class);
		bind(AsynchAdminJobExecutor.class).to(AsynchAdminJobExecutorImpl.class);
		bind(SynapseDocsBuilder.class).to(SynapseDocsBuilderImpl.class);
		bind(UserDocsRedirectorBuilder.class).to(UserDocsRedirectorBuilderImpl.class);
		bind(CdnBuilder.class).to(CdnBuilderImpl.class);
		bind(IpAddressPoolBuilder.class).to(IpAddressPoolBuilderImpl.class);
		bind(NetworkLoadBalancerBuilder.class).to(NetworkLoadBalancerBuilderImpl.class);
		bind(BindNetworkLoadBalancerBuilder.class).to(BindNetworkLoadBalancerBuilderImpl.class);
		bind(TimeToLive.class).to(TimeToLiveImpl.class);
		bind(Clock.class).to(DefaultClock.class);
		bind(ExpiredStackTeardown.class).to(ExpiredStackTeardownImpl.class);
		bind(DataWarehouseBuilder.class).to(DataWarehouseBuilderImpl.class);
		bind(BackfillDataWarehouseBuilder.class).to(BackfillDataWarehouseBuilderImpl.class);
		bind(MarkDownItLambdaBuilder.class).to(MarkDownItLambdaBuilderImpl.class);
		bind(ImageBuilderClient.class).to(ImageBuilderClientImpl.class);
		bind(CdnWebAclBuilder.class).to(CdnWebAclBuilderImpl.class);
		bind(DockerImageBuilder.class).to(DockerImageBuilderImpl.class);

		Multibinder<VelocityContextProvider> velocityContextProviderMultibinder = Multibinder.newSetBinder(binder(), VelocityContextProvider.class);

		velocityContextProviderMultibinder.addBinding().to(AppConfigVelocityContextProvider.class);
		velocityContextProviderMultibinder.addBinding().to(SnsAndSqsVelocityContextProvider.class);
		velocityContextProviderMultibinder.addBinding().to(KinesisFirehoseVelocityContextProvider.class);
		velocityContextProviderMultibinder.addBinding().to(RecurrentAthenaQueryContextProvider.class);
		velocityContextProviderMultibinder.addBinding().to(BedrockAgentContextProvider.class);
		velocityContextProviderMultibinder.addBinding().to(BedrockGridAgentContextProvider.class);
		velocityContextProviderMultibinder.addBinding().to(GridContextProvider.class);
		
		Multibinder<WaitConditionHandler> waitConditionHandlerBinder = Multibinder.newSetBinder(binder(), WaitConditionHandler.class);
		
		waitConditionHandlerBinder.addBinding().to(SynapseHelpCollectionIndexCreation.class);
		waitConditionHandlerBinder.addBinding().to(SynapseHelpKnowledgeBaseDataSourceSync.class);
	}
	
	/**
	 * Create a AmazonCloudFormation client that uses the  {@link DefaultAWSCredentialsProviderChain}.
	 * @return
	 */
	@Provides
	public CloudFormationClient provideAmazonCloudFormationClient() {
		CloudFormationClient client = CloudFormationClient.builder().region(Region.US_EAST_1).build();
		return client;
	}
	
	@Provides
	public AmazonS3 provideAmazonS3Client() {
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		builder.withRegion(Regions.US_EAST_1);
		return builder.build();
	}

	@Provides
	public S3Client provideS3Client() {
		return S3Client.builder().region(Region.US_EAST_1).build();
	}

	@Provides
	public LambdaClient provideAWSLambdaClient() {
		LambdaClient client = LambdaClient.builder().region(Region.US_EAST_1).build();
		return client;
	}

	@Provides
	public GlueClient provideAmazonAWSGlueClient() {
		GlueClient client = GlueClient.builder().region(Region.US_EAST_1).build();
		return client;
	}

	@Provides
	public AthenaClient provideAmazonAmazonAthenaClient() {
		AthenaClient client = AthenaClient.builder().region(Region.US_EAST_1).build();
		return client;
	}

	@Provides
	public SesClient provideAmazonSimpleEmalService() {
		return SesClient.builder().region(Region.US_EAST_1).credentialsProvider(DefaultCredentialsProvider.create()).build();
	}

 	@Provides
	public HttpClient provideHttpClient() {
		HttpClientBuilder builder = HttpClientBuilder.create();
		return builder.build();
	}
	
	@Provides
	public SecretsManagerClient provideAWSSecretsManager() {
		SecretsManagerClient client = SecretsManagerClient.builder().region(Region.US_EAST_1).build();
		return client;
	}

	@Provides
	public KmsClient provideAWSKMSClient() {
		KmsClient client = KmsClient.builder().region(Region.US_EAST_1).build();
		return client;
	}

	@Provides
	public Ec2Client provideAmazonEc2(){
		Ec2Client client = Ec2Client.builder().region(Region.US_EAST_1).build();
		return client;
	}

	@Provides
	public ElasticBeanstalkClient provideAmazonElasticBeanstalk(){
		ElasticBeanstalkClient client = ElasticBeanstalkClient.builder()
				.region(Region.US_EAST_1).build();
		return client;
	}

	@Provides
	public VelocityEngine velocityEngineProvider() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, CLASSPATH_AND_FILE); 
		engine.setProperty(CLASSPATH_RESOURCE_LOADER_CLASS, ClasspathResourceLoader.class.getName());
		engine.setProperty(FILE_RESOURCE_LOADER_CLASS, FileResourceLoader.class.getName());
		engine.setProperty(RUNTIME_REFERENCES_STRICT, true);
		return engine;
	}

	@Provides
	public SnsAndSqsConfig snsAndSqsConfigProvider() throws IOException {
		return loadFromJsonFile(SNS_AND_SQS_CONFIG_FILE, SnsAndSqsConfig.class);
	}
	
	@Provides
	@Named("GridQueueReferenceName")
	public String getGridQueueRef(SnsAndSqsConfig config) {
		SqsQueueDescriptor des = config.getQueueDescriptors().stream()
				.filter(d -> "GRID_WEBSOCKET_MESSAGE.fifo".equals(d.getQueueName())).findFirst().get();
		return des.getQueueReferenceName() + "Queue";
	}

	@Provides
	public AppConfigConfig appConfigProvider() throws IOException {
		return new AppConfigConfigValidator(loadFromJsonFile(APPCONFIG_CONFIG_FILE, AppConfigConfig.class)).validate();
	}
	
	@Provides
	public KinesisFirehoseConfig kinesisConfigProvider() throws IOException {
		return new KinesisFirehoseConfigValidator(loadFromJsonFile(KINESIS_CONFIG_FILE, KinesisFirehoseConfig.class)).validate();
	}
	
	@Provides
	public RecurrentAthenaQueryConfig athenaQueryConfigProvider(SnsAndSqsConfig sqsConfig) throws IOException {
		return new RecurrentAthenaQueryConfigValidator(loadFromJsonFile(ATHENA_QUERIES_CONFIG_FILE, RecurrentAthenaQueryConfig.class), sqsConfig).validate();
	}

	@Provides
	public CloudwatchLogsConfig cloudwatchLogsConfigProvider(Configuration props) throws IOException {
		return new CloudwatchLogsConfigValidator(loadFromJsonFile(CLOUDWATCH_LOGS_CONFIG_FILE, CloudwatchLogsConfig.class), props).validate();
	}
	
	@Provides
	public S3Config s3ConfigProvider() throws IOException {
		return new S3ConfigValidator(loadFromJsonFile(S3_CONFIG_FILE, S3Config.class)).validate();
	}
	
	@Provides
	public LoadBalancerAlarmsConfig loadBalanacerConfigProvider() throws IOException {
		return new LoadBalancerAlarmsConfigValidator(loadFromJsonFile(LOAD_BALANCER_ALARM_CONFIG_FILE, LoadBalancerAlarmsConfig.class)).validate();
	}
	
	@Provides
	public SynapseAdminClient synapseAdminClient(SynapseAdminClientFactory factory) {
		return factory.getInstance();
	}
	
	@Provides
	public S3TransferManagerFactory provideS3TransferManagerFactory(AmazonS3 s3Client) {
		return new S3TransferManagerFactoryImpl(s3Client);
	}

	@Provides
	public DataWarehouseConfig dataWarehouseConfigProvider() throws IOException {
		return new DataWarehouseConfigValidator(loadFromJsonFile(DATAWAREHOUSE_CONFIG_FILE, DataWarehouseConfig.class)).validate();
	}
		
	@Provides
	public OpenSearchServerlessClient ossManagementClientProvider() {
		return OpenSearchServerlessClient.builder().region(Region.US_EAST_1).build();
	}
	
	@Provides
	public BedrockAgentClient bedrockAgentClientProvider() {
		return BedrockAgentClient.builder().region(Region.US_EAST_1).build();
	}
	
	@Provides
	public OpenSearchClientFactory openSearchClientFactoryProvider() {
		return new OpenSearchClientFactoryImpl(ApacheHttpClient.builder().build());
	}
	
	@Provides
	public StsClient provideAmazonSTS(){
		return StsClient.builder().region(Region.US_EAST_1).build();
	}
	
	/*
	 * Requests to image builder in the image central AWS account
	 * must be made using a role in that account.  The role 
	 * is shared with the entire organization, so any role running
	 * the stack builder can assume it.
	 */
	@Provides
	public ImagebuilderClient imageBuilderClientProvider(StsClient stsClient, RepoConfiguration props) {
		String imageCentralRoleArn=props.getProperty(PROPERTY_KEY_IMAGE_CENTRAL_ROLE_ARN);
		
		AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder().
				roleArn(imageCentralRoleArn).
				roleSessionName(IMAGE_CENTRAL_SESSION_NAME).
				build();
		
		StsAssumeRoleCredentialsProvider credentialsProvider = 
				StsAssumeRoleCredentialsProvider.builder().
				refreshRequest(assumeRoleRequest).
				stsClient(stsClient)
				.build();

		ImagebuilderClientBuilder imageBuilderClientBuilder = ImagebuilderClient.builder();
		imageBuilderClientBuilder.credentialsProvider(credentialsProvider);
		imageBuilderClientBuilder.region(Region.US_EAST_1);
		return imageBuilderClientBuilder.build();
	}
	
	@Provides
	public CognitoIdentityProviderClient provideCognitoIdentityProvider() {
		CognitoIdentityProviderClient client = CognitoIdentityProviderClient.builder().region(Region.US_EAST_1).build();
		return client;
	}


}
