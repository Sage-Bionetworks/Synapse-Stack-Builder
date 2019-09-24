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

	@JsonCreator
	public SqsQueueDescriptor(@JsonProperty(value = "queueName", required=true) String queueName,
							  @JsonProperty(value = "subscribedTopicNames", required=true) List<String> subscribedTopicNames,
							  @JsonProperty(value = "messageVisibilityTimeoutSec", required=true) Integer messageVisibilityTimeoutSec,
							  @JsonProperty("deadLetterQueueMaxFailureCount") Integer deadLetterQueueMaxFailureCount,
							  @JsonProperty("oldestMessageInQueueAlarmThresholdSec") Integer oldestMessageInQueueAlarmThresholdSec) {
		SnsAndSqsNameValidator.validateName(queueName);
		SnsAndSqsNameValidator.validateNames(subscribedTopicNames);

		this.queueName = queueName;
		this.subscribedTopicNames = Collections.unmodifiableSet(new LinkedHashSet<>(subscribedTopicNames));
		this.messageVisibilityTimeoutSec = messageVisibilityTimeoutSec;
		this.deadLetterQueueMaxFailureCount = deadLetterQueueMaxFailureCount;
		this.oldestMessageInQueueAlarmThresholdSec = oldestMessageInQueueAlarmThresholdSec;
		this.subscribedTopicDescriptors = new HashSet<>(subscribedTopicNames.size());
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

	@Override
	public String toString() {
		return "SqsQueueDescriptor{" +
				"queueName='" + queueName + '\'' +
				", subscribedTopicNames=" + subscribedTopicNames +
				", messageVisibilityTimeoutSec=" + messageVisibilityTimeoutSec +
				", deadLetterQueueMaxFailureCount=" + deadLetterQueueMaxFailureCount +
				", oldestMessageInQueueAlarmThresholdSec=" + oldestMessageInQueueAlarmThresholdSec +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SqsQueueDescriptor that = (SqsQueueDescriptor) o;
		return Objects.equals(queueName, that.queueName) &&
				Objects.equals(subscribedTopicNames, that.subscribedTopicNames) &&
				Objects.equals(messageVisibilityTimeoutSec, that.messageVisibilityTimeoutSec) &&
				Objects.equals(deadLetterQueueMaxFailureCount, that.deadLetterQueueMaxFailureCount) &&
				Objects.equals(oldestMessageInQueueAlarmThresholdSec, that.oldestMessageInQueueAlarmThresholdSec);
	}

	@Override
	public int hashCode() {
		return Objects.hash(queueName, subscribedTopicNames, messageVisibilityTimeoutSec, deadLetterQueueMaxFailureCount, oldestMessageInQueueAlarmThresholdSec);
	}


}
