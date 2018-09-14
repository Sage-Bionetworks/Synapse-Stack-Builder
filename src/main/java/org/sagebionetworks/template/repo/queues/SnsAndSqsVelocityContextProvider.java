package org.sagebionetworks.template.repo.queues;

import static org.sagebionetworks.template.Constants.SNS_TOPIC_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.SQS_QUEUE_DESCRIPTORS;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.repo.VelocityContextProvider;

/**
 * Provides velocity context for SNS topics and SQS queues that are subscribed to that topic
 */
public class SnsAndSqsVelocityContextProvider implements VelocityContextProvider {
	//used to read JSON
	ObjectMapper objectMapper;

	String jsonFilePath;

	public SnsAndSqsVelocityContextProvider(String jsonFilePath){
		this.jsonFilePath = jsonFilePath;
		objectMapper = new ObjectMapper();
	}


	@Override
	public void addToContext(VelocityContext context) {
		try {
			SnsAndSqsConfig config = objectMapper.readValue(new File(jsonFilePath), SnsAndSqsConfig.class);
			SnsTopicAndSqsQueueDescriptors snsTopicAndQueueDescriptor = config.convertToDesciptor();
			context.put(SNS_TOPIC_DESCRIPTORS, snsTopicAndQueueDescriptor.snsTopicDescriptors);
			context.put(SQS_QUEUE_DESCRIPTORS, snsTopicAndQueueDescriptor.sqsQueueDescriptors);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
