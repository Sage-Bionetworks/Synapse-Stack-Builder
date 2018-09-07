package org.sagebionetworks.template.repo.workers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.repo.WebACLBuilder;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopy;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;

public class WorkerQueueBuilderImpl implements WorkerQueueBuilder{
	public static final String TOPIC_NAME_TEMPLATE_PREFIX = "%1$s-%2$s-repo-"; //used to generate SNS topic names

	CloudFormationClient cloudFormationClient;
	VelocityEngine velocityEngine;
	Configuration config;
	Logger logger;

	@Inject
	public WorkerQueueBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine, Configuration config, LoggerFactory loggerFactory) {
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.config = config;
		this.logger = loggerFactory.getLogger(WorkerQueueBuilderImpl.class);
	}

	@Override
	public void buildAndDeploy() {

	}

	public String generateCloudFormationTemplate(List<WorkerQueueDescriptor> workerQueueDescriptors){
		Set<String> topicNames = new HashSet<>();

		for (WorkerQueueDescriptor workerQueueDescriptor : workerQueueDescriptors){
			topicNames.addAll(workerQueueDescriptor.topicNamesToSubscribe);
		}


	}


}
