package org.sagebionetworks.stack.notifications;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.Subscription;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.TestHelper;
import org.sagebionetworks.stack.config.InputConfiguration;

public class NotificationUtilsTest {
	
	AmazonSNSClient mockClient;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	InputConfiguration config;
	GeneratedResources resources;
	StackInstanceNotificationSetup setup;
	
	@Before
	public void before() throws IOException{
		config = TestHelper.createTestConfig("dev");
		mockClient = factory.createSNSClient();
		resources = new GeneratedResources();
		setup = new StackInstanceNotificationSetup(factory, config, resources);
	}
	
	@Test
	public void testfindSubscriptionDoesNotExist(){
		String topicArn = "arn:123";
		String protocol = "email";
		String endpoint = "testing@domain.com";
		ListSubscriptionsByTopicRequest tRequest = new ListSubscriptionsByTopicRequest().withTopicArn(topicArn);
		ListSubscriptionsByTopicResult result = new ListSubscriptionsByTopicResult().withSubscriptions( new Subscription().withEndpoint("nomatch").withProtocol("noMatch"));
		when(mockClient.listSubscriptionsByTopic(tRequest)).thenReturn(result);
		// For this case it should not be found
		Subscription sub = NotificationUtils.findSubscription(mockClient, topicArn, protocol, endpoint);
		assertNull(sub);
	}
	
	@Test
	public void testfindSubscriptionFound(){
		String topicArn = "arn:123";
		String protocol = "email";
		String endpoint = "testing@domain.com";
		ListSubscriptionsByTopicRequest tRequest = new ListSubscriptionsByTopicRequest().withTopicArn(topicArn);
		Subscription expected = new Subscription().withEndpoint(endpoint).withProtocol(protocol);
		ListSubscriptionsByTopicResult result = new ListSubscriptionsByTopicResult().withSubscriptions(expected);
		when(mockClient.listSubscriptionsByTopic(tRequest)).thenReturn(result);
		// For this case it should not be found
		Subscription sub = NotificationUtils.findSubscription(mockClient, topicArn, protocol, endpoint);
		assertEquals(expected, sub);
	}
	
	@Test
	public void testCreateSubscriptionDoesNotExist(){
		String topicArn = "arn:123";
		String protocol = "email";
		String endpoint = "testing@domain.com";
		ListSubscriptionsByTopicRequest tRequest = new ListSubscriptionsByTopicRequest().withTopicArn(topicArn);
		ListSubscriptionsByTopicResult result = new ListSubscriptionsByTopicResult().withSubscriptions( new Subscription().withEndpoint("nomatch").withProtocol("noMatch"));
		when(mockClient.listSubscriptionsByTopic(tRequest)).thenReturn(result);
		
		// This should call create
		SubscribeRequest expectedRequest = new SubscribeRequest();
		expectedRequest.setTopicArn(topicArn);
		expectedRequest.setProtocol(protocol);
		expectedRequest.setEndpoint(endpoint);
		Subscription sub = NotificationUtils.createSubScription(mockClient, topicArn, protocol, endpoint);
		assertNull(sub);
		verify(mockClient, times(1)).subscribe(expectedRequest);
	}

	
	@Test
	public void testCreateSubscriptionAlreadyExists(){
		String topicArn = "arn:123";
		String protocol = "email";
		String endpoint = "testing@domain.com";
		ListSubscriptionsByTopicRequest tRequest = new ListSubscriptionsByTopicRequest().withTopicArn(topicArn);
		Subscription expected = new Subscription().withEndpoint(endpoint).withProtocol(protocol);
		ListSubscriptionsByTopicResult result = new ListSubscriptionsByTopicResult().withSubscriptions(expected);
		when(mockClient.listSubscriptionsByTopic(tRequest)).thenReturn(result);
		
		// This should not call create because it already exists
		SubscribeRequest expectedRequest = new SubscribeRequest();
		expectedRequest.setTopicArn(topicArn);
		expectedRequest.setProtocol(protocol);
		expectedRequest.setEndpoint(endpoint);
		Subscription sub = NotificationUtils.createSubScription(mockClient, topicArn, protocol, endpoint);
		assertEquals(expected, sub);
		// Subscribe should not have been called!
		verify(mockClient, times(0)).subscribe(expectedRequest);
	}
	
}
