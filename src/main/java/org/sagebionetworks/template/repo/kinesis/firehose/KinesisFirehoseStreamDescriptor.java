package org.sagebionetworks.template.repo.kinesis.firehose;

import java.util.Objects;

public class KinesisFirehoseStreamDescriptor {

	private String name;
	private String partitionScheme = "!{timestamp:yyyy-MM-dd}";
	private boolean convertToParquet = false;
	private String tableName = null;

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

	public boolean isConvertToParquet() {
		return convertToParquet;
	}

	public void setConvertToParquet(boolean convertToParquet) {
		this.convertToParquet = convertToParquet;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(convertToParquet, name, partitionScheme, tableName);
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
		return convertToParquet == other.convertToParquet && Objects.equals(name, other.name)
				&& Objects.equals(partitionScheme, other.partitionScheme) && Objects.equals(tableName, other.tableName);
	}

}
