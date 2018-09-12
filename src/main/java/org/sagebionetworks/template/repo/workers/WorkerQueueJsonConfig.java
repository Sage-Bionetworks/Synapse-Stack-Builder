package org.sagebionetworks.template.repo.workers;

import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

public class WorkerQueueJsonConfig {
	String queueWorkerName;

	Set<String> subscribedTopicTypes;
	Integer messageVisibilityTimeoutSec;

	Integer deadLetterQueueMaxFailureCount;
	Integer oldestMessageInQueueAlarmThresholdSec;

	public void validateNotNull(){
		if(queueWorkerName == null){
			throw new IllegalArgumentException("queueWorkerName can not be null");
		}

		if(CollectionUtils.isEmpty(subscribedTopicTypes)){
			throw new IllegalArgumentException("subscribedTopicTypes for " + queueWorkerName + " can not be null or empty");
		}

		if(messageVisibilityTimeoutSec == null){
			throw new IllegalArgumentException("messageVisibilityTimeoutSec for " + queueWorkerName + " can not be null");
		}
	}
}
