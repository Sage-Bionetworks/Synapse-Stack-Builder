package org.sagebionetworks.template.repo.queues;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class SnsTopicDescriptor {
	String topicName;
	Set<String> subscribedQueueCloudformationResources;

	public SnsTopicDescriptor(String topicName){
		this.topicName = topicName;
		this.subscribedQueueCloudformationResources = new LinkedHashSet<>();
	}


	public SnsTopicDescriptor addToSubscribedQueues(String subscribedQueue){
		this.subscribedQueueCloudformationResources.add(subscribedQueue);
		return this;
	}

	public String getTopicName() {
		return topicName;
	}

	public String geTopicNameCloudformationResource(){
		return this.topicName.replaceAll("[^a-zA-Z0-9]", "");
	}

	public Set<String> getSubscribedQueueCloudformationResources() {
		return subscribedQueueCloudformationResources;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SnsTopicDescriptor that = (SnsTopicDescriptor) o;
		return Objects.equals(topicName, that.topicName) &&
				Objects.equals(subscribedQueueCloudformationResources, that.subscribedQueueCloudformationResources);
	}

	@Override
	public int hashCode() {

		return Objects.hash(topicName, subscribedQueueCloudformationResources);
	}
}
