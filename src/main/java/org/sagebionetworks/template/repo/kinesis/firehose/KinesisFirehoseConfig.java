package org.sagebionetworks.template.repo.kinesis.firehose;

import java.util.HashSet;
import java.util.Set;

public class KinesisFirehoseConfig {

	private Set<KinesisFirehoseStreamDescriptor> streamDescriptors = new HashSet<>();

	public Set<KinesisFirehoseStreamDescriptor> getStreamDescriptors() {
		return streamDescriptors;
	}

	public void setStreamDescriptors(Set<KinesisFirehoseStreamDescriptor> streamDescriptors) {
		this.streamDescriptors = streamDescriptors;
	}

}
