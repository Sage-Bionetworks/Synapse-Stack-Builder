package org.sagebionetworks.template.repo.queues;

import static org.sagebionetworks.template.Constants.*;
import static org.sagebionetworks.template.Constants.SQS_QUEUE_DESCRIPTORS;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
		List<SnsTopicDescriptor> topics = snsAndSqsConfig.processSnsTopicDescriptors();
		
		List<SnsTopicDescriptor> stackTopics = new ArrayList<>(); 
		List<SnsTopicDescriptor> globalTopics = new ArrayList<>();
		
		topics.forEach(topic -> {
			if (topic.isGlobal()) {
				globalTopics.add(topic);
			} else { 
				stackTopics.add(topic);
			}
		});
		
		context.put(SNS_TOPIC_DESCRIPTORS, stackTopics);
		context.put(SNS_GLOBAL_TOPIC_DESCRIPTORS, globalTopics);
		
		context.put(SQS_QUEUE_DESCRIPTORS, snsAndSqsConfig.getQueueDescriptors());
	}
}
