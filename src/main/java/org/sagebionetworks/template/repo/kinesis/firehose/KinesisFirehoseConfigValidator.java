package org.sagebionetworks.template.repo.kinesis.firehose;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.template.repo.glue.GlueTableDescriptor;

public class KinesisFirehoseConfigValidator {

	private KinesisFirehoseConfig config;

	public KinesisFirehoseConfigValidator(KinesisFirehoseConfig config) {
		this.config = config;
	}

	public KinesisFirehoseConfig validate() {
		config.getStreamDescriptors().forEach(this::validateStream);
		return config;
	}

	private void validateStream(KinesisFirehoseStreamDescriptor stream) {
		if (StringUtils.isBlank(stream.getName())) {
			throw new IllegalStateException("The stream name cannot be empty");
		}		
		if (stream.getBufferFlushInterval() < KinesisFirehoseStreamDescriptor.MIN_BUFFER_INTERVAL) {
			throw new IllegalStateException(
					"Stream " + stream.getName() + ": the minimum value for the bufferFlushInterval is "
							+ KinesisFirehoseStreamDescriptor.MIN_BUFFER_INTERVAL + " (was "
							+ stream.getBufferFlushInterval() + ")");
		}
		if (stream.getBufferFlushInterval() > KinesisFirehoseStreamDescriptor.MAX_BUFFER_INTERVAL) {
			throw new IllegalStateException(
					"Stream " + stream.getName() + ": the maximum value for the bufferFlushInterval is "
							+ KinesisFirehoseStreamDescriptor.MAX_BUFFER_INTERVAL + " (was "
							+ stream.getBufferFlushInterval() + ")");
		}
		if (stream.getBufferFlushSize() < KinesisFirehoseStreamDescriptor.MIN_BUFFER_SIZE) {
			throw new IllegalStateException("Stream " + stream.getName()
					+ ": the minimum value for the bufferFlushSize is "
					+ KinesisFirehoseStreamDescriptor.MIN_BUFFER_SIZE + " (was " + stream.getBufferFlushSize() + ")");
		}
		if (stream.getBufferFlushSize() > KinesisFirehoseStreamDescriptor.MAX_BUFFER_SIZE) {
			throw new IllegalStateException("Stream " + stream.getName()
					+ ": the maximum value for the bufferFlushSize is "
					+ KinesisFirehoseStreamDescriptor.MAX_BUFFER_SIZE + " (was " + stream.getBufferFlushSize() + ")");
		}
		if (KinesisFirehoseRecordFormat.PARQUET.equals(stream.getFormat())) {
			if (stream.getTableDescriptor() == null) {
				throw new IllegalStateException("The stream " + stream.getName()
						+ " is configured to be converted to parquet records, but the tableDescriptor is missing");
			}
			validateTable(stream.getName(), stream.getTableDescriptor());
		}
		if (StringUtils.isBlank(stream.getBucket())) {
			throw new IllegalStateException("Stream " + stream.getName() + ": the bucket cannot be empty");
		}
	}

	private KinesisFirehoseConfigValidator validateTable(String streamName, GlueTableDescriptor table) {
		if (table.getColumns().isEmpty()) {
			throw new IllegalStateException("Stream " + streamName + ": no column definition found for table " + table.getName());
		}
		return this;
	}

}
