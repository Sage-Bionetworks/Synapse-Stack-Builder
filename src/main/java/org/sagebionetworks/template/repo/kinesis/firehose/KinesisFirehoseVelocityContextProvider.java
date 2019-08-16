package org.sagebionetworks.template.repo.kinesis.firehose;

import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;

public class KinesisFirehoseVelocityContextProvider implements VelocityContextProvider {
	
	private KinesisFirehoseConfig config;
	
	@Inject
	public KinesisFirehoseVelocityContextProvider(KinesisFirehoseConfig config) {
		this.config = config;
	}

	@Override
	public void addToContext(VelocityContext context) {
		context.put(Constants.KINESIS_FIREHOSE_STREAM_DESCRIPTORS, config.getStreamDescriptors());
	}

}
