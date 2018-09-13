package org.sagebionetworks.template.repo.queues;

import java.util.List;

public class SnsTopicAndQueueDescriptor {
	List<SnsTopicDescriptor> snsTopicDescriptors;
	List<QueueDescriptor> queueDescriptors;

	public SnsTopicAndQueueDescriptor(List<SnsTopicDescriptor> snsTopicDescriptors, List<QueueDescriptor> queueDescriptors) {
		this.snsTopicDescriptors = snsTopicDescriptors;
		this.queueDescriptors = queueDescriptors;
	}

	///////////////////////
	// Getters and Setters
	///////////////////////

	public void setSnsTopicDescriptors(List<SnsTopicDescriptor> snsTopicDescriptors) {
		this.snsTopicDescriptors = snsTopicDescriptors;
	}

	public void setQueueDescriptors(List<QueueDescriptor> queueDescriptors) {
		this.queueDescriptors = queueDescriptors;
	}

}
