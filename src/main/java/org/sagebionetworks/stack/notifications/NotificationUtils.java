package org.sagebionetworks.stack.notifications;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sns.model.Subscription;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

public class NotificationUtils {
	private static Logger logger = Logger.getLogger(NotificationUtils.class.getName());
	
	/**
	 * Create the subscription if it does not exist.
	 */
	public static Subscription createSubScription(AmazonSNSClient client, String topicArn, String protocol, String endpoint){
		// first determine if the subscription exists
		Subscription result = findSubscription(client, topicArn, protocol, endpoint);
		if(result == null){
			// Subscribe to this topic
			SubscribeRequest subscribeRequest = new SubscribeRequest();
			subscribeRequest.setTopicArn(topicArn);
			subscribeRequest.setProtocol(protocol);
			subscribeRequest.setEndpoint(endpoint);
			SubscribeResult subResults = client.subscribe(subscribeRequest);
			logger.debug("Subscription did not exist so created it: "+subResults);
		}else{
			logger.debug("Subscription already exists: "+result);
		}
		// Search again to find it.
		return findSubscription(client, topicArn, protocol, endpoint);
	}
	
	/**
	 * This is a pain-in-the-butt way to determine if a subscription already exists
	 * @param topicArn
	 * @param protocol
	 * @param endpoint
	 * @return
	 */
	public static Subscription findSubscription(AmazonSNSClient client, String topicArn, String protocol, String endpoint){
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
