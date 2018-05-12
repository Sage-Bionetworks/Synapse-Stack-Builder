package org.sagebionetworks.template.repo;

import static org.sagebionetworks.template.Constants.CONFIGURATION_URL;
import static org.sagebionetworks.template.Constants.DATABASE_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.DEFAULT_REPO_PROPERTIES;
import static org.sagebionetworks.template.Constants.ENVIRONMENT;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PARAMETER_AWS_KEY;
import static org.sagebionetworks.template.Constants.PARAMETER_AWS_SECRET;
import static org.sagebionetworks.template.Constants.PARAMETER_ENCRYPTION_KEY;
import static org.sagebionetworks.template.Constants.PARAMETER_MYSQL_PASSWORD;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_AWS_ACCESS_KEY_ID;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_AWS_SECRET_KEY;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_ENCRYPTION_KEY;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MAX_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_MIN_INSTANCES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_NUMBER;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_SSL_ARN;
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
import static org.sagebionetworks.template.Constants.TEMPALTE_BEAN_STALK_ENVIRONMENT;
import static org.sagebionetworks.template.Constants.TEMPALTE_SHARED_RESOUCES_MAIN_JSON_VTP;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.*;

import java.io.StringWriter;
import java.util.StringJoiner;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopy;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentConfiguration;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentDescriptor;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.google.inject.Inject;

public class RepositoryTemplateBuilderImpl implements RepositoryTemplateBuilder {

	CloudFormationClient cloudFormationClient;
	VelocityEngine velocityEngine;
	Configuration config;
	Logger logger;
	ArtifactCopy artifactCopy;
	EnvironmentConfiguration environConfig;

