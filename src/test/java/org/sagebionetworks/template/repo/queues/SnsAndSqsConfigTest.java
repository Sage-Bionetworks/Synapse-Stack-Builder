package org.sagebionetworks.template.repo.queues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class SnsAndSqsConfigTest {

	final String queueName = "queueNameEcksDee";
	final Integer messageVisibilityTimeoutSec = 42;
	final Integer deadLetterQueueMaxFailureCount = 24;
	final Integer oldestMessGeInQueueAlarmThresholdSec = 65;

	final String topicName1 = "topic1";
	final String topicName2 = "topic2";

	SnsAndSqsConfig snsAndSqsConfig;


	@Before
	public void setUp(){
	}

	@Test
	public void testSerializationAndDeserlaization() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();

		//create original
		QueueConfig queueConfig = new QueueConfig(queueName, Collections.singleton(topicName1), messageVisibilityTimeoutSec, deadLetterQueueMaxFailureCount, oldestMessGeInQueueAlarmThresholdSec);
		snsAndSqsConfig = new SnsAndSqsConfig(Collections.singleton(topicName1), Collections.singletonList(queueConfig));

		//write original to json
		StringWriter originalStringWriter = new StringWriter();
		objectMapper.writeValue(originalStringWriter, snsAndSqsConfig);

		//deserialize from json into object
		SnsAndSqsConfig deseralized = objectMapper.readValue(originalStringWriter.toString(), SnsAndSqsConfig.class);

		//compare original json to json of
		assertEquals(snsAndSqsConfig, deseralized);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertToDescriptor_TopicNamesNotAllListed(){
		//queue uses 2 queues but only 1 is listed in SnsAndSqsConfig
		Set<String> usedTopics = Sets.newHashSet(topicName1, topicName2);
		Set<String> listedTopics = Collections.singleton(topicName1);

		QueueConfig queueConfig = new QueueConfig(queueName, usedTopics, messageVisibilityTimeoutSec, deadLetterQueueMaxFailureCount, oldestMessGeInQueueAlarmThresholdSec);
		snsAndSqsConfig = new SnsAndSqsConfig(listedTopics, Collections.singletonList(queueConfig));

		//method under test
		snsAndSqsConfig.convertToDesciptor();
	}

	@Test
	public void testConvertToDescriptor(){

		Set<String> listedTopics = Sets.newHashSet(topicName1, topicName2);


		Set<String> queue1Topics = Sets.newHashSet(topicName1, topicName2);
		Set<String> queue2Topics = Sets.newHashSet(topicName2);

		QueueConfig queue1Config = new QueueConfig(queueName, queue1Topics, messageVisibilityTimeoutSec, deadLetterQueueMaxFailureCount, oldestMessGeInQueueAlarmThresholdSec);

		String queueName2 = "theOtherQueue";
		QueueConfig queue2Config = new QueueConfig(queueName2, queue2Topics, messageVisibilityTimeoutSec, deadLetterQueueMaxFailureCount, oldestMessGeInQueueAlarmThresholdSec);

		snsAndSqsConfig = new SnsAndSqsConfig(listedTopics, Arrays.asList(queue1Config, queue2Config));

		//method under test
		SnsTopicAndSqsQueueDescriptors descriptors = snsAndSqsConfig.convertToDesciptor();

		SqsQueueDescriptor expectedQueue1Descriptor = new SqsQueueDescriptor(queue1Config);
		SqsQueueDescriptor expectedQueue2Descriptor = new SqsQueueDescriptor(queue2Config);

		SnsTopicDescriptor expecteTopic1Descriptor = new SnsTopicDescriptor(topicName1).withSubscribedQueue(queueName);
		SnsTopicDescriptor expecteTopic2Descriptor = new SnsTopicDescriptor(topicName2).withSubscribedQueue(queueName).withSubscribedQueue(queueName2);

		assertTrue(descriptors.snsTopicDescriptors.contains(expecteTopic1Descriptor));
		assertTrue(descriptors.snsTopicDescriptors.contains(expecteTopic2Descriptor));
		assertTrue(descriptors.sqsQueueDescriptors.contains(expectedQueue1Descriptor));
		assertTrue(descriptors.sqsQueueDescriptors.contains(expectedQueue2Descriptor));
	}





}
