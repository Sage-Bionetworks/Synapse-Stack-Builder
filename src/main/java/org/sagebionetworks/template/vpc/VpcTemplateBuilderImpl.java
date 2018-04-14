package org.sagebionetworks.template.vpc;

import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PARAMETER_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PARAMETER_VPN_CIDR;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_COLORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_VPN_CIDR;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.SUBNET_GROUPS;
import static org.sagebionetworks.template.Constants.TEMPLATES_VPC_MAIN_VPC_JSON_VTP;
import static org.sagebionetworks.template.Constants.VPC_CIDR;
import static org.sagebionetworks.template.Constants.VPC_CIDR_SUFFIX;
import static org.sagebionetworks.template.Constants.VPC_COLOR_GROUP_NETWORK_MASK;
import static org.sagebionetworks.template.Constants.VPC_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.*;

import java.io.StringWriter;

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
		String stackName = createStackName();
		// Create the context from the input
		VelocityContext context = createContext();
		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate(TEMPLATES_VPC_MAIN_VPC_JSON_VTP);
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String resultJSON = stringWriter.toString();
//		System.out.println(resultJSON);
		JSONObject templateJson = new JSONObject(resultJSON);
		// Format the JSON
		resultJSON = templateJson.toString(JSON_INDENT);
		this.logger.info(resultJSON);
		Parameter[] params = createParameters(stackName);
		// create or update the template
		this.cloudFormationClient.createOrUpdateStack(stackName, resultJSON, params);
	}

	/**
	 * Create the context for this template.
	 * 
	 * @return
	 */
	VelocityContext createContext() {
		VelocityContext context = new VelocityContext();
		
		String vpcSubnetPrefix = propertyProvider.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX);
		// VPC CIDR
		String vpcCidr = vpcSubnetPrefix+VPC_CIDR_SUFFIX;
		context.put(VPC_CIDR, vpcCidr);

		// Create the sub-nets
		SubnetBuilder builder = new SubnetBuilder();
		builder.withCidrPrefix(vpcSubnetPrefix);
		builder.withColors(getColorsFromProperty());
		builder.withSubnetMask(VPC_SUBNET_NETWORK_MASK);
		builder.withColorGroupNetMaskSubnetMask(VPC_COLOR_GROUP_NETWORK_MASK);
		builder.withPublicAvailabilityZones(propertyProvider.getComaSeparatedProperty(PROPERTY_KEY_VPC_PUBLIC_SUBNET_ZONES));
		builder.withPrivateAvailabilityZones(propertyProvider.getComaSeparatedProperty(PROPERTY_KEY_VPC_PRIVATE_SUBNET_ZONES));
		SubnetGroup[] subnets = builder.build();
		context.put(SUBNET_GROUPS, subnets);	
		
		context.put(STACK, propertyProvider.getProperty(PROPERTY_KEY_STACK));
		
		return context;
	}
	
	/**
	 * Get the colors from the property CSV.
	 * @return
	 */
	Color[] getColorsFromProperty() {
		String[] colorString = propertyProvider.getComaSeparatedProperty(PROPERTY_KEY_COLORS);
		Color[] colors = new Color[colorString.length];
		for (int i = 0; i < colorString.length; i++) {
			colors[i] = Color.valueOf(colorString[i]);
		}
		return colors;
	}	
	
	/**
	 * Create the name of the stack.
	 * @return
	 */
	String createStackName() {
		return String.format(VPC_STACK_NAME_FORMAT, propertyProvider.getProperty(PROPERTY_KEY_STACK));
	}

	/**
	 * Create the parameters for the template.
	 * 
	 * @return
	 */
	public Parameter[] createParameters(String stackName) {
		Parameter VpcSubnetPrefix = new Parameter().withParameterKey(PARAMETER_VPC_SUBNET_PREFIX)
				.withParameterValue(propertyProvider.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX));
		Parameter VpnCidr = new Parameter().withParameterKey(PARAMETER_VPN_CIDR)
				.withParameterValue(propertyProvider.getProperty(PROPERTY_KEY_VPC_VPN_CIDR));
		return new Parameter[] { VpcSubnetPrefix, VpnCidr };
	}
}
