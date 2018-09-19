package org.sagebionetworks.template.repo.queues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
	private final Set<String> snsTopicNames;
	private final List<SqsQueueDescriptor> queueDescriptors;

	@JsonCreator
	public SnsAndSqsConfig(@JsonProperty(value = "snsTopicNames", required = true) List<String> snsTopicNames,
						   @JsonProperty(value = "queueDescriptors", required = true) List<SqsQueueDescriptor> queueDescriptors) {
		this.snsTopicNames = Collections.unmodifiableSet(new LinkedHashSet<>(snsTopicNames));
		this.queueDescriptors = Collections.unmodifiableList(new ArrayList<>(queueDescriptors));
	}


	public List<SnsTopicDescriptor> processSnsTopicDescriptors() {
		Map<String,SnsTopicDescriptor> topicNameToTopicDescriptor = new HashMap<>(snsTopicNames.size());

		for(SqsQueueDescriptor sqsQueueDescriptor : queueDescriptors){
			for(String subscribedTopicName : sqsQueueDescriptor.subscribedTopicNames){

				//check SNS topic type used in the SqsQueueDescriptor exist in the set snsTopicTypes
				if(!snsTopicNames.contains(subscribedTopicName)){
					throw new IllegalArgumentException(subscribedTopicName + " listed in "
							+ sqsQueueDescriptor.queueName
							+ ".subscribedTopicNames does not exist in your previously defined snsTopicNames "
							+ snsTopicNames );
				}

				//add queue name that is filtered alphanumerically to SnsTopicDescriptor.
				topicNameToTopicDescriptor.computeIfAbsent(subscribedTopicName, SnsTopicDescriptor::new)
							.addToSubscribedQueues(sqsQueueDescriptor.getQueueNameCloudformationResource());
			}
		}
		return new ArrayList<>(topicNameToTopicDescriptor.values());
	}

	public List<SqsQueueDescriptor> getQueueDescriptors() {
		return queueDescriptors;
	}

	@Override
	public String toString() {
		return "SnsAndSqsConfig{" +
				"snsTopicNames=" + snsTopicNames +
				", queueDescriptors=" + queueDescriptors +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SnsAndSqsConfig that = (SnsAndSqsConfig) o;
		return Objects.equals(snsTopicNames, that.snsTopicNames) &&
				Objects.equals(queueDescriptors, that.queueDescriptors);
	}

	@Override
	public int hashCode() {
		return Objects.hash(snsTopicNames, queueDescriptors);
	}
}
