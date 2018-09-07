package org.sagebionetworks.template.repo.workers;

import java.util.List;

public class WorkerQueueDescriptor { //TODO: maybe WorkerResourceDescriptor?
	private static final int DEFAULT_MAX_FAILURE_COUNT = 10;  //TODO: adjust this number?

	String queueName;
	List<String> topicNamesToSubscribe;
	Integer visibilityTimeoutSec; //todo: can't find code in worker utils that actually sets a default for this?????


	String deadLetterQueueName; //optional
	Integer maxFailureCount; //optional

	Integer oldestMessageInQueueAlarmThresholdSec; //optional


}
