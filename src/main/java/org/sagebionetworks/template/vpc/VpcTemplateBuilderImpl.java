package org.sagebionetworks.template.vpc;

import java.io.StringWriter;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.SesClient;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;

import static org.sagebionetworks.template.Constants.*;

/**
 * Builder for the VPC template.
 *
 */
public class VpcTemplateBuilderImpl implements VpcTemplateBuilder {

	CloudFormationClient cloudFormationClient;
	VelocityEngine velocityEngine;
	Configuration config;
	Logger logger;
	StackTagsProvider stackTagsProvider;

	@Inject
	public VpcTemplateBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
								  Configuration configuration, LoggerFactory loggerFactory, StackTagsProvider stackTagsProvider) {
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.config = configuration;
		this.logger = loggerFactory.getLogger(VpcTemplateBuilderImpl.class);
		this.stackTagsProvider = stackTagsProvider;
	}

	@Override
	public void buildAndDeploy() throws InterruptedException {
		String stackName = createStackName();
		// Create the context from the input
		VelocityContext context = createContext();
		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate(TEMPLATES_VPC_MAIN_VPC_JSON_VTP);
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String resultJSON = stringWriter.toString();
		System.out.println(resultJSON);
		JSONObject templateJson = new JSONObject(resultJSON);
		// Format the JSON
		resultJSON = templateJson.toString(JSON_INDENT);
		this.logger.info(resultJSON);
		Parameter[] params = createParameters(stackName);
		// create or update the template
		this.cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest()
				.withStackName(stackName)
				.withTemplateBody(resultJSON)
				.withTags(stackTagsProvider.getStackTags())
				.withParameters(params));
		this.cloudFormationClient.waitForStackToComplete(stackName);
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
		Parameter VpnCidrNew = new Parameter().withParameterKey(PARAMETER_VPN_CIDR_NEW)
				.withParameterValue(config.getProperty(PROPERTY_KEY_VPC_VPN_CIDR_NEW));
		return new Parameter[] { VpnCidrNew };
	}
}
