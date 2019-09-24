package org.sagebionetworks.template.repo.queues;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.template.Constants;

public class SnsTopicDescriptor {
	String topicName;
	boolean global;
	Set<String> subscribedQueueNames;

	public SnsTopicDescriptor(String topicName){
		this.topicName = topicName;
		this.subscribedQueueNames = new LinkedHashSet<>();
	}


	public SnsTopicDescriptor addToSubscribedQueues(String subscribedQueue){
		this.subscribedQueueNames.add(subscribedQueue);
		return this;
	}

	public String getTopicName() {
		return topicName;
	}
	
	public boolean isGlobal() {
		return global;
	}
	
	public SnsTopicDescriptor setGlobal(boolean global) {
		this.global = global;
		return this;
	}

	public String getTopicReferenceName(){
		return Constants.createCamelCaseName(topicName, "_");
	}

	public List<String> getSubscribedQueueReferenceNames() {
		return Constants.createCamelCaseName(subscribedQueueNames, "_");
	}


	@Override
	public int hashCode() {
		return Objects.hash(global, subscribedQueueNames, topicName);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SnsTopicDescriptor other = (SnsTopicDescriptor) obj;
		return global == other.global && Objects.equals(subscribedQueueNames, other.subscribedQueueNames)
				&& Objects.equals(topicName, other.topicName);
	}


	
	
}
