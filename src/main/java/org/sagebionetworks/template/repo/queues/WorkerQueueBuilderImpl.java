package org.sagebionetworks.template.repo.queues;

import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_WORKER_RESOURCES;
import static org.sagebionetworks.template.Constants.WORKER_SNS_TOPIC_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.WORKER_SQS_DESCRIPTORS;

import java.io.StringWriter;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.LoggerFactory;

public class WorkerQueueBuilderImpl {
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

	VelocityContext createSharedContext(SnsTopicAndQueueDescriptor snsTopicAndQueueDescriptor){

		VelocityContext context = new VelocityContext();

		context.put(WORKER_SNS_TOPIC_DESCRIPTORS, snsTopicAndQueueDescriptor.snsTopicDescriptors);
		context.put(WORKER_SQS_DESCRIPTORS, snsTopicAndQueueDescriptor.queueDescriptors);
		context.put(STACK, config.getProperty(PROPERTY_KEY_STACK));
		context.put(INSTANCE, config.getProperty(PROPERTY_KEY_INSTANCE));

		return context;
	}

	public String generateJSON(SnsTopicAndQueueDescriptor descriptor){//TODO: used for work in progress testing. remove
		Template template = velocityEngine.getTemplate(TEMPLATE_WORKER_RESOURCES);

		VelocityContext context = createSharedContext(descriptor);

		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}



}
