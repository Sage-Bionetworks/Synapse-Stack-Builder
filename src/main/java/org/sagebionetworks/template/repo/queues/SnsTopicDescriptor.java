package org.sagebionetworks.template.repo.queues;

import java.util.ArrayList;
import java.util.List;

public class SnsTopicDescriptor {
	String topicName;
	List<String> subscribedQueueNames;

	public SnsTopicDescriptor(String topicName){
		this.topicName = topicName;
		this.subscribedQueueNames = new ArrayList<>();
	}


	public void addSubscribedQueue(String subscribedQueue){
		this.subscribedQueueNames.add(subscribedQueue);
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public List<String> getSubscribedQueueNames() {
		return subscribedQueueNames;
	}

	public void setSubscribedQueueNames(List<String> subscribedQueueNames) {
		this.subscribedQueueNames = subscribedQueueNames;
	}

}
