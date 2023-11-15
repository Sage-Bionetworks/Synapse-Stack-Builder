package org.sagebionetworks.template.s3;

import java.util.Objects;

public class S3InventoryConfig {
	
	private String bucket;
	private String prefix;

	public S3InventoryConfig() { }
	
	public String getBucket() {
		return bucket;
	}
	
	public S3InventoryConfig setBucket(String bucket) {
		this.bucket = bucket;
		return this;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public S3InventoryConfig setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bucket, prefix);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof S3InventoryConfig)) {
			return false;
		}
		S3InventoryConfig other = (S3InventoryConfig) obj;
		return Objects.equals(bucket, other.bucket) && Objects.equals(prefix, other.prefix);
	}

}
