package org.sagebionetworks.template.repo.queues;

import java.util.ArrayList;
import java.util.List;

public class WorkerSNSTopicDescriptor {
	String topicType;
	List<String> subscribedQueues;

	public WorkerSNSTopicDescriptor(String topicType){
		this.topicType = topicType;
		this.subscribedQueues = new ArrayList<>();
	}


	public void addSubscribedQueue(String subscribedQueue){
		this.subscribedQueues.add(subscribedQueue);
	}

	public String getTopicType() {
		return topicType;
	}

	public void setTopicType(String topicType) {
		this.topicType = topicType;
	}

	public List<String> getSubscribedQueues() {
		return subscribedQueues;
	}

	public void setSubscribedQueues(List<String> subscribedQueues) {
		this.subscribedQueues = subscribedQueues;
	}

}
