package org.sagebionetworks.template.vpc;

import static org.sagebionetworks.template.Constants.COLORS;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PARAMETER_PRIVATE_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PARAMETER_PUBLIC_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PARAMETER_VPC_NAME;
import static org.sagebionetworks.template.Constants.PARAMETER_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PARAMETER_VPN_CIDR;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_COLORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_PRIVATE_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_PUBLIC_SUBNET_ZONES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_VPN_CIDR;
import static org.sagebionetworks.template.Constants.TEMPLATES_VPC_MAIN_VPC_JSON_VTP;
import static org.sagebionetworks.template.Constants.VPC_STACK_NAME;

import java.io.StringWriter;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.PropertyProvider;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;

/**
 * Builder for the VPC template.
 *
 */
public class VpcTemplateBuilderImpl implements VpcTemplateBuilder {

	CloudFormationClient cloudFormationClient;
	VelocityEngine velocityEngine;
	PropertyProvider propertyProvider;
	Logger logger;

	@Inject
	public VpcTemplateBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
			PropertyProvider propertyProvider, LoggerFactory loggerFactory) {
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.propertyProvider = propertyProvider;
		this.logger = loggerFactory.getLogger(VpcTemplateBuilderImpl.class);
	}

	@Override
	public void buildAndDeploy() {
		// Create the context from the input
		VelocityContext context = createContext();
		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate(TEMPLATES_VPC_MAIN_VPC_JSON_VTP);
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String resultJSON = stringWriter.toString();
		JSONObject templateJson = new JSONObject(resultJSON);
		// Format the JSON
		resultJSON = templateJson.toString(JSON_INDENT);
		this.logger.info(resultJSON);
		Parameter[] params = createParameters();
		// create or update the template
		this.cloudFormationClient.createOrUpdateStack(Constants.VPC_STACK_NAME, resultJSON, params);
	}

	/**
	 * Create the context for this template.
	 * 
	 * @return
	 */
	VelocityContext createContext() {
		VelocityContext context = new VelocityContext();
		// Lookup the colors property
		String colorsCSV = propertyProvider.getProperty(PROPERTY_KEY_COLORS);
		String[] colors = colorsCSV.split(",");
		// trim
		for (int i = 0; i < colors.length; i++) {
			colors[i] = colors[i].trim();
		}
		context.put(COLORS, colors);
		return context;
	}

	/**
	 * Create the parameters for the template.
	 * 
	 * @return
	 */
	public Parameter[] createParameters() {
		Parameter VpcName = new Parameter().withParameterKey(PARAMETER_VPC_NAME).withParameterValue(VPC_STACK_NAME);
		Parameter VpcSubnetPrefix = new Parameter().withParameterKey(PARAMETER_VPC_SUBNET_PREFIX)
				.withParameterValue(propertyProvider.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX));
		Parameter PrivateSubnetZones = new Parameter().withParameterKey(PARAMETER_PRIVATE_SUBNET_ZONES)
				.withParameterValue(propertyProvider.getProperty(PROPERTY_KEY_VPC_PRIVATE_SUBNET_ZONES));
		Parameter PublicSubnetZones = new Parameter().withParameterKey(PARAMETER_PUBLIC_SUBNET_ZONES)
				.withParameterValue(propertyProvider.getProperty(PROPERTY_KEY_VPC_PUBLIC_SUBNET_ZONES));
		Parameter VpnCidr = new Parameter().withParameterKey(PARAMETER_VPN_CIDR)
				.withParameterValue(propertyProvider.getProperty(PROPERTY_KEY_VPC_VPN_CIDR));
		return new Parameter[] { VpcName, VpcSubnetPrefix, PrivateSubnetZones, PublicSubnetZones, VpnCidr };
	}
}
