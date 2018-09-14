package org.sagebionetworks.template.repo.queues;

import java.util.List;

public class SnsTopicAndSqsQueueDescriptors {
	List<SnsTopicDescriptor> snsTopicDescriptors;
	List<SqsQueueDescriptor> sqsQueueDescriptors;

	public SnsTopicAndSqsQueueDescriptors(List<SnsTopicDescriptor> snsTopicDescriptors, List<SqsQueueDescriptor> sqsQueueDescriptors) {
		this.snsTopicDescriptors = snsTopicDescriptors;
		this.sqsQueueDescriptors = sqsQueueDescriptors;
	}

	///////////////////////
	// Getters and Setters
	///////////////////////

	public void setSnsTopicDescriptors(List<SnsTopicDescriptor> snsTopicDescriptors) {
		this.snsTopicDescriptors = snsTopicDescriptors;
	}

	public void setSqsQueueDescriptors(List<SqsQueueDescriptor> sqsQueueDescriptors) {
		this.sqsQueueDescriptors = sqsQueueDescriptors;
	}

}
