package org.sagebionetworks.template.repo.workers;

import java.util.List;

public class WorkerQueueDescriptor {
	String queueName;
	List<String> snsTopicsToSubscribe;
	Integer visibilityTimeoutSec;

	Integer maxFailureCount; //optional //TODO: probably rename to indicate dead letter

	Integer oldestMessageInQueueAlarmThresholdSec; //optional



	///////////////////////
	// Getters and Setters
	///////////////////////


	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public List<String> getSnsTopicsToSubscribe() {
		return snsTopicsToSubscribe;
	}

	public void setSnsTopicsToSubscribe(List<String> snsTopicsToSubscribe) {
		this.snsTopicsToSubscribe = snsTopicsToSubscribe;
	}

	public Integer getVisibilityTimeoutSec() {
		return visibilityTimeoutSec;
	}

	public void setVisibilityTimeoutSec(Integer visibilityTimeoutSec) {
		this.visibilityTimeoutSec = visibilityTimeoutSec;
	}

	public Integer getMaxFailureCount() {
		return maxFailureCount;
	}

	public void setMaxFailureCount(Integer maxFailureCount) {
		this.maxFailureCount = maxFailureCount;
	}

	public Integer getOldestMessageInQueueAlarmThresholdSec() {
		return oldestMessageInQueueAlarmThresholdSec;
	}

	public void setOldestMessageInQueueAlarmThresholdSec(Integer oldestMessageInQueueAlarmThresholdSec) {
		this.oldestMessageInQueueAlarmThresholdSec = oldestMessageInQueueAlarmThresholdSec;
	}


}
