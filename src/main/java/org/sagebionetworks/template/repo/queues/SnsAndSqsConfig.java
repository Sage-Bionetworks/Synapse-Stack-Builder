package org.sagebionetworks.template.repo.queues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class used to deserialize JSON config using the GSON package.
 * This class is immutable.
 */
public class SnsAndSqsConfig {
	final Set<String> snsTopicNames;
	final List<QueueConfig> queues;

	@JsonCreator
	public SnsAndSqsConfig(@JsonProperty(value = "snsTopicNames", required = true) Set<String> snsTopicNames,
						   @JsonProperty(value = "queues", required = true) List<QueueConfig> queues) {
		this.snsTopicNames = Collections.unmodifiableSet(new HashSet<>(snsTopicNames));
		this.queues = Collections.unmodifiableList(new ArrayList<>(queues));
	}


	public SnsTopicAndSqsQueueDescriptors convertToDesciptor(){
		Map<String,SnsTopicDescriptor> topicToQueue = new HashMap<>(snsTopicNames.size());
		List<SqsQueueDescriptor> sqsQueueDescriptors = new ArrayList<>(queues.size());

		for(QueueConfig queueConfig: queues){
			for(String subscribedTopicName : queueConfig.subscribedTopicNames){

				//check SNS topic type used in the QueueConfig exist in the set snsTopicTypes
				if(!snsTopicNames.contains(subscribedTopicName)){
					throw new IllegalArgumentException(subscribedTopicName + " listed in "
							+ queueConfig.queueName
							+ ".subscribedTopicNames does not exist in your previously defined snsTopicNames "
							+ snsTopicNames );
				}

				//add queue to SnsTopicDescriptors
				topicToQueue.computeIfAbsent(subscribedTopicName, SnsTopicDescriptor::new)
							.withSubscribedQueue(queueConfig.queueName);
			}

			//wrap the queueConfig in a SqsQueueDescriptor
			sqsQueueDescriptors.add(new SqsQueueDescriptor(queueConfig));
		}

		return new SnsTopicAndSqsQueueDescriptors(new ArrayList<>(topicToQueue.values()), sqsQueueDescriptors);
	}

	@Override
	public String toString() {
		return "SnsAndSqsConfig{" +
				"snsTopicNames=" + snsTopicNames +
				", queues=" + queues +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SnsAndSqsConfig that = (SnsAndSqsConfig) o;
		return Objects.equals(snsTopicNames, that.snsTopicNames) &&
				Objects.equals(queues, that.queues);
	}

	@Override
	public int hashCode() {
		return Objects.hash(snsTopicNames, queues);
	}
}
