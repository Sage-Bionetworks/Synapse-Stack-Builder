package org.sagebionetworks.template.repo.queues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import com.google.gson.Gson;
import org.junit.Test;

public class WorkerResourceJsonConfigTest {

	@Test
	public void testSerialization(){
		Gson gson = new Gson();

		WorkerResourceJsonConfig config = new WorkerResourceJsonConfig();
		config.queues = new ArrayList<>();
		config.repositoryChangeTopicTypes = new HashSet<>();

		WorkerQueueJsonConfig queueJsonConfig = new WorkerQueueJsonConfig();
		queueJsonConfig.deadLetterQueueMaxFailureCount = 5;
		queueJsonConfig.messageVisibilityTimeoutSec = 42;
		queueJsonConfig.oldestMessageInQueueAlarmThresholdSec = 35;
		queueJsonConfig.subscribedTopicTypes = Collections.singleton("TEST_TOPIC");

		config.queues.add(queueJsonConfig);

		config.repositoryChangeTopicTypes.add("TEST_TOPIC");

		WorkerResourceJsonConfig deseralized = gson.fromJson(gson.toJson(config), WorkerResourceJsonConfig.class);

		System.out.println(gson.toJson(gson.fromJson("{\"notRelated\":5}", WorkerResourceJsonConfig.class)));

		System.out.println(gson.toJson(deseralized));
	}
}
