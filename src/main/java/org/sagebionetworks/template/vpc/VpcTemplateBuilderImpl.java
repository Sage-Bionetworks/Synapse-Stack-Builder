package org.sagebionetworks.template.vpc;

import static org.sagebionetworks.template.Constants.AVAILABILITY_ZONES;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PARAMETER_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PARAMETER_VPN_CIDR;
import static org.sagebionetworks.template.Constants.PEERING_ROLE_ARN_PREFIX;
import static org.sagebionetworks.template.Constants.PEER_ROLE_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_COLORS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_AVAILABILITY_ZONES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_PEERING_ACCEPT_ROLE_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_SUBNET_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_VPC_VPN_CIDR;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.SUBNETS;
import static org.sagebionetworks.template.Constants.TEMPLATES_VPC_MAIN_VPC_JSON_VTP;
import static org.sagebionetworks.template.Constants.VPC_CIDR;
import static org.sagebionetworks.template.Constants.VPC_CIDR_SUFFIX;
import static org.sagebionetworks.template.Constants.VPC_COLOR_GROUP_NETWORK_MASK;
import static org.sagebionetworks.template.Constants.VPC_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.VPC_SUBNET_NETWORK_MASK;

import java.io.StringWriter;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;

/**
 * Builder for the VPC template.
 *
 */
public class VpcTemplateBuilderImpl implements VpcTemplateBuilder {

	CloudFormationClient cloudFormationClient;
	VelocityEngine velocityEngine;
	Configuration config;
	Logger logger;

	@Inject
	public VpcTemplateBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
			Configuration configuration, LoggerFactory loggerFactory) {
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.config = configuration;
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
		this.cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest().withStackName(stackName)
				.withTemplateBody(resultJSON).withParameters(params));
	}

	/**
	 * Create the context for this template.
	 * 
	 * @return
	 */
	VelocityContext createContext() {
		VelocityContext context = new VelocityContext();

		String vpcSubnetPrefix = config.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX);
		// VPC CIDR
		String vpcCidr = vpcSubnetPrefix + VPC_CIDR_SUFFIX;
		context.put(VPC_CIDR, vpcCidr);

		// The roll from the admin-central account that allows this account to accept
		// VPC peering
		context.put(PEER_ROLE_ARN, getPeeringRoleArn());

		String availabilityZonesRaw = config.getProperty(PROPERTY_KEY_VPC_AVAILABILITY_ZONES);
		context.put(AVAILABILITY_ZONES, availabilityZonesRaw);

		String[] availabilityZones = config.getComaSeparatedProperty(PROPERTY_KEY_VPC_AVAILABILITY_ZONES);

		// Create the sub-nets
		SubnetBuilder builder = new SubnetBuilder();
		builder.withCidrPrefix(vpcSubnetPrefix);
		builder.withColors(getColorsFromProperty());
		builder.withSubnetMask(VPC_SUBNET_NETWORK_MASK);
		builder.withColorGroupNetMaskSubnetMask(VPC_COLOR_GROUP_NETWORK_MASK);
		builder.withAvailabilityZones(availabilityZones);
		Subnets subnets = builder.build();
		context.put(SUBNETS, subnets);

		context.put(STACK, config.getProperty(PROPERTY_KEY_STACK));

		return context;
	}

	/**
	 * Get the role ARN used to accept VPC connection peering.
	 * 
	 * @return
	 */
	public String getPeeringRoleArn() {
		String peeringRoleArn = config.getProperty(PROPERTY_KEY_VPC_PEERING_ACCEPT_ROLE_ARN);
		if (!peeringRoleArn.startsWith(PEERING_ROLE_ARN_PREFIX)) {
			throw new IllegalArgumentException(
					PROPERTY_KEY_VPC_PEERING_ACCEPT_ROLE_ARN + " must start with: " + PEERING_ROLE_ARN_PREFIX);
		}
		return peeringRoleArn;
	}

	/**
	 * Get the colors from the property CSV.
	 * 
	 * @return
	 */
	Color[] getColorsFromProperty() {
		String[] colorString = config.getComaSeparatedProperty(PROPERTY_KEY_COLORS);
		Color[] colors = new Color[colorString.length];
		for (int i = 0; i < colorString.length; i++) {
			colors[i] = Color.valueOf(colorString[i]);
		}
		return colors;
	}

	/**
	 * Create the name of the stack.
	 * 
	 * @return
	 */
	String createStackName() {
		return String.format(VPC_STACK_NAME_FORMAT, config.getProperty(PROPERTY_KEY_STACK));
	}

	/**
	 * Create the parameters for the template.
	 * 
	 * @return
	 */
	public Parameter[] createParameters(String stackName) {
		Parameter VpcSubnetPrefix = new Parameter().withParameterKey(PARAMETER_VPC_SUBNET_PREFIX)
				.withParameterValue(config.getProperty(PROPERTY_KEY_VPC_SUBNET_PREFIX));
		Parameter VpnCidr = new Parameter().withParameterKey(PARAMETER_VPN_CIDR)
				.withParameterValue(config.getProperty(PROPERTY_KEY_VPC_VPN_CIDR));
		return new Parameter[] { VpcSubnetPrefix, VpnCidr };
	}
}
