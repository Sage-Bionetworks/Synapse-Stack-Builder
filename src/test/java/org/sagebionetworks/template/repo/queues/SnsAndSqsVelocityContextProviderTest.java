package org.sagebionetworks.template.repo.queues;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.SNS_TOPIC_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.SQS_QUEUE_DESCRIPTORS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SnsAndSqsVelocityContextProviderTest {

	@Mock
	ObjectMapper mockObjectMapper;

	@Mock
	VelocityContext mockContext;

	@Mock
	SnsAndSqsConfig mockSnsAndSqsConfig;

	String fakeFilePath = "someFilePath";

	@InjectMocks
	SnsAndSqsVelocityContextProvider provider = new SnsAndSqsVelocityContextProvider(fakeFilePath);

	@Captor
	ArgumentCaptor<File> fileCaptor;

	List<SnsTopicDescriptor> snsTopicDescriptors;
	List<SqsQueueDescriptor> sqsQueueDescriptors;
	SnsTopicAndSqsQueueDescriptors descriptors;



	@Before
	public void setUp() throws IOException {
		snsTopicDescriptors = new ArrayList<>();
		sqsQueueDescriptors = new ArrayList<>();
		descriptors = new SnsTopicAndSqsQueueDescriptors(snsTopicDescriptors, sqsQueueDescriptors);

		when(mockObjectMapper.readValue(fileCaptor.capture(), eq(SnsAndSqsConfig.class))).thenReturn(mockSnsAndSqsConfig);
		when(mockSnsAndSqsConfig.convertToDesciptor()).thenReturn(descriptors);
	}


	@Test
	public void testAddToContext(){
		//method under test
		provider.addToContext(mockContext);

		verify(mockSnsAndSqsConfig).convertToDesciptor();
		verify(mockContext).put(SNS_TOPIC_DESCRIPTORS, snsTopicDescriptors);
		verify(mockContext).put(SQS_QUEUE_DESCRIPTORS, sqsQueueDescriptors);

		assertEquals(new File(fakeFilePath), fileCaptor.getValue());
	}


}
