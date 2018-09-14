package org.sagebionetworks.template.repo.queues;

import java.util.List;
import java.util.Objects;

public class SqsQueueDescriptor {

	public QueueConfig getConfig() {
		return config;
	}

	public QueueConfig config;

	public SqsQueueDescriptor(QueueConfig config){
		this.config = config;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SqsQueueDescriptor that = (SqsQueueDescriptor) o;
		return Objects.equals(config, that.config);
	}

	@Override
	public int hashCode() {

		return Objects.hash(config);
	}
}
