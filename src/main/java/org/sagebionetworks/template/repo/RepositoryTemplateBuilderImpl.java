package org.sagebionetworks.template.repo;

import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_COLOR;
import static org.sagebionetworks.template.Constants.SHARED_RESOUCES_STACK_NAME;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPALTE_SHARED_RESOUCES_MAIN_JSON_VTP;
import static org.sagebionetworks.template.Constants.VPC_SUBNET_COLOR;

import java.io.StringWriter;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.PropertyProvider;

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
		this.logger.info("Template for stack: "+stackName);
		this.logger.info(resultJSON);
		// create or update the template
		this.cloudFormationClient.createOrUpdateStack(stackName, resultJSON);
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
		return context;
	}

	/**
	 * Create the name of the stack from the input.
	 * @return
	 */
	String createSharedResourcesStackName() {
		StringBuilder builder = new StringBuilder();
		builder.append(propertyProvider.getProperty(PROPERTY_KEY_STACK));
		builder.append(propertyProvider.getProperty(PROPERTY_KEY_INSTANCE));
		builder.append("SharedResources");
		return builder.toString();
	}

}
