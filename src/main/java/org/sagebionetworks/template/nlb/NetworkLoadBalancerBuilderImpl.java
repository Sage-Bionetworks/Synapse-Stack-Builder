package org.sagebionetworks.template.nlb;

import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_IP_ADDRESS_POOL_NUMBER_AZ_PER_NLB;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_NLB_DOMAIN_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_NLB_NUMBER;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.ip.address.IpAddressPoolBuilderImpl;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;

public class NetworkLoadBalancerBuilderImpl implements NetworkLoadBalancerBuilder {

	private CloudFormationClient cloudFormationClient;
	private VelocityEngine velocityEngine;
	private Configuration config;
	private Logger logger;
	private StackTagsProvider tagsProvider;

	@Inject
	public NetworkLoadBalancerBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
			Configuration config, LoggerFactory loggerFactory, StackTagsProvider tagsProvider) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.config = config;
		this.logger = loggerFactory.getLogger(IpAddressPoolBuilderImpl.class);
		this.tagsProvider = tagsProvider;
	}

	@Override
	public void buildAndDeploy() {
		VelocityContext context = new VelocityContext();
		String domain = config.getProperty(PROPERTY_KEY_NLB_DOMAIN_NAME);
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		int nlbNumber = config.getIntegerProperty(PROPERTY_KEY_NLB_NUMBER);
		int numberAzPerNlb = config.getIntegerProperty(PROPERTY_KEY_IP_ADDRESS_POOL_NUMBER_AZ_PER_NLB);
		
		List<String> addressNames = new ArrayList<>(numberAzPerNlb);
		for(int az=0; az<numberAzPerNlb; az++) {
			addressNames.add(IpAddressPoolBuilderImpl.ipAddressName(stack, nlbNumber, az));
		}

		String nlbName = new StringJoiner("-").add(stack).add(domain).toString();
		context.put("domain", domain);
		context.put("stack", stack);
		context.put("addressNames", addressNames);
		context.put("nlbName", nlbName);

		Parameter parameter = new Parameter();

		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate("templates/global/domain-network-load-balancer.json.vpt");
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String resultJSON = stringWriter.toString();
		System.out.println(resultJSON);
		JSONObject templateJson = new JSONObject(resultJSON);
		// Format the JSON
		resultJSON = templateJson.toString(JSON_INDENT);
		String stackName = new StringJoiner("-").add(stack).add(domain).add("nlb").toString();
		this.logger.info("Template for stack: " + stackName);
		this.logger.info(resultJSON);
		// create or update the template
		this.cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest().withStackName(stackName)
				.withTemplateBody(resultJSON).withParameters(parameter).withTags(tagsProvider.getStackTags()));

	}

}
