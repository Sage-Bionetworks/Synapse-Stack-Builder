package org.sagebionetworks.template.repo.kinesis.firehose;

import org.sagebionetworks.template.repo.glue.GlueTableDescriptor;

import java.util.Objects;

public class KinesisFirehoseStreamDescriptor {

	public static final String DEFAULT_BUCKET = "${stack}.log.sagebase.org";
	public static final int MIN_BUFFER_INTERVAL = 60;
	public static final int MAX_BUFFER_INTERVAL = 900;
	public static final int MIN_BUFFER_SIZE = 64;
	public static final int MAX_BUFFER_SIZE = 128;

	private String name;
	// Optional destination bucket for the streamed data, by default is {stack}.log.sagebase.org
	private String bucket = DEFAULT_BUCKET;
	// True if the stream data destination should be parameterized by the stack instance
	private boolean parameterizeDestinationByStack = false;
	// True if the stream should be deployed only in the dev stack
	private boolean devOnly = false;
	// Partitioning prefix for the stream in S3 (See https://docs.aws.amazon.com/firehose/latest/dev/s3-prefixes.html)
	private String partitionScheme = "!{timestamp:yyyy-MM-dd}";
	// Buffer flush interval in seconds
	private int bufferFlushInterval = MAX_BUFFER_INTERVAL;
	// Buffer flush max size in MB
	private int bufferFlushSize = MIN_BUFFER_SIZE;
	// The record format in the destination
	private KinesisFirehoseRecordFormat format = KinesisFirehoseRecordFormat.JSON;
	// A glue table descriptor for athena, mandatory if the format is PARQUET (used for conversion)
	private GlueTableDescriptor tableDescriptor = null;
	// When the format is PARQUET by default we create a backup of the raw JSON data, setting this to true will disable the backup
	private boolean backupDisabled = false;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getBucket() {
		return bucket;
	}
	
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public boolean isParameterizeDestinationByStack() {
		return parameterizeDestinationByStack;
	}
	
	public void setParameterizeDestinationByStack(boolean parameterizeDestinationByStack) {
		this.parameterizeDestinationByStack = parameterizeDestinationByStack;
	}

	public boolean isDevOnly() {
		return devOnly;
	}

	public void setDevOnly(boolean devOnly) {
		this.devOnly = devOnly;
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

	public KinesisFirehoseRecordFormat getFormat() {
		return format;
	}

	public void setFormat(KinesisFirehoseRecordFormat format) {
		this.format = format;
	}

	public GlueTableDescriptor getTableDescriptor() {
		return tableDescriptor;
	}

	public void setTableDescriptor(GlueTableDescriptor tableDescriptor) {
		this.tableDescriptor = tableDescriptor;
	}
	
	public boolean isBackupDisabled() {
		return backupDisabled;
	}
	
	public void setBackupDisabled(boolean backupDisabled) {
		this.backupDisabled = backupDisabled;
	}

	@Override
	public int hashCode() {
		return Objects.hash(backupDisabled, bucket, bufferFlushInterval, bufferFlushSize, devOnly, format, name,
				parameterizeDestinationByStack, partitionScheme, tableDescriptor);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		KinesisFirehoseStreamDescriptor other = (KinesisFirehoseStreamDescriptor) obj;
		return backupDisabled == other.backupDisabled && Objects.equals(bucket, other.bucket)
				&& bufferFlushInterval == other.bufferFlushInterval && bufferFlushSize == other.bufferFlushSize && devOnly == other.devOnly
				&& format == other.format && Objects.equals(name, other.name)
				&& parameterizeDestinationByStack == other.parameterizeDestinationByStack
				&& Objects.equals(partitionScheme, other.partitionScheme) && Objects.equals(tableDescriptor, other.tableDescriptor);
	}	

}
