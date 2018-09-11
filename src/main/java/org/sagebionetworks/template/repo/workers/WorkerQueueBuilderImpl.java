package org.sagebionetworks.template.repo.workers;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_WORKER_RESOURCES;
import static org.sagebionetworks.template.Constants.WORKER_SNS_TOPIC_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.WORKER_SQS_DESCRIPTORS;

import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.LoggerFactory;

public class WorkerQueueBuilderImpl implements WorkerQueueBuilder{
	public static final String TOPIC_NAME_TEMPLATE_PREFIX = "%1$s-%2$s-repo-"; //used to generate SNS topic names

	CloudFormationClient cloudFormationClient;
	VelocityEngine velocityEngine;
	Configuration config;
	Logger logger;

	@Inject
	public WorkerQueueBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine, Configuration config, LoggerFactory loggerFactory) {
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.config = config;
		this.logger = loggerFactory.getLogger(WorkerQueueBuilderImpl.class);
	}

	@Override
	public void buildAndDeploy() {

	}

	public String getFullSnsTopicName(String suffix){
		return String.format(TOPIC_NAME_TEMPLATE_PREFIX, config.getProperty(PROPERTY_KEY_STACK), suffix);
	}

	VelocityContext createSharedContext(WorkerResourceDescriptor workerResourceDescriptor){

		VelocityContext context = new VelocityContext();

		context.put(WORKER_SNS_TOPIC_DESCRIPTORS, workerResourceDescriptor.workerSnsTopicDescriptors);
		context.put(WORKER_SQS_DESCRIPTORS, workerResourceDescriptor.workerQueueDescriptors);

		return context;
	}

	public String generateJSON(WorkerResourceDescriptor descriptor){//TODO: used for work in progress testing. remove
		Template template = velocityEngine.getTemplate(TEMPLATE_WORKER_RESOURCES);

		StringWriter writer = new StringWriter();
		template.merge(createSharedContext(descriptor), writer);
		return writer.toString();
	}



}
