package org.sagebionetworks.template.s3;

import java.util.List;
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
	
	/**
	 * Optional list of storage class transition rules
	 */
	private List<S3BucketClassTransition> storageClassTransitions;
	
	/**
	 * Optional intelligent tiering archive configuration
	 */
	private S3IntArchiveConfiguration intArchiveConfiguration;
	
	/**
	 * Optional configuration bit to setup notifications to an sns topic
	 */
	private S3NotificationsConfiguration notificationsConfiguration;

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
	
	public List<S3BucketClassTransition> getStorageClassTransitions() {
		return storageClassTransitions;
	}
	
	public void setStorageClassTransitions(List<S3BucketClassTransition> storageClassTransitions) {
		this.storageClassTransitions = storageClassTransitions;
	}
	
	public S3IntArchiveConfiguration getIntArchiveConfiguration() {
		return intArchiveConfiguration;
	}
	
	public void setIntArchiveConfiguration(S3IntArchiveConfiguration intArchiveConfiguration) {
		this.intArchiveConfiguration = intArchiveConfiguration;
	}
	
	public S3NotificationsConfiguration getNotificationsConfiguration() {
		return notificationsConfiguration;
	}
	
	public void setNotificationsConfiguration(S3NotificationsConfiguration notificationsConfiguration) {
		this.notificationsConfiguration = notificationsConfiguration;
	}

	@Override
	public int hashCode() {
		return Objects.hash(devOnly, intArchiveConfiguration, inventoryEnabled, name, notificationsConfiguration, retentionDays,
				storageClassTransitions);
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
		return devOnly == other.devOnly && Objects.equals(intArchiveConfiguration, other.intArchiveConfiguration)
				&& inventoryEnabled == other.inventoryEnabled && Objects.equals(name, other.name)
				&& Objects.equals(notificationsConfiguration, other.notificationsConfiguration)
				&& Objects.equals(retentionDays, other.retentionDays)
				&& Objects.equals(storageClassTransitions, other.storageClassTransitions);
	}

	@Override
	public String toString() {
		return "S3BucketDescriptor [name=" + name + ", inventoryEnabled=" + inventoryEnabled + ", retentionDays=" + retentionDays
				+ ", devOnly=" + devOnly + ", storageClassTransitions=" + storageClassTransitions + ", intArchiveConfiguration="
				+ intArchiveConfiguration + ", notificationsConfiguration=" + notificationsConfiguration + "]";
	}

}
