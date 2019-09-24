package org.sagebionetworks.template.repo.queues;

import static org.sagebionetworks.template.Constants.*;
import static org.sagebionetworks.template.Constants.SQS_QUEUE_DESCRIPTORS;

import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;

/**
 * Provides velocity context for SNS topics and SQS queues that are subscribed to that topic
 */
public class SnsAndSqsVelocityContextProvider implements VelocityContextProvider {
	SnsAndSqsConfig snsAndSqsConfig;

	@Inject
	public SnsAndSqsVelocityContextProvider(SnsAndSqsConfig snsAndSqsConfig){
		this.snsAndSqsConfig = snsAndSqsConfig;
	}


	@Override
	public void addToContext(VelocityContext context) {
		context.put(SNS_TOPIC_DESCRIPTORS, snsAndSqsConfig.processSnsTopicDescriptors());
		context.put(SNS_GLOBAL_TOPIC_DESCRIPTORS, snsAndSqsConfig.processSnsGlobalTopicDescriptors());
		context.put(SQS_QUEUE_DESCRIPTORS, snsAndSqsConfig.getQueueDescriptors());
	}
}
