package org.sagebionetworks.template.repo.queues;

import static org.sagebionetworks.template.Constants.WORKER_SNS_TOPIC_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.WORKER_SQS_DESCRIPTORS;

import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.repo.VelocityContextProvider;

/**
 * Provides velocity context for SNS topics and SQS queues that are subscribed to that topic
 */
public class SnsAndSqsVelocityContextProvider implements VelocityContextProvider {


	public SnsAndSqsVelocityContextProvider(){

	}


	@Override
	public void addToContext(VelocityContext context) {
		WorkerResourceDescriptor workerResourceDescriptor;

		context.put(WORKER_SNS_TOPIC_DESCRIPTORS, workerResourceDescriptor.workerSnsTopicDescriptors);
		context.put(WORKER_SQS_DESCRIPTORS, workerResourceDescriptor.workerQueueDescriptors);
	}
}
