package org.sagebionetworks.template.s3;

import java.util.List;
import java.util.Objects;

public class S3Config {

	/**
	 * The list of bucket descriptors
	 */
	private List<S3BucketDescriptor> buckets;

	/**
	 * The name of the bucket used as inventory if any
	 */
	private String inventoryBucket;

	public S3Config() { }

	public List<S3BucketDescriptor> getBuckets() {
		return buckets;
	}

	public void setBuckets(List<S3BucketDescriptor> buckets) {
		this.buckets = buckets;
	}

	public String getInventoryBucket() {
		return inventoryBucket;
	}

	public void setInventoryBucket(String inventoryBucket) {
		this.inventoryBucket = inventoryBucket;
	}

	@Override
	public int hashCode() {
		return Objects.hash(buckets, inventoryBucket);
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
		S3Config other = (S3Config) obj;
		return Objects.equals(buckets, other.buckets) && Objects.equals(inventoryBucket, other.inventoryBucket);
	}

	@Override
	public String toString() {
		return "S3Config [buckets=" + buckets + ", inventoryBucket=" + inventoryBucket + "]";
	}

}
