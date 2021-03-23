package org.sagebionetworks.template.s3;

import java.util.Objects;

public class S3BucketDescriptor {

	/**
	 * The name of the bucket
	 */
	private String name;

	/**
	 * True if the an inventory configuration should be enabled for the bucket using the inventory bucket as destination
	 */
	private boolean inventoryEnabled = false;

	/**
	 * If set will setup a retention life cycle rule for the specified number of days
	 */
	private Integer retentionDays;
	
	/**
	 * True if the bucket should be created only in dev
	 */
	private boolean devOnly = false;

	public S3BucketDescriptor() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isInventoryEnabled() {
		return inventoryEnabled;
	}

	public void setInventoryEnabled(boolean inventoryEnabled) {
		this.inventoryEnabled = inventoryEnabled;
	}

	public Integer getRetentionDays() {
		return retentionDays;
	}

	public void setRetentionDays(Integer retentionDays) {
		this.retentionDays = retentionDays;
	}
	
	public boolean isDevOnly() {
		return devOnly;
	}
	
	public void setDevOnly(boolean devOnly) {
		this.devOnly = devOnly;
	}

	@Override
	public int hashCode() {
		return Objects.hash(devOnly, inventoryEnabled, name, retentionDays);
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
		S3BucketDescriptor other = (S3BucketDescriptor) obj;
		return devOnly == other.devOnly && inventoryEnabled == other.inventoryEnabled && Objects.equals(name, other.name)
				&& Objects.equals(retentionDays, other.retentionDays);
	}

	@Override
	public String toString() {
		return "S3BucketDescriptor [name=" + name + ", inventoryEnabled=" + inventoryEnabled + ", retentionDays=" + retentionDays
				+ ", devOnly=" + devOnly + "]";
	}

}
