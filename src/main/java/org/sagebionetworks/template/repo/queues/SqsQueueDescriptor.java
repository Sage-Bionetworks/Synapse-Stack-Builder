package org.sagebionetworks.template.repo.queues;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Config read from JSON for a queue
 */
public class SqsQueueDescriptor {
	final String queueName;
	final Set<String> subscribedTopicNames;
	final Integer messageVisibilityTimeoutSec;

	final Integer deadLetterQueueMaxFailureCount;
	final Integer oldestMessageInQueueAlarmThresholdSec;

	@JsonCreator
	public SqsQueueDescriptor(@JsonProperty(value = "queueName", required=true) String queueName,
							  @JsonProperty(value = "subscribedTopicNames", required=true) List<String> subscribedTopicNames,
							  @JsonProperty(value = "messageVisibilityTimeoutSec", required=true) Integer messageVisibilityTimeoutSec,
							  @JsonProperty("deadLetterQueueMaxFailureCount") Integer deadLetterQueueMaxFailureCount,
							  @JsonProperty("oldestMessageInQueueAlarmThresholdSec") Integer oldestMessageInQueueAlarmThresholdSec) {
		this.queueName = queueName;
		this.subscribedTopicNames = Collections.unmodifiableSet(new LinkedHashSet<>(subscribedTopicNames));
		this.messageVisibilityTimeoutSec = messageVisibilityTimeoutSec;
		this.deadLetterQueueMaxFailureCount = deadLetterQueueMaxFailureCount;
		this.oldestMessageInQueueAlarmThresholdSec = oldestMessageInQueueAlarmThresholdSec;
	}

	///////////////////////////////////////////
	// getters are required by Apache Velocity
	///////////////////////////////////////////
	public String getQueueName() {
		return queueName;
	}

	/**
	 * Returns the queueName with only alphanumeric characters [a-zA-Z0-9]
	 * @return
	 */
	public String getQueueNameCloudformationResource(){
		return queueName.replaceAll("[^a-zA-Z0-9]", "");
	}

	public Set<String> getSubscribedTopicNames() {
		return subscribedTopicNames;
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
