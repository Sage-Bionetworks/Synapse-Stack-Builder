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
 * Class used to deserialize JSON config using the GSON package. This class is immutable.
 */
public class SnsAndSqsConfig {

	private final Set<String> snsTopicNames;
	private final Set<String> snsGlobalTopicNames;
	private final List<SqsQueueDescriptor> queueDescriptors;

	@JsonCreator
	public SnsAndSqsConfig(@JsonProperty(value = "snsTopicNames", required = true) List<String> snsTopicNames,
			@JsonProperty(value = "snsGlobalTopicNames", required = true) List<String> snsGlobalTopicNames,
			@JsonProperty(value = "queueDescriptors", required = true) List<SqsQueueDescriptor> queueDescriptors) {
		SnsAndSqsNameValidator.validateNames(snsTopicNames);
		SnsAndSqsNameValidator.validateNames(snsGlobalTopicNames);

		this.snsTopicNames = Collections.unmodifiableSet(new LinkedHashSet<>(snsTopicNames));
		this.snsGlobalTopicNames = Collections.unmodifiableSet(new LinkedHashSet<>(snsGlobalTopicNames));
		this.queueDescriptors = Collections.unmodifiableList(new ArrayList<>(queueDescriptors));
	}

	public List<SnsTopicDescriptor> processSnsTopicDescriptors() {
		Map<String, SnsTopicDescriptor> topicNameToTopicDescriptor = new HashMap<>();

		for (SqsQueueDescriptor sqsQueueDescriptor : queueDescriptors) {
			for (String subscribedTopicName : sqsQueueDescriptor.subscribedTopicNames) {
				boolean global = false;
				if (snsGlobalTopicNames.contains(subscribedTopicName)) {
					global = true;
					// check SNS topic type used in the SqsQueueDescriptor exist in the set snsTopicTypes
				} else if (!snsTopicNames.contains(subscribedTopicName)) {
					throw new IllegalArgumentException(subscribedTopicName + " listed in " + sqsQueueDescriptor.queueName
							+ ".subscribedTopicNames does not exist in your previously defined snsTopicNames " + snsTopicNames);
				}

				// add queue to SnsTopicDescriptors
				SnsTopicDescriptor topicDescriptor = topicNameToTopicDescriptor
						.computeIfAbsent(subscribedTopicName, SnsTopicDescriptor::new)
						.setGlobal(global)
						.addToSubscribedQueues(sqsQueueDescriptor.queueName);
				
				sqsQueueDescriptor.addTopicDescriptor(topicDescriptor);
			}
		}
		
		return new ArrayList<>(topicNameToTopicDescriptor.values());
	}

	public List<SqsQueueDescriptor> getQueueDescriptors() {
		return queueDescriptors;
	}

	@Override
	public int hashCode() {
		return Objects.hash(queueDescriptors, snsGlobalTopicNames, snsTopicNames);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SnsAndSqsConfig other = (SnsAndSqsConfig) obj;
		return Objects.equals(queueDescriptors, other.queueDescriptors) && Objects.equals(snsGlobalTopicNames, other.snsGlobalTopicNames)
				&& Objects.equals(snsTopicNames, other.snsTopicNames);
	}

	@Override
	public String toString() {
		return "SnsAndSqsConfig [snsTopicNames=" + snsTopicNames + ", snsGlobalTopicNames=" + snsGlobalTopicNames + ", queueDescriptors="
				+ queueDescriptors + "]";
	}

}
