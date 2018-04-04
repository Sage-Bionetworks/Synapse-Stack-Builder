package org.sagebionetworks.template.vpc;

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

	private static final int JSON_INDENT = 3;
	public static final String TEMPLATES_VPC_MAIN_VPC_JSON_VTP = "templates/vpc/main-vpc.json.vtp";
	public static final String COLORS = "colors";
	public static final String PROPERTY_KEY_COLORS = "org.sagebionetworks.vpc.colors.csv";
	
	CloudFormationClient cloudFormationClient;
	VelocityEngine velocityEngine;
	PropertyProvider propertyProvider;
	Logger logger;
	
	@Inject
	public VpcTemplateBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine, PropertyProvider propertyProvider, LoggerFactory loggerFactory) {
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.propertyProvider = propertyProvider;
		this.logger = loggerFactory.getLogger(VpcTemplateBuilderImpl.class);
	}

	@Override
	public void buildAndDeploy() {
		// Create the context from the input
		VelocityContext context = createContex();
		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate(TEMPLATES_VPC_MAIN_VPC_JSON_VTP);;
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
	 * @return
	 */
	 VelocityContext createContex() {
		VelocityContext context = new VelocityContext();
		// Lookup the colors property
		String colorsCSV = propertyProvider.getProperty(PROPERTY_KEY_COLORS);
		String[] colors = colorsCSV.split(",");
		// trim
		for(int i=0; i < colors.length; i++) {
			colors[i] = colors[i].trim();
		}
		context.put(COLORS, colors);
		return context;
	}
	 /**
	  * Create the parameters for the template.
	  * @return
	  */
	public Parameter[] createParameters() {
		Parameter VpcName = new Parameter().withParameterKey("VpcName").withParameterValue(Constants.VPC_STACK_NAME);
		Parameter VpcSubnetPrefix = new Parameter().withParameterKey("VpcSubnetPrefix")
				.withParameterValue(propertyProvider.getProperty("org.sagebionetworks.vpc.subnet.prefix"));
		Parameter PrivateSubnetZones = new Parameter().withParameterKey("PrivateSubnetZones")
				.withParameterValue(propertyProvider.getProperty("org.sagebionetworks.vpc.private.subnet.zones"));
		Parameter PublicSubnetZones = new Parameter().withParameterKey("PublicSubnetZones")
				.withParameterValue(propertyProvider.getProperty("org.sagebionetworks.vpc.public.subnet.zones"));
		Parameter VpnCidr = new Parameter().withParameterKey("VpnCidr")
				.withParameterValue(propertyProvider.getProperty("org.sagebionetworks.vpc.vpn.cidr"));
		return new Parameter[] { VpcName, VpcSubnetPrefix, PrivateSubnetZones,PublicSubnetZones, VpnCidr };
	}
}
