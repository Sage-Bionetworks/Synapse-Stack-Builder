package org.sagebionetworks.template.repo.queues;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SnsTopicDescriptor {
	String topicName;
	Set<String> subscribedQueueNames;

	public SnsTopicDescriptor(String topicName){
		this.topicName = topicName;
		this.subscribedQueueNames = new HashSet<>();
	}


	public SnsTopicDescriptor withSubscribedQueue(String subscribedQueue){
		this.subscribedQueueNames.add(subscribedQueue);
		return this;
	}

	public String getTopicName() {
		return topicName;
	}

	public Set<String> getSubscribedQueueNames() {
		return subscribedQueueNames;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SnsTopicDescriptor that = (SnsTopicDescriptor) o;
		return Objects.equals(topicName, that.topicName) &&
				Objects.equals(subscribedQueueNames, that.subscribedQueueNames);
	}

	@Override
	public int hashCode() {

		return Objects.hash(topicName, subscribedQueueNames);
	}
}
