package org.sagebionetworks.template.repo.workers;

import java.util.List;

public class WorkerQueueDescriptor { //TODO: maybe WorkerResourceDescriptor?
	//TODO; USE GSON TO SERIALZIE? NVM this is the fp
	private static final int DEFAULT_MAX_FAILURE_COUNT = 10;  //TODO: adjust this number?

	String queueName;
	List<String> snsTopicSuffixesToSubscribe;
	Integer visibilityTimeoutSec; //todo: can't find code in worker utils that actually sets a default for this?????

	String deadLetterQueueName; //optional
	Integer maxFailureCount; //optional

	Integer oldestMessageInQueueAlarmThresholdSec; //optional

	public void validate(){
		//TODO:
	}



	///////////////////////
	// Getters and Setters
	///////////////////////


	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public List<String> getSnsTopicSuffixesToSubscribe() {
		return snsTopicSuffixesToSubscribe;
	}

	public void setSnsTopicSuffixesToSubscribe(List<String> snsTopicSuffixesToSubscribe) {
		this.snsTopicSuffixesToSubscribe = snsTopicSuffixesToSubscribe;
	}

	public Integer getVisibilityTimeoutSec() {
		return visibilityTimeoutSec;
	}

	public void setVisibilityTimeoutSec(Integer visibilityTimeoutSec) {
		this.visibilityTimeoutSec = visibilityTimeoutSec;
	}

	public String getDeadLetterQueueName() {
		return deadLetterQueueName;
	}

	public void setDeadLetterQueueName(String deadLetterQueueName) {
		this.deadLetterQueueName = deadLetterQueueName;
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
