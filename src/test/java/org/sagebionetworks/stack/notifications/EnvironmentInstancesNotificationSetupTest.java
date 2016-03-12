package org.sagebionetworks.stack.notifications;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.Subscription;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import static org.mockito.Mockito.when;
import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.Constants;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.StackEnvironmentType;
import org.sagebionetworks.stack.TestHelper;
import org.sagebionetworks.stack.config.InputConfiguration;
import static org.mockito.Mockito.verify;

public class EnvironmentInstancesNotificationSetupTest {
	
	AmazonSNSClient mockClient;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	InputConfiguration config;
	GeneratedResources resources;
	EnvironmentInstancesNotificationSetup setup;
	
	@Before
	public void before() throws IOException{
		config = TestHelper.createTestConfig("dev");
		mockClient = factory.createSNSClient();
		resources = new GeneratedResources();
		setup = new EnvironmentInstancesNotificationSetup(factory, config, resources);
	}
	
	@Test
	public void testSetupResources() throws InterruptedException {
		
		CreateTopicRequest expectedReqPortal = expectedReq(StackEnvironmentType.PORTAL, config.getEnvironmentInstanceNotificationTopicName(StackEnvironmentType.PORTAL));
		CreateTopicResult expectedResPortal = setupExpectedRes(StackEnvironmentType.PORTAL, expectedReqPortal);
		CreateTopicRequest expectedReqRepo = expectedReq(StackEnvironmentType.REPO, config.getEnvironmentInstanceNotificationTopicName(StackEnvironmentType.REPO));
		CreateTopicResult expectedResRepo = setupExpectedRes(StackEnvironmentType.REPO, expectedReqRepo);
		CreateTopicRequest expectedReqWorkers = expectedReq(StackEnvironmentType.WORKERS, config.getEnvironmentInstanceNotificationTopicName(StackEnvironmentType.WORKERS));
		CreateTopicResult expectedResWorkers = setupExpectedRes(StackEnvironmentType.WORKERS, expectedReqWorkers);
		
		// Make the call
		setup.setupResources();

		verify(mockClient).createTopic(expectedReqPortal);
		verify(mockClient).createTopic(expectedReqRepo);
		verify(mockClient).createTopic(expectedReqWorkers);
		
		// Make sure it was set the resources
		assertEquals("The expected topic was not set in the resoruces", expectedResPortal.getTopicArn(), resources.getEnvironmentInstanceNotificationTopicArn(StackEnvironmentType.PORTAL));
		assertEquals("The expected topic was not set in the resoruces", expectedResRepo.getTopicArn(), resources.getEnvironmentInstanceNotificationTopicArn(StackEnvironmentType.REPO));
		assertEquals("The expected topic was not set in the resoruces", expectedResWorkers.getTopicArn(), resources.getEnvironmentInstanceNotificationTopicArn(StackEnvironmentType.WORKERS));
	}
	
	private CreateTopicRequest expectedReq(StackEnvironmentType env, String topicName) {
		
		CreateTopicRequest expectedReq = new CreateTopicRequest();
		expectedReq.setName(topicName);
		return expectedReq;
	}
	
	private CreateTopicResult setupExpectedRes(StackEnvironmentType env, CreateTopicRequest req) {
		String topicArn = "arn:" + req.getName();
		String protocol = Constants.TOPIC_SUBSCRIBE_PROTOCOL_EMAIL;
		String endpoint = config.getEnvironmentInstanceNotificationEndpoint(env);

		CreateTopicResult expectedRes = new CreateTopicResult().withTopicArn(topicArn);
		when(mockClient.createTopic(req)).thenReturn(expectedRes);
		
		ListSubscriptionsByTopicRequest tRequest = new ListSubscriptionsByTopicRequest().withTopicArn(topicArn);
		Subscription expected = new Subscription().withEndpoint(endpoint).withProtocol(protocol);
		ListSubscriptionsByTopicResult result = new ListSubscriptionsByTopicResult().withSubscriptions(expected);
		when(mockClient.listSubscriptionsByTopic(tRequest)).thenReturn(result);
		
		return expectedRes;
	}
}
