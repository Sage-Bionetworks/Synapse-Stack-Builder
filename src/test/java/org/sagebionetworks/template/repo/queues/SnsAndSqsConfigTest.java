package org.sagebionetworks.template.repo.queues;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class SnsAndSqsConfigTest {

	final String queueName = "queueNameEcksDee";
	final Integer messageVisibilityTimeoutSec = 42;
	final Integer deadLetterQueueMaxFailureCount = 24;
	final Integer oldestMessGeInQueueAlarmThresholdSec = 65;
	
	private Integer messageRetentionPeriodSec = 1209600;

	final String topicName1 = "topic1";
	final String topicName2 = "topic2";

	SnsAndSqsConfig snsAndSqsConfig;


	@Test
	public void testConstructor_SnsNamesValidation(){
		SqsQueueDescriptor sqsQueueDescriptor = new SqsQueueDescriptor(queueName, Collections.emptyList(), messageVisibilityTimeoutSec, deadLetterQueueMaxFailureCount, oldestMessGeInQueueAlarmThresholdSec,messageRetentionPeriodSec);

		assertThrows(IllegalArgumentException.class, ()->{
			new SnsAndSqsConfig(Arrays.asList("BAD-NAME"), Collections.emptyList(), Arrays.asList(sqsQueueDescriptor));
		});
	}

	@Test
	public void testProcessSnsTopicDescriptors_TopicNamesNotAllListed(){
		//queue uses 2 queues but only 1 is listed in SnsAndSqsConfig
		List<String> declaredTopics = Arrays.asList(topicName1);

		List<String> usedTopics = Arrays.asList(topicName1, topicName2);

		SqsQueueDescriptor sqsQueueDescriptor = new SqsQueueDescriptor(queueName, usedTopics, messageVisibilityTimeoutSec, deadLetterQueueMaxFailureCount, oldestMessGeInQueueAlarmThresholdSec, messageRetentionPeriodSec);
		snsAndSqsConfig = new SnsAndSqsConfig(declaredTopics, Collections.emptyList(), Collections.singletonList(sqsQueueDescriptor));

		String message = assertThrows(IllegalArgumentException.class, ()->{
			//method under test
			snsAndSqsConfig.processSnsTopicDescriptors();
		}).getMessage();
		assertEquals("topic2 listed in queueNameEcksDee.subscribedTopicNames does not exist in your previously defined snsTopicNames [topic1]", message);

	}

	@Test
	public void testProcessSnsTopicDescriptors(){

		List<String> declaredTopics = Arrays.asList(topicName1, topicName2);

		List<String> queue1Topics = Arrays.asList(topicName1, topicName2);
		List<String> queue2Topics = Arrays.asList(topicName2);

		SqsQueueDescriptor queue1Config = new SqsQueueDescriptor(queueName, queue1Topics, messageVisibilityTimeoutSec, deadLetterQueueMaxFailureCount, oldestMessGeInQueueAlarmThresholdSec, messageRetentionPeriodSec);

		String queueName2 = "theOtherQueue";
		SqsQueueDescriptor queue2Config = new SqsQueueDescriptor(queueName2, queue2Topics, messageVisibilityTimeoutSec, deadLetterQueueMaxFailureCount, oldestMessGeInQueueAlarmThresholdSec, messageRetentionPeriodSec);

		snsAndSqsConfig = new SnsAndSqsConfig(declaredTopics,Collections.emptyList(), Arrays.asList(queue1Config, queue2Config));

		//method under test
		List<SnsTopicDescriptor> descriptors = snsAndSqsConfig.processSnsTopicDescriptors();

		SnsTopicDescriptor expectedTopic1Descriptor = new SnsTopicDescriptor(topicName1).addToSubscribedQueues(queueName);
		SnsTopicDescriptor expectedTopic2Descriptor = new SnsTopicDescriptor(topicName2).addToSubscribedQueues(queueName).addToSubscribedQueues(queueName2);

		assertEquals(Arrays.asList(expectedTopic1Descriptor, expectedTopic2Descriptor), descriptors);
	}

	@Test
	public void testSearchIndexLifecycleQueueDescriptor() {
		List<String> allTopics = Arrays.asList("ENTITY");
		List<String> subscribedTopics = Arrays.asList("ENTITY");

		SqsQueueDescriptor searchIndexLifecycleQueue = new SqsQueueDescriptor(
				"SEARCH_INDEX_LIFECYCLE",
				subscribedTopics,
				300,      // messageVisibilityTimeoutSec
				5,        // deadLetterQueueMaxFailureCount
				null,     // oldestMessageInQueueAlarmThresholdSec
				null      // messageRetentionPeriodSec
		);

		SnsAndSqsConfig config = new SnsAndSqsConfig(allTopics, Collections.emptyList(),
				Collections.singletonList(searchIndexLifecycleQueue));

		// Verify subscribed topic
		Set<String> expectedTopics = new LinkedHashSet<>(Arrays.asList("ENTITY"));
		assertEquals(expectedTopics, searchIndexLifecycleQueue.subscribedTopicNames);
		assertEquals(1, searchIndexLifecycleQueue.subscribedTopicNames.size());

		// Verify configuration values
		assertEquals(300, searchIndexLifecycleQueue.getMessageVisibilityTimeoutSec());
		assertEquals(5, searchIndexLifecycleQueue.getDeadLetterQueueMaxFailureCount());
		assertNull(searchIndexLifecycleQueue.getOldestMessageInQueueAlarmThresholdSec());
		assertNull(searchIndexLifecycleQueue.getMessageRetentionPeriodSec());

		// Verify processSnsTopicDescriptors succeeds
		List<SnsTopicDescriptor> descriptors = config.processSnsTopicDescriptors();
		assertEquals(1, descriptors.size());
	}

	@Test
	public void testSearchQueryQueueDescriptor() {
		SqsQueueDescriptor searchQueryQueue = new SqsQueueDescriptor(
				"SEARCH_QUERY",
				Collections.emptyList(),
				120,      // messageVisibilityTimeoutSec
				null,     // deadLetterQueueMaxFailureCount
				30,       // oldestMessageInQueueAlarmThresholdSec
				null      // messageRetentionPeriodSec
		);

		SnsAndSqsConfig config = new SnsAndSqsConfig(Collections.emptyList(), Collections.emptyList(),
				Collections.singletonList(searchQueryQueue));

		// Verify no subscribed topics (async job queue)
		assertEquals(Collections.emptySet(), searchQueryQueue.subscribedTopicNames);
		assertEquals(0, searchQueryQueue.subscribedTopicNames.size());

		// Verify configuration values
		assertEquals(120, searchQueryQueue.getMessageVisibilityTimeoutSec());
		assertNull(searchQueryQueue.getDeadLetterQueueMaxFailureCount());
		assertEquals(30, searchQueryQueue.getOldestMessageInQueueAlarmThresholdSec());
		assertNull(searchQueryQueue.getMessageRetentionPeriodSec());

		// Verify processSnsTopicDescriptors succeeds with no topics
		List<SnsTopicDescriptor> descriptors = config.processSnsTopicDescriptors();
		assertEquals(0, descriptors.size());
	}

}
