package org.sagebionetworks.template.repo.workers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

/**
 * Class used to deserialize JSON config using the GSON package
 */
public class WorkerResourceJsonConfig {

	Set<String> repositoryChangeTopicTypes;
	List<WorkerQueueJsonConfig> queues;



	public WorkerResourceDescriptor convertToDesciptor(){
		if(CollectionUtils.isEmpty(repositoryChangeTopicTypes)){
			throw new IllegalArgumentException("repositoryChangeTopicTypes can not be null or empty");
		}
		if(CollectionUtils.isEmpty(queues)){
			throw new IllegalArgumentException("workerQueueDescriptors can not be null or empty");
		}

		Map<String,WorkerSNSTopicDescriptor> topicToQueue = new HashMap<>(repositoryChangeTopicTypes.size());
		List<WorkerQueueDescriptor> workerQueueDescriptors = new WorkerQueueDescriptor();
		for(WorkerQueueJsonConfig queueConfig: queues){
			queueConfig.validateNotNull();
			for(String snsTopicType : queueConfig.subscribedTopicTypes){

				//check SNS topic type used in the WorkerQueueJsonConfig exist in the set snsTopicTypes
				if(!repositoryChangeTopicTypes.contains(snsTopicType)){
					throw new IllegalArgumentException(snsTopicType + " listed in "
							+ queueConfig.queueWorkerName + ".subscribedTopicType does not exist in your previously defined snsTopicSuffixes " + snsTopicType );
				}

				topicToQueue.computeIfAbsent(snsTopicType, WorkerSNSTopicDescriptor::new).addSubscribedQueue(queueConfig.queueWorkerName);
			}


		}


	}

}
