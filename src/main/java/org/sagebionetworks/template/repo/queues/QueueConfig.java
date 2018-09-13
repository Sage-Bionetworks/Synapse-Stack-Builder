package org.sagebionetworks.template.repo.queues;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QueueConfig {
	public final String queueName;
	public final Set<String> subscribedTopicNames;
	public final Integer messageVisibilityTimeoutSec;

	public final Integer deadLetterQueueMaxFailureCount; //todo: test this field is not required
	public final Integer oldestMessageInQueueAlarmThresholdSec;

	@JsonCreator
	public QueueConfig(@JsonProperty(value = "queueName", required=true) String queueName,
					   @JsonProperty(value = "subscribedTopicNames", required=true) Set<String> subscribedTopicNames,
					   @JsonProperty(value = "messageVisibilityTimeoutSec", required=true) Integer messageVisibilityTimeoutSec,
					   @JsonProperty("deadLetterQueueMaxFailureCount") Integer deadLetterQueueMaxFailureCount,
					   @JsonProperty("oldestMessageInQueueAlarmThresholdSec") Integer oldestMessageInQueueAlarmThresholdSec) {
		this.queueName = queueName;
		this.subscribedTopicNames = Collections.unmodifiableSet(subscribedTopicNames);
		this.messageVisibilityTimeoutSec = messageVisibilityTimeoutSec;
		this.deadLetterQueueMaxFailureCount = deadLetterQueueMaxFailureCount;
		this.oldestMessageInQueueAlarmThresholdSec = oldestMessageInQueueAlarmThresholdSec;
	}

	@Override
	public String toString() {
		return "QueueConfig{" +
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
		QueueConfig that = (QueueConfig) o;
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
