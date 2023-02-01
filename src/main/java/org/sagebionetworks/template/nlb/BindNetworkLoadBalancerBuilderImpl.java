package org.sagebionetworks.template.nlb;

import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BIND_RECORD_TO_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import com.amazonaws.services.cloudformation.model.Stack;
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
		List<RecordToStackMapping> configMapping = Arrays
				.stream(config.getComaSeparatedProperty(PROPERTY_KEY_BIND_RECORD_TO_STACK))
				.map(s -> RecordToStackMapping.builder().withMapping(s).build()).collect(Collectors.toList());
		// remove anything with a 'none' target
		configMapping = configMapping.stream().filter(m-> m.getTarget() != null).collect(Collectors.toList());
		
		StringJoiner joiner = new StringJoiner(",");
		configMapping.forEach(r -> joiner.add(r.getMapping()));
		String mappingsCSV = joiner.toString();
		String stack = config.getProperty(PROPERTY_KEY_STACK);

		String stackName = stack + "-dns-record-to-stack-mapping";

		/*
		 * If the stack already exists, we to extract the mapping used to build it. This
		 * mapping is used to to determine the order that updates are to be applied. We
		 * use the cloud formation "dependsOn" attribute to control the order of the
		 * updates. Basically, the old mapping is used to build the dependency tree that
		 * will control the order in which changes are made to the nlbs.
		 * 
		 */
		List<RecordToStackMapping> currentMapping = this.cloudFormationClient.describeStack(stackName)
				.map(BindNetworkLoadBalancerBuilderImpl::buildStackMapping).orElse(Collections.emptyList());

		List<RecordToStackMapping> mappings = buildMappingWithDependencies(currentMapping, configMapping);

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

	}

	/**
	 * Given an existing stack build the mapping from the stack's output.
	 * 
	 * @param stack
	 * @return
	 */
	public static List<RecordToStackMapping> buildStackMapping(Stack stack) {
		return stack.getOutputs().stream().filter(o -> MAPPINGS_CSV.equals(o.getOutputKey())).findFirst()
				.map(o -> Arrays.stream(o.getOutputValue().split(","))
						.map(m -> RecordToStackMapping.builder().withMapping(m).build()).collect(Collectors.toList()))
				.orElse(Collections.emptyList());

	}

	/**
	 * When cloud formation builds a stack, it will create all resources in
	 * parallel. This is works when all of the resources all of the resources are
	 * new. However, for the case of record mappings that already exists, the
	 * updates must occur is specific order. For example, if the current mapping of
	 * a stack is: www->100, staging->101 and we want to set the new mapping to:
	 * www->101, staging->102. If this change occurs in parallel we would get an
	 * error: cannot map www to 101 because 101 is already mapped. To prevent this
	 * error, staging must be updated before www. Cloud formation, provides the
	 * "dependsOn" feature to ensure that resources are updated in the correct
	 * order. Therefore, to prevent the above error, we must setup the new template
	 * such that www depends on staging.
	 * <p>
	 * This function will build a mapping that with the correct dependencies set to
	 * ensure that updates are executed in their dependent order.
	 * 
	 * @param oldMapping
	 * @param newMapping
	 * @return
	 */
	public static List<RecordToStackMapping> buildMappingWithDependencies(List<RecordToStackMapping> oldMapping,
			List<RecordToStackMapping> newMapping) {
		if (oldMapping.isEmpty()) {
			return newMapping;
		}
		Set<String> inNew = newMapping.stream().map(n->n.getRecord().getShortName()).collect(Collectors.toSet());
		return newMapping.stream().map(n -> {
			// is this target in the current mapping?
			return oldMapping.stream()
					.filter(o -> o.getTarget().equals(n.getTarget())
							&& !o.getRecord().getShortName().equals(n.getRecord().getShortName())
							&& inNew.contains(o.getRecord().getShortName())
							)
					.findFirst().map(o -> RecordToStackMapping.builder().withMapping(n.getMapping())
							.withDependsOn(o.getRecord().getName()).build())
					.orElse(n);
		}).collect(Collectors.toList());
	}

}
