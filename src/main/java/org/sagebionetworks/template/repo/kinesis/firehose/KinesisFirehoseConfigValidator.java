package org.sagebionetworks.template.repo.kinesis.firehose;

public class KinesisFirehoseConfigValidator {

	private KinesisFirehoseConfig config;

	public KinesisFirehoseConfigValidator(KinesisFirehoseConfig config) {
		this.config = config;
	}

	public KinesisFirehoseConfig validate() {
		config.getStreamDescriptors().forEach(stream -> {
			if (stream.isConvertToParquet()) {
				if (stream.getTableDescriptor() == null) {
					throw new IllegalStateException("The stream " + stream.getName()
							+ " is configured to be converted to parquet records, but the tableDescriptor is missing");
				}
				validateTable(stream.getTableDescriptor());
			}
		});
		return config;
	}

	private KinesisFirehoseConfigValidator validateTable(GlueTableDescriptor table) {
		if (table.getColumns().isEmpty()) {
			throw new IllegalStateException("No column definition found for table " + table.getName());
		}
		return this;
	}

}
