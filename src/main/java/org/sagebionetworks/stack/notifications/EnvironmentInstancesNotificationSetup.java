package org.sagebionetworks.stack.notifications;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicResult;
import org.apache.log4j.Logger;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.ResourceProcessor;
import org.sagebionetworks.stack.StackEnvironmentType;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

public class EnvironmentInstancesNotificationSetup implements ResourceProcessor {
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
	public EnvironmentInstancesNotificationSetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		initialize(factory, config, resources);
	}
	
	@Override
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		if(factory == null) throw new IllegalArgumentException("AmazonClientFactory cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		this.client = factory.createSNSClient();
		this.config = config;
		this.resources = resources;
	}
	

	@Override
	public void setupResources() throws InterruptedException {
		// Portal environment
		String topicName = config.getEnvironmentInstanceNotificationTopicName(StackEnvironmentType.PORTAL);
		String endpoint = config.getEnvironmentInstanceNotificationEndpoint(StackEnvironmentType.PORTAL);
		CreateTopicResult result = NotificationUtils.setupNotificationTopicAndSubscription(client, topicName, endpoint);
		// TODO: CHANGE generated resource
		resources.setEnvironmentInstanceNotificationTopicArn(StackEnvironmentType.PORTAL, result.getTopicArn());
		
		// Plfm environments (repo/worker)
		topicName = config.getEnvironmentInstanceNotificationTopicName(StackEnvironmentType.REPO);
		endpoint = config.getEnvironmentInstanceNotificationEndpoint(StackEnvironmentType.REPO);
		result = NotificationUtils.setupNotificationTopicAndSubscription(client, topicName, endpoint);
		// TODO: CHANGE generated resource
		resources.setEnvironmentInstanceNotificationTopicArn(StackEnvironmentType.REPO, result.getTopicArn());
		
		topicName = config.getEnvironmentInstanceNotificationTopicName(StackEnvironmentType.WORKERS);
		endpoint = config.getEnvironmentInstanceNotificationEndpoint(StackEnvironmentType.WORKERS);
		result = NotificationUtils.setupNotificationTopicAndSubscription(client, topicName, endpoint);
		// TODO: CHANGE generated resource
		resources.setEnvironmentInstanceNotificationTopicArn(StackEnvironmentType.WORKERS, result.getTopicArn());
	}

	@Override
	public void teardownResources() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
