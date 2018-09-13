package org.sagebionetworks.template.repo.queues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections.CollectionUtils;

/**
 * Class used to deserialize JSON config using the GSON package
 */
public class SnsAndSqsConfig {
	public final Set<String> snsTopicNames;
	public final List<QueueConfig> queues;

	@JsonCreator
	public SnsAndSqsConfig(@JsonProperty(value = "snsTopicNames", required = true) Set<String> snsTopicNames,
						   @JsonProperty(value = "queues", required = true) List<QueueConfig> queues) {
		this.snsTopicNames = Collections.unmodifiableSet(snsTopicNames);
		this.queues = Collections.unmodifiableList(queues);
	}


	public SnsTopicAndQueueDescriptor convertToDesciptor(){
		Map<String,SnsTopicDescriptor> topicToQueue = new HashMap<>(snsTopicNames.size());
		List<QueueDescriptor> queueDescriptors = new ArrayList<>();

		for(QueueConfig queueConfig: queues){
			for(String snsTopicType : queueConfig.subscribedTopicNames){

				//check SNS topic type used in the QueueConfig exist in the set snsTopicTypes
				if(!snsTopicNames.contains(snsTopicType)){
					throw new IllegalArgumentException(snsTopicType + " listed in "
							+ queueConfig.queueName
							+ ".subscribedTopicType does not exist in your previously defined snsTopicSuffixes "
							+ snsTopicType );
				}

				//add queue to SnsTopicDescriptors
				topicToQueue.computeIfAbsent(snsTopicType, SnsTopicDescriptor::new)
							.addSubscribedQueue(queueConfig.queueName);
			}

			//wrap the queueConfig in a QueueDescriptor
			queueDescriptors.add(new QueueDescriptor(queueConfig));
		}

		return new SnsTopicAndQueueDescriptor(new ArrayList<>(topicToQueue.values()), queueDescriptors);
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
