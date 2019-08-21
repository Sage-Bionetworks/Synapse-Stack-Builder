package org.sagebionetworks.template.repo.kinesis.firehose;

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
		if (stream.getBufferFlushInterval() < KinesisFirehoseStreamDescriptor.MIN_BUFFER_INTERVAL) {
			throw new IllegalStateException(
					"Stream " + stream.getName() + ": the minimum value for the bufferFlushInterval is "
							+ KinesisFirehoseStreamDescriptor.MIN_BUFFER_INTERVAL + "(was "
							+ stream.getBufferFlushInterval() + ")");
		}
		if (stream.getBufferFlushInterval() > KinesisFirehoseStreamDescriptor.MAX_BUFFER_INTERVAL) {
			throw new IllegalStateException(
					"Stream " + stream.getName() + ": the maximum value for the bufferFlushInterval is "
							+ KinesisFirehoseStreamDescriptor.MAX_BUFFER_INTERVAL + "(was "
							+ stream.getBufferFlushInterval() + ")");
		}
		if (stream.getBufferFlushSize() < KinesisFirehoseStreamDescriptor.MIN_BUFFER_SIZE) {
			throw new IllegalStateException("Stream " + stream.getName()
					+ ": The minimum value for the bufferFlushSize is "
					+ KinesisFirehoseStreamDescriptor.MIN_BUFFER_SIZE + "(was " + stream.getBufferFlushSize() + ")");
		}
		if (stream.getBufferFlushSize() > KinesisFirehoseStreamDescriptor.MAX_BUFFER_SIZE) {
			throw new IllegalStateException("Stream " + stream.getName()
					+ ": The maximum value for the bufferFlushSize is "
					+ KinesisFirehoseStreamDescriptor.MAX_BUFFER_SIZE + "(was " + stream.getBufferFlushSize() + ")");
		}
		if (stream.isConvertToParquet()) {
			if (stream.getTableDescriptor() == null) {
				throw new IllegalStateException("The stream " + stream.getName()
						+ " is configured to be converted to parquet records, but the tableDescriptor is missing");
			}
			validateTable(stream.getTableDescriptor());
		}
	}

	private KinesisFirehoseConfigValidator validateTable(GlueTableDescriptor table) {
		if (table.getColumns().isEmpty()) {
			throw new IllegalStateException("No column definition found for table " + table.getName());
		}
		return this;
	}

}
