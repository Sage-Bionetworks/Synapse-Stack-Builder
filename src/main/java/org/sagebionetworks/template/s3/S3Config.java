package org.sagebionetworks.template.s3;

import java.util.List;
import java.util.Objects;

public class S3Config {

	/**
	 * The list of bucket descriptors
	 */
	private List<S3BucketDescriptor> buckets;

	/**
	 * The inventory configuration
	 */
	private S3InventoryConfig inventoryConfig;

	private S3VirusScannerConfig virusScannerConfig;

	public S3Config() { }

	public List<S3BucketDescriptor> getBuckets() {
		return buckets;
	}

	public void setBuckets(List<S3BucketDescriptor> buckets) {
		this.buckets = buckets;
	}

	public S3InventoryConfig getInventoryConfig() {
		return inventoryConfig;
	}

	public void setInventoryConfig(S3InventoryConfig inventoryConfig) {
		this.inventoryConfig = inventoryConfig;
	}
	
	public S3VirusScannerConfig getVirusScannerConfig() {
		return virusScannerConfig;
	}
	
	public void setVirusScannerConfig(S3VirusScannerConfig virusScannerConfig) {
		this.virusScannerConfig = virusScannerConfig;
	}

	@Override
	public int hashCode() {
		return Objects.hash(buckets, inventoryConfig, virusScannerConfig);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof S3Config)) {
			return false;
		}
		S3Config other = (S3Config) obj;
		return Objects.equals(buckets, other.buckets) && Objects.equals(inventoryConfig, other.inventoryConfig)
				&& Objects.equals(virusScannerConfig, other.virusScannerConfig);
	}

	@Override
	public String toString() {
		return "S3Config [buckets=" + buckets + ", inventoryConfig=" + inventoryConfig + ", virusScannerConfig=" + virusScannerConfig + "]";
	}

}
