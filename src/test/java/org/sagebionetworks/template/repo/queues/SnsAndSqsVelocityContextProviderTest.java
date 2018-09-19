package org.sagebionetworks.template.repo.queues;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.SNS_TOPIC_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.SQS_QUEUE_DESCRIPTORS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SnsAndSqsVelocityContextProviderTest {

	@Mock
	ObjectMapper mockObjectMapper;

	@Mock
	VelocityContext mockContext;

	@Mock
	SnsAndSqsConfig mockSnsAndSqsConfig;

	@InjectMocks
	SnsAndSqsVelocityContextProvider provider;

	List<SnsTopicDescriptor> snsTopicDescriptors;
	List<SqsQueueDescriptor> sqsQueueDescriptors;



	@Before
	public void setUp() throws IOException {
		snsTopicDescriptors = new ArrayList<>();
		sqsQueueDescriptors = new ArrayList<>();

		when(mockSnsAndSqsConfig.processSnsTopicDescriptors()).thenReturn(snsTopicDescriptors);
		when(mockSnsAndSqsConfig.getQueueDescriptors()).thenReturn(sqsQueueDescriptors);
	}


	@Test
	public void testAddToContext(){
		//method under test
		provider.addToContext(mockContext);

		verify(mockSnsAndSqsConfig).processSnsTopicDescriptors();
		verify(mockContext).put(SNS_TOPIC_DESCRIPTORS, snsTopicDescriptors);
		verify(mockContext).put(SQS_QUEUE_DESCRIPTORS, sqsQueueDescriptors);

	}


}