	@Inject
	public RepositoryTemplateBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
			Configuration configuration, LoggerFactory loggerFactory, ArtifactCopy artifactCopy,
			EnvironmentConfiguration environConfig) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.config = configuration;
		this.config.initializeWithDefaults(DEFAULT_REPO_PROPERTIES);
		this.logger = loggerFactory.getLogger(RepositoryTemplateBuilderImpl.class);
		this.artifactCopy = artifactCopy;
		this.environConfig = environConfig;
	}

	@Override
	public void buildAndDeploy() throws InterruptedException {
		// Create the context from the input
		VelocityContext context = createSharedContext();

		Parameter[] sharedParameters = createSharedParameters();
		// Create the shared-resource stack
		String sharedResourceStackName = createSharedResourcesStackName();
		buildAndDeployStack(context, sharedResourceStackName, TEMPALTE_SHARED_RESOUCES_MAIN_JSON_VTP, sharedParameters);
		// Wait for the shared resources to complete
		Stack sharedStackResults = cloudFormationClient.waitForStackToComplete(sharedResourceStackName);
		// Build each bean stalk environment.
		buildEnvironments(sharedStackResults);
	}

	/**
	 * Build all of the environments
	 * @param sharedStackResults
	 */
	public void buildEnvironments(Stack sharedStackResults) {
		VelocityContext context = createEnvironmentContext(sharedStackResults);
		Parameter[] parameters = createEnvironmentParameters();
		// each environment is treated as its own stack.
		EnvironmentDescriptor[] environements = createEnvironments();
		for (EnvironmentDescriptor environment : environements) {
			// Replace the environment context
			context.put(ENVIRONMENT, environment);
			buildAndDeployStack(context, environment.getName(), TEMPALTE_BEAN_STALK_ENVIRONMENT, parameters);
		}
	}

	/**
	 * Parameters passed to each Elastic Bean Stalk build.
	 * 
	 * @return
	 */
	Parameter[] createEnvironmentParameters() {
		Parameter[] params = new Parameter[3];
		// Deprecated - Will be removed after switching to roles.
		params[0] = new Parameter().withParameterKey(PARAMETER_AWS_KEY)
				.withParameterValue(config.getProperty(PROPERTY_KEY_AWS_ACCESS_KEY_ID));
		// Deprecated - Will be removed after switching to roles.
		params[1] = new Parameter().withParameterKey(PARAMETER_AWS_SECRET)
				.withParameterValue(config.getProperty(PROPERTY_KEY_AWS_SECRET_KEY));
		// Deprecated - Will be removed after AWS secret manager.
		params[2] = new Parameter().withParameterKey(PARAMETER_ENCRYPTION_KEY)
				.withParameterValue(config.getProperty(PROPERTY_KEY_BEANSTALK_ENCRYPTION_KEY));
		return params;
	}

	/**
	 * Create the context used for each environment
	 * 
	 * @return
	 */
	VelocityContext createEnvironmentContext(Stack sharedStackResults) {
		VelocityContext context = new VelocityContext();
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		context.put(STACK, stack);
		context.put(INSTANCE, config.getProperty(PROPERTY_KEY_INSTANCE));
		context.put(VPC_SUBNET_COLOR, config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR));
		context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
		context.put(SHARED_EXPORT_PREFIX, createSharedExportPrefix());
		// Create and upload the configuration property file.
		String configUrl = environConfig.createEnvironmentConfiguration(sharedStackResults);
		context.put(CONFIGURATION_URL, configUrl);
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
		this.cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest().withStackName(stackName)
				.withTemplateBody(resultJSON).withParameters(parameters));
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
		context.put(VPC_SUBNET_COLOR, config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR));
		context.put(SHARED_RESOUCES_STACK_NAME, createSharedResourcesStackName());
		context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
		context.put(SHARED_EXPORT_PREFIX, createSharedExportPrefix());

		// Create the descriptors for all of the database.
		context.put(DATABASE_DESCRIPTORS, createDatabaseDescriptors());
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
		results[0] = new DatabaseDescriptor().withResourceName(stack + instance + "RepositoryDB")
				.withAllocatedStorage(config.getIntegerProperty(PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE))
				.withInstanceIdentifier(stack + "-" + instance + "-db").withDbName(stack + instance)
				.withInstanceClass(config.getProperty(PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS))
				.withMultiAZ(config.getBooleanProperty(PROPERTY_KEY_REPO_RDS_MULTI_AZ));

		// Describe each table database
		for (int i = 0; i < numberOfTablesDatabase; i++) {
			results[i + 1] = new DatabaseDescriptor().withResourceName(stack + instance + "Table" + i + "RepositoryDB")
					.withAllocatedStorage(config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE))
					.withInstanceIdentifier(stack + "-" + instance + "-table-" + i).withDbName(stack + instance)
					.withInstanceClass(config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS)).withMultiAZ(false);
		}
		return results;
	}

	/**
	 * Create an Environment descriptor for reop, workers, and portal.
	 * 
	 * @return
	 */
	public EnvironmentDescriptor[] createEnvironments() {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		EnvironmentDescriptor[] environments = new EnvironmentDescriptor[EnvironmentType.values().length];
		// create each type.
		for (int i = 0; i < EnvironmentType.values().length; i++) {
			EnvironmentType type = EnvironmentType.values()[i];
			int number = config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_NUMBER + type.getShortName());
			String name = new StringJoiner("-").add(type.getShortName()).add(stack).add(instance).add("" + number)
					.toString();
			String refName = Constants.createCamelCaseName(name);
			String version = config.getProperty(PROPERTY_KEY_BEANSTALK_VERSION + type.getShortName());
			String healthCheckUrl = config.getProperty(PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL + type.getShortName());
			int minInstances = config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MIN_INSTANCES + type.getShortName());
			int maxInstances = config.getIntegerProperty(PROPERTY_KEY_BEANSTALK_MAX_INSTANCES + type.getShortName());
			String sslCertificateARN = config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN+type.getShortName());
			String hostedZone = config.getProperty(PROPERTY_KEY_ROUTE_53_HOSTED_ZONE+type.getShortName());
			String cnamePrefix = name+"-"+hostedZone.replaceAll("\\.", "-");
			
			// Copy the version from artifactory to S3.
			SourceBundle bundle = artifactCopy.copyArtifactIfNeeded(type, version);
			environments[i] = new EnvironmentDescriptor().withName(name).withRefName(refName).withNumber(number)
					.withHealthCheckUrl(healthCheckUrl).withSourceBundle(bundle).withType(type)
					.withMinInstances(minInstances).withMaxInstances(maxInstances)
					.withVersionLabel(version)
					.withSslCertificateARN(sslCertificateARN)
					.withHostedZone(hostedZone)
					.withCnamePrefix(cnamePrefix);
		}
		return environments;
	}

	/**
	 * Create the parameters to be passed to the template at runtime.
	 * 
	 * @return
	 */
	Parameter[] createSharedParameters() {
		Parameter databasePassword = new Parameter().withParameterKey(PARAMETER_MYSQL_PASSWORD)
				.withParameterValue(config.getProperty(PROPERTY_KEY_MYSQL_PASSWORD));
		return new Parameter[] { databasePassword };
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

}
