package org.sagebionetworks.template;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.sagebionetworks.template.repo.RepositoryTemplateBuilder;
import org.sagebionetworks.template.repo.RepositoryTemplateBuilderImpl;
import org.sagebionetworks.template.vpc.VpcTemplateBuilder;
import org.sagebionetworks.template.vpc.VpcTemplateBuilderImpl;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.google.inject.Provides;


public class TemplateGuiceModule extends com.google.inject.AbstractModule {

	private static final String RUNTIME_REFERENCES_STRICT = "runtime.references.strict";
	private static final String CLASSPATH = "classpath";
	private static final String CLASSPATH_RESOURCE_LOADER_CLASS = "classpath.resource.loader.class";

	@Override
	protected void configure() {
		bind(CloudFormationClient.class).to(CloudFormationClientImpl.class);
		bind(VpcTemplateBuilder.class).to(VpcTemplateBuilderImpl.class);
		bind(PropertyProvider.class).to(SystemPropertyProvider.class);
		bind(LoggerFactory.class).to(LoggerFactoryImpl.class);
		bind(RepositoryTemplateBuilder.class).to(RepositoryTemplateBuilderImpl.class);
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
	public VelocityEngine velocityEngineProvider() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, CLASSPATH); 
		engine.setProperty(CLASSPATH_RESOURCE_LOADER_CLASS, ClasspathResourceLoader.class.getName());
		engine.setProperty(RUNTIME_REFERENCES_STRICT, true);
		return engine;
	}

}
