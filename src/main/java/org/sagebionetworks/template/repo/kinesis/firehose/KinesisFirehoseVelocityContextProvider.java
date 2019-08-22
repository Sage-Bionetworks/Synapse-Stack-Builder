package org.sagebionetworks.template.repo.kinesis.firehose;

import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;

public class KinesisFirehoseVelocityContextProvider implements VelocityContextProvider {

	private KinesisFirehoseConfig firehoseConfig;

	@Inject
	public KinesisFirehoseVelocityContextProvider(KinesisFirehoseConfig firehoseConfig) {
		this.firehoseConfig = firehoseConfig;
	}

	@Override
	public void addToContext(VelocityContext context) {
		context.put(Constants.KINESIS_FIREHOSE_STREAM_DESCRIPTORS, firehoseConfig.getStreamDescriptors());
	}

}
