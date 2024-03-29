package org.sagebionetworks.template.nlb;

import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BIND_RECORD_TO_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

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

public class BindNetworkLoadBalancerBuilderImpl implements BindNetworkLoadBalancerBuilder {

	public static final String MAPPINGS_CSV = "mappingsCSV";
	
	private CloudFormationClient cloudFormationClient;
	private VelocityEngine velocityEngine;
	private Configuration config;
	private Logger logger;
	private StackTagsProvider tagsProvider;

	@Inject
	public BindNetworkLoadBalancerBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
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
		List<RecordToStackMapping> mappings = Arrays
				.stream(config.getComaSeparatedProperty(PROPERTY_KEY_BIND_RECORD_TO_STACK))
				.map(s -> RecordToStackMapping.builder().withMapping(s).build()).collect(Collectors.toList());
		
		StringJoiner joiner = new StringJoiner(",");
		mappings.forEach(r -> joiner.add(r.getMapping()));
		String mappingsCSV = joiner.toString();
		String stack = config.getProperty(PROPERTY_KEY_STACK);

		String stackName = stack + "-dns-record-to-stack-mapping";

		List<Listener> listeners = new ArrayList<>(mappings.size() * 2);
		for (RecordToStackMapping map : mappings) {
			listeners.add(new Listener(80, map));
			listeners.add(new Listener(443, map));
		}

		context.put(MAPPINGS_CSV, mappingsCSV);
		context.put("listeners", listeners);
		context.put("stack", stack);
		Parameter parameter = new Parameter();

		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate("templates/global/dns-record-to-stack-mapping.json.vpt");
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
				.withTemplateBody(resultJSON).withParameters(parameter).withTags(tagsProvider.getStackTags()));
		
		try {
			this.cloudFormationClient.waitForStackToComplete(stackName);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

}
