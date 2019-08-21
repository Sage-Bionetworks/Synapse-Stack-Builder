package org.sagebionetworks.template.repo.kinesis.firehose;

import java.util.Objects;

public class KinesisFirehoseStreamDescriptor {

	public static final int MIN_BUFFER_INTERVAL = 60;
	public static final int MAX_BUFFER_INTERVAL = 900;
	public static final int MIN_BUFFER_SIZE = 64;
	public static final int MAX_BUFFER_SIZE = 128;

	private String name;
	private String partitionScheme = "!{timestamp:yyyy-MM-dd}";
	// Buffer flush interval in seconds
	private int bufferFlushInterval = MAX_BUFFER_INTERVAL;
	// Buffer flush max size in MB
	private int bufferFlushSize = MIN_BUFFER_SIZE;
	private boolean convertToParquet = false;
	private GlueTableDescriptor tableDescriptor = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPartitionScheme() {
		return partitionScheme;
	}

	public void setPartitionScheme(String partitionScheme) {
		this.partitionScheme = partitionScheme;
	}

	public int getBufferFlushInterval() {
		return bufferFlushInterval;
	}

	public void setBufferFlushInterval(int bufferFlushInterval) {
		this.bufferFlushInterval = bufferFlushInterval;
	}

	public int getBufferFlushSize() {
		return bufferFlushSize;
	}

	public void setBufferFlushSize(int bufferFlushSize) {
		this.bufferFlushSize = bufferFlushSize;
	}

	public boolean isConvertToParquet() {
		return convertToParquet;
	}

	public void setConvertToParquet(boolean convertToParquet) {
		this.convertToParquet = convertToParquet;
	}

	public GlueTableDescriptor getTableDescriptor() {
		return tableDescriptor;
	}

	public void setTableDescriptor(GlueTableDescriptor tableDescriptor) {
		this.tableDescriptor = tableDescriptor;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bufferFlushInterval, bufferFlushSize, convertToParquet, name, partitionScheme,
				tableDescriptor);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KinesisFirehoseStreamDescriptor other = (KinesisFirehoseStreamDescriptor) obj;
		return bufferFlushInterval == other.bufferFlushInterval && bufferFlushSize == other.bufferFlushSize
				&& convertToParquet == other.convertToParquet && Objects.equals(name, other.name)
				&& Objects.equals(partitionScheme, other.partitionScheme)
				&& Objects.equals(tableDescriptor, other.tableDescriptor);
	}

}
