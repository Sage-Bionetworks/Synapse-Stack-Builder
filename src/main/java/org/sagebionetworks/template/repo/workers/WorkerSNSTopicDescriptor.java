package org.sagebionetworks.template.repo.workers;

import java.util.List;

public class WorkerSNSTopicDescriptor {
	String topicName;
	List<String> subscribedQueues;


	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public List<String> getSubscribedQueues() {
		return subscribedQueues;
	}

	public void setSubscribedQueues(List<String> subscribedQueues) {
		this.subscribedQueues = subscribedQueues;
	}

}
