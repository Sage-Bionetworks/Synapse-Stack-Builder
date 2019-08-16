package org.sagebionetworks.template.repo.kinesis.firehose;

public class KinesisFirehoseStreamDescriptor {

	private String name;
	private String partitionScheme;
	private KinesisFirehoseCompressionFormat compressionFormat;

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

	public KinesisFirehoseCompressionFormat getCompressionFormat() {
		return compressionFormat;
	}

	public void setCompressionFormat(KinesisFirehoseCompressionFormat compressionFormat) {
		this.compressionFormat = compressionFormat;
	}

}
