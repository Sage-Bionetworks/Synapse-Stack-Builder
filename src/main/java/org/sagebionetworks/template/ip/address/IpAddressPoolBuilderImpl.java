package org.sagebionetworks.template.ip.address;

import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_IP_ADDRESS_POOL_NUMBER_AZ_PER_NLB;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_NLB_RECORDS_CSV;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.sagebionetworks.template.nlb.RecordName;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;

public class IpAddressPoolBuilderImpl implements IpAddressPoolBuilder {

	private CloudFormationClient cloudFormationClient;
	private VelocityEngine velocityEngine;
	private Configuration config;
	private Logger logger;
	private StackTagsProvider tagsProvider;

	@Inject
	public IpAddressPoolBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
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
		List<RecordName> records = Arrays.stream(config.getComaSeparatedProperty(PROPERTY_KEY_NLB_RECORDS_CSV)).map(RecordName::new).collect(Collectors.toList());
		int numberAzPerNlb = config.getIntegerProperty(PROPERTY_KEY_IP_ADDRESS_POOL_NUMBER_AZ_PER_NLB);
		int poolSize = records.size() * numberAzPerNlb;
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		
		List<String> names = new ArrayList<>(poolSize);
		for(RecordName record: records) {
			for(int az=0; az<numberAzPerNlb; az++) {
				names.add(ipAddressName(record.getShortName(), az));
			}
		}
				
		context.put("poolSize", poolSize);
		context.put("numberAzPerNlb", numberAzPerNlb);
		context.put("stack", stack);
		context.put("names", names);

		Parameter parameter = new Parameter();

		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate("templates/global/ip-address-pool.json.vpt");
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String resultJSON = stringWriter.toString();
		JSONObject templateJson = new JSONObject(resultJSON);
		// Format the JSON
		resultJSON = templateJson.toString(JSON_INDENT);
		String stackName = stack + "-ip-address-pool";
		this.logger.info("Template for stack: " + stackName);
		this.logger.info(resultJSON);
		// create or update the template
		this.cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest().withStackName(stackName)
				.withTemplateBody(resultJSON).withParameters(parameter).withTags(tagsProvider.getStackTags()));
	}
	
	public static String ipAddressName(String domain, int azNumber) {
		return String.format("%sAZ%d", domain.toLowerCase(), azNumber);
	}

}
