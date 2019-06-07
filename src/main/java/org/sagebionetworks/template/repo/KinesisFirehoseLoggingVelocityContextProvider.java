package org.sagebionetworks.template.repo;

import static org.sagebionetworks.template.Constants.KINESIS_FIREHOSE_STREAM_NAMES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_KINESIS_FIREHOSE_LOGGER_STREAM_NAMES;

import com.google.inject.Inject;
import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.config.RepoConfiguration;

public class KinesisFirehoseLoggingVelocityContextProvider implements VelocityContextProvider{

	RepoConfiguration config;

	@Inject
	public KinesisFirehoseLoggingVelocityContextProvider(RepoConfiguration config) {
		this.config = config;
	}

	@Override
	public void addToContext(VelocityContext context) {
		String[] KinesisStreamNames = config.getComaSeparatedProperty(PROPERTY_KEY_KINESIS_FIREHOSE_LOGGER_STREAM_NAMES);
		context.put(KINESIS_FIREHOSE_STREAM_NAMES, KinesisStreamNames);
	}
}
