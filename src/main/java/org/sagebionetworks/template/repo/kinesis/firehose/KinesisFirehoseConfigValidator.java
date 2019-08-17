package org.sagebionetworks.template.repo.kinesis.firehose;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

public class KinesisFirehoseConfigValidator {
	
	private KinesisFirehoseConfig config;
	
	public KinesisFirehoseConfigValidator(KinesisFirehoseConfig config) {
		this.config = config;
	}

	public KinesisFirehoseConfig validate() {
		validateTables();
		validateStreams();
		return config;
	}

	private KinesisFirehoseConfigValidator validateTables() {
		config.getGlueTableDescriptors().forEach(this::validateTable);
		return this;
	}

	private KinesisFirehoseConfigValidator validateTable(GlueTableDescriptor table) {
		if (table.getColumns().isEmpty()) {
			throw new IllegalStateException("No column definition found for table " + table.getName());
		}
		return this;
	}

	private void validateStreams() {
		
		Set<String> tableNames = config.getGlueTableDescriptors().stream().map(GlueTableDescriptor::getName).collect(Collectors.toSet());
		
		config.getStreamDescriptors().forEach(stream -> {
			if (stream.isConvertToParquet()) {
				if (StringUtils.isEmpty(stream.getTableName())) {
					throw new IllegalStateException("The stream " + stream.getName()
							+ " is configured to be converted to parquet records, but no table name was defined");
				}
				if (!tableNames.contains(stream.getTableName())) {
					throw new IllegalStateException("The stream " + stream.getName()
							+ " is configured to be converted to parquet records, but the referenced table descriptor "
							+ stream.getTableName() + " is not defined");
				}
			}
		});
	}

}
