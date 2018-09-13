package org.sagebionetworks.template.repo.queues;

import java.util.List;

public class WorkerResourceDescriptor {
	List<WorkerSNSTopicDescriptor> workerSnsTopicDescriptors;
	List<WorkerQueueDescriptor> workerQueueDescriptors;


	//TODO: move this chunk of code into the JSON schema validation
//	public void validate(){
//		if (CollectionUtils.isEmpty(snsTopicSuffixes)){
//			throw new IllegalArgumentException("snsTopicSuffixes can not be null or empty"); //TODO: right exception type?
//		}
//		if(CollectionUtils.isEmpty(workerQueueDescriptors)){
//			throw new IllegalArgumentException("workerQueueDescriptors can not be null or empty"); //TODO: right exception type?
//		}
//
//		//make sure that
//		for(WorkerQueueDescriptor workerQueueDescriptor: workerQueueDescriptors){
//
//			workerQueueDescriptor.validate();
//
//			//check that all SNS topic suffixes used in the workerQueueDescriptors exist in the set snsTopicSuffixes
//			for(String snsTopicSuffix : workerQueueDescriptor.snsTopicSuffixesToSubscribe){
//				if(!snsTopicSuffix.contains(snsTopicSuffix)){
//					throw new IllegalArgumentException("snsTopicSuffix=" +snsTopicSuffix + " listed in "
//							+ workerQueueDescriptor.queueName + " does not exist in your previously defined snsTopicSuffixes " + snsTopicSuffix );
//				}
//			}
//		}
//	}


	///////////////////////
	// Getters and Setters
	///////////////////////

	public void setWorkerSnsTopicDescriptors(List<WorkerSNSTopicDescriptor> workerSnsTopicDescriptors) {
		this.workerSnsTopicDescriptors = workerSnsTopicDescriptors;
	}

	public void setWorkerQueueDescriptors(List<WorkerQueueDescriptor> workerQueueDescriptors) {
		this.workerQueueDescriptors = workerQueueDescriptors;
	}

}
