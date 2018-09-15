package org.sagebionetworks.template.repo.queues;

import static org.sagebionetworks.template.Constants.SNS_TOPIC_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.SQS_QUEUE_DESCRIPTORS;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.repo.VelocityContextProvider;

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
		context.put(SQS_QUEUE_DESCRIPTORS, snsAndSqsConfig.getQueueDescriptors());
	}
}
