package org.sagebionetworks.template.repo.queues;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sagebionetworks.template.Constants;

/**
 * Config read from JSON for a queue
 */
public class SqsQueueDescriptor {
	final String queueName;
	final Set<String> subscribedTopicNames;
	final Integer messageVisibilityTimeoutSec;

	final Integer deadLetterQueueMaxFailureCount;
	final Integer oldestMessageInQueueAlarmThresholdSec;
	
	final Set<SnsTopicDescriptor> subscribedTopicDescriptors;
	final Integer messageRetentionPeriodSec;

	@JsonCreator
	public SqsQueueDescriptor(@JsonProperty(value = "queueName", required=true) String queueName,
							  @JsonProperty(value = "subscribedTopicNames", required=true) List<String> subscribedTopicNames,
							  @JsonProperty(value = "messageVisibilityTimeoutSec", required=true) Integer messageVisibilityTimeoutSec,
							  @JsonProperty("deadLetterQueueMaxFailureCount") Integer deadLetterQueueMaxFailureCount,
							  @JsonProperty("oldestMessageInQueueAlarmThresholdSec") Integer oldestMessageInQueueAlarmThresholdSec,
							  @JsonProperty("messageRetentionPeriodSec") Integer messageRetentionPeriodSec) {
		SnsAndSqsNameValidator.validateName(queueName);
		SnsAndSqsNameValidator.validateNames(subscribedTopicNames);

		this.queueName = queueName;
		this.subscribedTopicNames = Collections.unmodifiableSet(new LinkedHashSet<>(subscribedTopicNames));
		this.messageVisibilityTimeoutSec = messageVisibilityTimeoutSec;
		this.deadLetterQueueMaxFailureCount = deadLetterQueueMaxFailureCount;
		this.oldestMessageInQueueAlarmThresholdSec = oldestMessageInQueueAlarmThresholdSec;
		this.subscribedTopicDescriptors = new HashSet<>(subscribedTopicNames.size());
		this.messageRetentionPeriodSec = messageRetentionPeriodSec;
	}
	
	void addTopicDescriptor(SnsTopicDescriptor topicDescriptor) {
		this.subscribedTopicDescriptors.add(topicDescriptor);
	}

	///////////////////////////////////////////
	// getters are only used by Apache Velocity
	///////////////////////////////////////////
	public String getQueueName() {
		return queueName;
	}

	public String getQueueReferenceName(){
		return Constants.createCamelCaseName(queueName, "_");
	}

	public Set<SnsTopicDescriptor> getSubscribedTopicDescriptors() {
		return subscribedTopicDescriptors;
	}

	public Integer getMessageVisibilityTimeoutSec() {
		return messageVisibilityTimeoutSec;
	}

	public Integer getDeadLetterQueueMaxFailureCount() {
		return deadLetterQueueMaxFailureCount;
	}

	public Integer getOldestMessageInQueueAlarmThresholdSec() {
		return oldestMessageInQueueAlarmThresholdSec;
	}
	
	public Integer getMessageRetentionPeriodSec() {
		return messageRetentionPeriodSec;
	}

	@Override
	public String toString() {
		return "SqsQueueDescriptor [queueName=" + queueName + ", subscribedTopicNames=" + subscribedTopicNames
				+ ", messageVisibilityTimeoutSec=" + messageVisibilityTimeoutSec + ", deadLetterQueueMaxFailureCount="
				+ deadLetterQueueMaxFailureCount + ", oldestMessageInQueueAlarmThresholdSec="
				+ oldestMessageInQueueAlarmThresholdSec + ", subscribedTopicDescriptors=" + subscribedTopicDescriptors
				+ ", messageRetentionPeriodSec=" + messageRetentionPeriodSec + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SqsQueueDescriptor)) {
			return false;
		}
		SqsQueueDescriptor other = (SqsQueueDescriptor) obj;
		return Objects.equals(deadLetterQueueMaxFailureCount, other.deadLetterQueueMaxFailureCount)
				&& Objects.equals(messageRetentionPeriodSec, other.messageRetentionPeriodSec)
				&& Objects.equals(messageVisibilityTimeoutSec, other.messageVisibilityTimeoutSec)
				&& Objects.equals(oldestMessageInQueueAlarmThresholdSec, other.oldestMessageInQueueAlarmThresholdSec)
				&& Objects.equals(queueName, other.queueName)
				&& Objects.equals(subscribedTopicDescriptors, other.subscribedTopicDescriptors)
				&& Objects.equals(subscribedTopicNames, other.subscribedTopicNames);
	}

	@Override
	public int hashCode() {
		return Objects.hash(deadLetterQueueMaxFailureCount, messageRetentionPeriodSec, messageVisibilityTimeoutSec,
				oldestMessageInQueueAlarmThresholdSec, queueName, subscribedTopicDescriptors, subscribedTopicNames);
	}


}
