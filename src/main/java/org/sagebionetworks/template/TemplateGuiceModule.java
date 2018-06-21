package org.sagebionetworks.template;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.sagebionetworks.template.repo.IdGeneratorBuilder;
import org.sagebionetworks.template.repo.IdGeneratorBuilderImpl;
import org.sagebionetworks.template.repo.RepositoryTemplateBuilder;
import org.sagebionetworks.template.repo.RepositoryTemplateBuilderImpl;
import org.sagebionetworks.template.repo.WebACLBuilder;
import org.sagebionetworks.template.repo.WebACLBuilderImpl;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopy;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopyImpl;
import org.sagebionetworks.template.repo.beanstalk.ArtifactDownload;
import org.sagebionetworks.template.repo.beanstalk.ArtifactDownloadImpl;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilderImpl;
import org.sagebionetworks.template.vpc.VpcTemplateBuilder;
import org.sagebionetworks.template.vpc.VpcTemplateBuilderImpl;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSAsyncClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.google.inject.Provides;


public class TemplateGuiceModule extends com.google.inject.AbstractModule {

	private static final String RUNTIME_REFERENCES_STRICT = "runtime.references.strict";
	private static final String CLASSPATH_AND_FILE = "classpath,file";
	private static final String CLASSPATH_RESOURCE_LOADER_CLASS = "classpath.resource.loader.class";
	private static final String FILE_RESOURCE_LOADER_CLASS = "file.resource.loader.class";

	@Override
	protected void configure() {
		bind(CloudFormationClient.class).to(CloudFormationClientImpl.class);
		bind(VpcTemplateBuilder.class).to(VpcTemplateBuilderImpl.class);
		bind(Configuration.class).to(ConfigurationImpl.class);
		bind(LoggerFactory.class).to(LoggerFactoryImpl.class);
		bind(RepositoryTemplateBuilder.class).to(RepositoryTemplateBuilderImpl.class);
		bind(ArtifactDownload.class).to(ArtifactDownloadImpl.class);
		bind(ArtifactCopy.class).to(ArtifactCopyImpl.class);
		bind(FileProvider.class).to(FileProviderImpl.class);
		bind(ThreadProvider.class).to(ThreadProviderImp.class);
		bind(IdGeneratorBuilder.class).to(IdGeneratorBuilderImpl.class);
		bind(SecretBuilder.class).to(SecretBuilderImpl.class);
		bind(WebACLBuilder.class).to(WebACLBuilderImpl.class);
	}
	
	/**
	 * Create a AmazonCloudFormation client that uses the  {@link DefaultAWSCredentialsProviderChain}.
	 * @return
	 */
	@Provides
	public AmazonCloudFormation provideAmazonCloudFormationClient() {
		AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard();
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		builder.withRegion(Regions.US_EAST_1);
		return builder.build();
	}
	
	@Provides
	public AmazonS3 provideAmazonS3Client() {
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		builder.withRegion(Regions.US_EAST_1);
		return builder.build();
	}
	
	
	@Provides
	public HttpClient provideHttpClient() {
		HttpClientBuilder builder = HttpClientBuilder.create();
		return builder.build();
	}
	
	@Provides
	public AWSSecretsManager provideAWSSecretsManager() {
	    AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard();
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		builder.withRegion(Regions.US_EAST_1);
	    return builder.build();
	}
	
	@Provides
	public AWSKMS provideAWSKMSClient() {
		AWSKMSAsyncClientBuilder builder = AWSKMSAsyncClientBuilder.standard();
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		builder.withRegion(Regions.US_EAST_1);
		return builder.build();
	}
	
	@Provides
	public AmazonElasticLoadBalancing provideAmazonElasticLoadBalancing() {
		AmazonElasticLoadBalancingClientBuilder builder = AmazonElasticLoadBalancingClientBuilder.standard();
		builder.withCredentials(new DefaultAWSCredentialsProviderChain());
		builder.withRegion(Regions.US_EAST_1);
		return builder.build();
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

}
