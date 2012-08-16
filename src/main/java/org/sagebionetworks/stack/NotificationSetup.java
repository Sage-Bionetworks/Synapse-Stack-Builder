package org.sagebionetworks.stack;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sns.model.Subscription;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 * Setup topics for notification.
 * 
 * @author John
 *
 */
public class NotificationSetup implements ResourceProcessor {
	
	private static Logger log = Logger.getLogger(NotificationSetup.class.getName());
	
	private AmazonSNSClient client;
	private InputConfiguration config;
	private GeneratedResources resources;
	
	/**
	 * The IoC constructor.
	 * 
	 * @param client
	 * @param config
	 */
	public NotificationSetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
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
	/**
	 * Create The Notification topic.
	 */
	public CreateTopicResult setupNotificationTopics(){
		// Create the RDS alert topic
		CreateTopicRequest request = new CreateTopicRequest();
		request.setName(config.getRDSAlertTopicName());
		CreateTopicResult result = client.createTopic(request);
		resources.setRdsAlertTopic(result);
		log.debug("Topic: "+result);
		// Create the RDS alert subscription
		Subscription sub = createSubScription(result.getTopicArn(), Constants.TOPIC_SUBSCRIBE_PROTOCOL_EMAIL, config.getRDSAlertSubscriptionEndpoint());
		return result;
	}
	
	/**
	 * Create the subscription if it does not exist.
	 */
	Subscription createSubScription(String topicArn, String protocol, String endpoint){
		// first determine if the subscription exists
		Subscription result = findSubscription(topicArn, protocol, endpoint);
		if(result == null){
			// Subscribe to this topic
			SubscribeRequest subscribeRequest = new SubscribeRequest();
			subscribeRequest.setTopicArn(topicArn);
			subscribeRequest.setProtocol(protocol);
			subscribeRequest.setEndpoint(endpoint);
			SubscribeResult subResults = client.subscribe(subscribeRequest);
			log.debug("Subscription did not exist so created it: "+subResults);
		}else{
			log.debug("Subscription already exists: "+result);
		}
		// Search again to find it.
		return findSubscription(topicArn, protocol, endpoint);
	}
	
	/**
	 * This is a pain-in-the-butt way to determine if a subscription already exists
	 * @param topicArn
	 * @param protocol
	 * @param endpoint
	 * @return
	 */
	Subscription findSubscription(String topicArn, String protocol, String endpoint){
		// Fill this list with all of the pages
		List<Subscription> fullList = new LinkedList<Subscription>();
		ListSubscriptionsByTopicResult subList = client.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest().withTopicArn(topicArn));
		fullList.addAll(subList.getSubscriptions());
		while(subList.getNextToken() != null){
			subList = client.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest().withTopicArn(topicArn));
			fullList.addAll(subList.getSubscriptions());
		}
		// Scan the full list for this results
		for(Subscription sub: fullList){
			if(protocol.equals(sub.getProtocol()) && endpoint.equals(sub.getEndpoint())){
				return sub;
			}
		}
		// Did not find it
		return null;
	}
	
	

}
