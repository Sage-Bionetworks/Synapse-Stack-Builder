package org.sagebionetworks.template.repo;

import static org.sagebionetworks.template.Constants.KINESIS_FIREHOSE_STREAM_NAMES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_KINESIS_FIREHOSE_LOGGER_STREAM_NAMES;

import com.google.inject.Inject;
import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.config.Configuration;

public class KinesisFirehoseLoggingVelocityContextProvider implements VelocityContextProvider{

	Configuration config;

	@Inject
	public KinesisFirehoseLoggingVelocityContextProvider(Configuration config) {
		this.config = config;
	}

	@Override
	public void addToContext(VelocityContext context) {
		String[] KinesisStreamNames = config.getComaSeparatedProperty(PROPERTY_KEY_KINESIS_FIREHOSE_LOGGER_STREAM_NAMES);
		context.put(KINESIS_FIREHOSE_STREAM_NAMES, KinesisStreamNames);
	}
}
