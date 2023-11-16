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
	
	/**
	 * True if the uploads to the bucket should be 
	 */
	private boolean virusScanEnabled = false;
	
	private List<S3BucketAclGrant> additionalAclGrants;

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
	
	public boolean isVirusScanEnabled() {
		return virusScanEnabled;
	}
	
	public void setVirusScanEnabled(boolean virusScanEnabled) {
		this.virusScanEnabled = virusScanEnabled;
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
	
	public List<S3BucketAclGrant> getAdditionalAclGrants() {
		return additionalAclGrants;
	}
	
	public void setAdditionalAclGrants(List<S3BucketAclGrant> additionalAclGrants) {
		this.additionalAclGrants = additionalAclGrants;
	}

	@Override
	public int hashCode() {
		return Objects.hash(additionalAclGrants, devOnly, intArchiveConfiguration, inventoryEnabled, name, notificationsConfiguration,
				retentionDays, storageClassTransitions, virusScanEnabled);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof S3BucketDescriptor)) {
			return false;
		}
		S3BucketDescriptor other = (S3BucketDescriptor) obj;
		return Objects.equals(additionalAclGrants, other.additionalAclGrants) && devOnly == other.devOnly
				&& Objects.equals(intArchiveConfiguration, other.intArchiveConfiguration) && inventoryEnabled == other.inventoryEnabled
				&& Objects.equals(name, other.name) && Objects.equals(notificationsConfiguration, other.notificationsConfiguration)
				&& Objects.equals(retentionDays, other.retentionDays)
				&& Objects.equals(storageClassTransitions, other.storageClassTransitions) && virusScanEnabled == other.virusScanEnabled;
	}

	@Override
	public String toString() {
		return "S3BucketDescriptor [name=" + name + ", inventoryEnabled=" + inventoryEnabled + ", retentionDays=" + retentionDays
				+ ", devOnly=" + devOnly + ", storageClassTransitions=" + storageClassTransitions + ", intArchiveConfiguration="
				+ intArchiveConfiguration + ", notificationsConfiguration=" + notificationsConfiguration + ", virusScanEnabled="
				+ virusScanEnabled + "]";
	}

}
