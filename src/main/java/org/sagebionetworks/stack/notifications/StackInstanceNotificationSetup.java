package org.sagebionetworks.stack.notifications;

import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import org.sagebionetworks.stack.Constants;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.ResourceProcessor;

import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 * Setup topics for notification.
 * 
 * @author John
 *
 */
public class StackInstanceNotificationSetup implements ResourceProcessor {
	
	private static Logger logger = Logger.getLogger(StackInstanceNotificationSetup.class.getName());
	
	private AmazonSNSClient client;
	private InputConfiguration config;
	private GeneratedResources resources;
	
	/**
	 * The IoC constructor.
	 * 
	 * @param client
	 * @param config
	 */
	public StackInstanceNotificationSetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		initialize(factory, config, resources);
	}
	
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		if(factory == null) throw new IllegalArgumentException("AmazonClientFactory cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		this.client = factory.createSNSClient();
		this.config = config;
		this.resources = resources;
	}
	
	public void setupResources() {
		setupNotificationTopics();
	}
	
	public void teardownResources() {
		
	}
	
	public void describeResources() {
		String topicName;
		String subscriptionEndpoint;
		ListTopicsResult res;
		List<Topic> topics;
		
		topicName = config.getRDSAlertTopicName();
		subscriptionEndpoint = config.getRDSAlertSubscriptionEndpoint();
		res = client.listTopics();
		topics = res.getTopics(); // TopicArn ends with topic name
		for (Topic topic:topics) {
			if (topic.getTopicArn().endsWith(topicName)) {
				resources.setStackInstanceNotificationTopicArn(topic.getTopicArn());
				break;
			}
		}		
	}
	/**
	 * Create The Notification topic.
	 */
	public CreateTopicResult setupNotificationTopics(){
		// Create the RDS alert topic
		CreateTopicRequest request = new CreateTopicRequest();
		request.setName(config.getRDSAlertTopicName());
		CreateTopicResult result = client.createTopic(request);
		resources.setStackInstanceNotificationTopicArn(result.getTopicArn());
		logger.debug("Topic: "+result);
		// Create the RDS alert subscription
		Subscription sub = NotificationUtils.createSubScription(client, result.getTopicArn(), Constants.TOPIC_SUBSCRIBE_PROTOCOL_EMAIL, config.getRDSAlertSubscriptionEndpoint());
		return result;
	}
	
	

}
