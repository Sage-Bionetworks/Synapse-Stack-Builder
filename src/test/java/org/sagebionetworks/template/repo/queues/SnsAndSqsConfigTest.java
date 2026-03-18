package org.sagebionetworks.template.repo.queues;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

}
