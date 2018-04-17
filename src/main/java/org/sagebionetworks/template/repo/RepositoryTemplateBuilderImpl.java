package org.sagebionetworks.template.repo;

import static org.sagebionetworks.template.Constants.*;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_COLOR;
import static org.sagebionetworks.template.Constants.SHARED_RESOUCES_STACK_NAME;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPALTE_SHARED_RESOUCES_MAIN_JSON_VTP;
import static org.sagebionetworks.template.Constants.VPC_SUBNET_COLOR;

import java.io.StringWriter;
import java.util.Properties;
import java.util.StringJoiner;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.PropertyProvider;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;

public class RepositoryTemplateBuilderImpl implements RepositoryTemplateBuilder {

	CloudFormationClient cloudFormationClient;
	VelocityEngine velocityEngine;
	PropertyProvider propertyProvider;
	Logger logger;

	@Inject
	public RepositoryTemplateBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
			PropertyProvider propertyProvider, LoggerFactory loggerFactory) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.propertyProvider = propertyProvider;
		this.logger = loggerFactory.getLogger(RepositoryTemplateBuilderImpl.class);
	}

	@Override
	public void buildAndDeploy() {
		// Create the context from the input
		VelocityContext context = createContext();

		// Create the shared-resource stack
		String sharedResourceStackName = createSharedResourcesStackName();
		buildAndDeployStack(context, sharedResourceStackName, TEMPALTE_SHARED_RESOUCES_MAIN_JSON_VTP);

	}

	void buildAndDeployStack(VelocityContext context, String stackName, String templatePath) {
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
		Parameter[] parameters = createParameters();
		// create or update the template
		this.cloudFormationClient.createOrUpdateStack(stackName, resultJSON, parameters);
	}

	/**
	 * Create the template context.
	 * 
	 * @return
	 */
	VelocityContext createContext() {
		VelocityContext context = new VelocityContext();
		context.put(STACK, propertyProvider.getProperty(PROPERTY_KEY_STACK));
		context.put(INSTANCE, propertyProvider.getProperty(PROPERTY_KEY_INSTANCE));
		context.put(VPC_SUBNET_COLOR, propertyProvider.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR));
		context.put(SHARED_RESOUCES_STACK_NAME, createSharedResourcesStackName());
		context.put(VPC_EXPORT_PREFIX, createVpcExportPrefix());
		
		Properties props = new Properties();
		// add all default properties
		props.putAll(propertyProvider.loadPropertiesFromClasspath(DEFAULT_REPO_PROPERTIES));
		// override the defaults from the system.
		props.putAll(propertyProvider.getSystemProperties());
		// add the merge of system and default properties.
		context.put(PROPS, props);
		
		context.put(TABLE_DATABASE_SUFFIXES, tableDatabaseSuffixes(
				Integer.parseInt((String) props.get(PROPERTY_KEY_TABLES_INSTANCE_COUNT))));
		
		return context;
	}
	
	/**
	 * Create the tables database suffixes from the number of database instances.
	 * 
	 * @param numbeDatabase
	 * @return
	 */
	public String[] tableDatabaseSuffixes(int numbeDatabase) {
		String[] results = new String[numbeDatabase];
		for(int i=0; i<numbeDatabase; i++) {
			results[i] = ""+i;
		}
		return results;
	}

	/**
	 * Create the parameters to be passed to the template at runtime.
	 * 
	 * @return
	 */
	Parameter[] createParameters() {
		Parameter databasePassword = new Parameter().withParameterKey(PARAMETER_MYSQL_PASSWORD)
				.withParameterValue(propertyProvider.getProperty(PROPERTY_KEY_MYSQL_PASSWORD));
		return new Parameter[] { databasePassword };
	}

	/**
	 * Create the name of the stack from the input.
	 * 
	 * @return
	 */
	String createSharedResourcesStackName() {
		StringJoiner joiner = new StringJoiner("-");
		joiner.add(propertyProvider.getProperty(PROPERTY_KEY_STACK));
		joiner.add(propertyProvider.getProperty(PROPERTY_KEY_INSTANCE));
		joiner.add("shared-resources");
		return joiner.toString();
	}
	
	/**
	 * Create the prefix used for all of the VPC stack exports;
	 * @return
	 */
	String createVpcExportPrefix() {
		StringJoiner joiner = new StringJoiner("-");
		joiner.add("us-east-1-synapse");
		joiner.add(propertyProvider.getProperty(PROPERTY_KEY_STACK));
		joiner.add("vpc");
		return joiner.toString();
	}

}
