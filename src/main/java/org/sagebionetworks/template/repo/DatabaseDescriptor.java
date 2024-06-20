package org.sagebionetworks.template.repo;

import java.util.Objects;

/**
 * Descriptor for creating an AWS RDS instance.
 *
 */
public class DatabaseDescriptor {

	private String resourceName;
	private int allocatedStorage;
	private int maxAllocatedStorage;
	private String instanceClass;
	private String instanceIdentifier;
	private String snapshotIdentifier;
	private String dbName;
	private boolean multiAZ;
	// default to standard (magnetic)
	private DatabaseStorageType dbStorageType = DatabaseStorageType.standard;
	private int dbIops = -1;
	private int backupRetentionPeriodDays = 0;
	private DeletionPolicy deletionPolicy = DeletionPolicy.Snapshot;
	private int dbThroughput = -1;

	/**
	 * @return the deletionPolicy
	 */
	public String getDeletionPolicy() {
		return deletionPolicy.name();
	}

	/**
	 * @param deletionPolicy the deletionPolicy to set
	 */
	public DatabaseDescriptor withDeletionPolicy(DeletionPolicy deletionPolicy) {
		this.deletionPolicy = deletionPolicy;
		return this;
	}

	/**
	 * @return the backupRetentionPeriodDays
	 */
	public int getBackupRetentionPeriodDays() {
		return backupRetentionPeriodDays;
	}

	/**
	 * @param backupRetentionPeriodDays the backupRetentionPeriodDays to set
	 */
	public DatabaseDescriptor withBackupRetentionPeriodDays(int backupRetentionPeriodDays) {
		this.backupRetentionPeriodDays = backupRetentionPeriodDays;
		return this;
	}

	/**
	 * The name of the database instances template resource.
	 * 
	 * @return
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * Allocated Storage in GB.
	 * 
	 * @return
	 */
	public int getAllocatedStorage() {
		return allocatedStorage;
	}

	/**
	 * Maximum allocated storage in GB
	 *
	 * @return
	 */
	public int getMaxAllocatedStorage() { return maxAllocatedStorage; }

	/**
	 * Calculate 10% of the allocated storage in bytes.
	 * 
	 * @return
	 */
	public double getTenPercentOfAllocatedStroageBytes() {
		return ((double) allocatedStorage) * 0.1 * Math.pow(2, 30);
	}

	/**
	 * The AWS RDS database instance type.
	 * 
	 * @return
	 */
	public String getInstanceClass() {
		return instanceClass;
	}

	public String getInstanceIdentifier() {
		return instanceIdentifier;
	}

	public String getSnapshotIdentifier() { return snapshotIdentifier; }

	/**
	 * The name of the DB.
	 * 
	 * @return
	 */
	public String getDbName() {
		return dbName;
	}

	/**
	 * Should multiple availability zones be use for this database.
	 * 
	 * @return
	 */
	public boolean isMultiAZ() {
		return multiAZ;
	}

	/**
	 * The name of the database instances template resource.
	 * 
	 * @param resourceName
	 * @return
	 */
	public DatabaseDescriptor withResourceName(String resourceName) {
		this.resourceName = resourceName;
		return this;
	}

	/**
	 * Allocated Storage in GB.
	 * 
	 * @param allocatedStorage
	 * @return
	 */
	public DatabaseDescriptor withAllocatedStorage(int allocatedStorage) {
		this.allocatedStorage = allocatedStorage;
		return this;
	}

	/**
	 * Max allocated storage in GB
	 *
	 * @param maxAllocatedStorage
	 * @return
	 */
	public DatabaseDescriptor withMaxAllocatedStorage(int maxAllocatedStorage) {
		this.maxAllocatedStorage = maxAllocatedStorage;
		return this;
	}

	/**
	 * The AWS RDS database instance type.
	 * 
	 * @param instanceClass
	 * @return
	 */
	public DatabaseDescriptor withInstanceClass(String instanceClass) {
		this.instanceClass = instanceClass;
		return this;
	}

	/**
	 * The instances identifier
	 * 
	 * @param instanceIdentifier
	 * @return
	 */
	public DatabaseDescriptor withInstanceIdentifier(String instanceIdentifier) {
		this.instanceIdentifier = instanceIdentifier;
		return this;
	}

	/**
	 * The name of the DB.
	 * 
	 * @param dbName
	 * @return
	 */
	public DatabaseDescriptor withDbName(String dbName) {
		this.dbName = dbName;
		return this;
	}

	/**
	 * Should multiple availability zones be use for this database.
	 * 
	 * @param multiAZ
	 * @return
	 */
	public DatabaseDescriptor withMultiAZ(boolean multiAZ) {
		this.multiAZ = multiAZ;
		return this;
	}

	public DatabaseDescriptor withSnapshotIdentifier(String snapshotIdentifier) {
		this.snapshotIdentifier = snapshotIdentifier;
		return this;
	}
	
	/**
	 * See: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_CreateDBInstance.html
	 * @return
	 */
	public String getDbStorageType() {
		return dbStorageType.name();
	}

	/**
	 * See: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_CreateDBInstance.html
	 * @param dbStorageType
	 * @return
	 */
	public DatabaseDescriptor withDbStorageType(String dbStorageType) {
		if(dbStorageType == null) {
			throw new IllegalArgumentException("DatabaseStorageType cannot be null");
		}
		this.dbStorageType = DatabaseStorageType.valueOf(dbStorageType);
		return this;
	}

	/**
	 * @return
	 */
	public int getDbIops() {
		return dbIops;
	}

	/**
	 * See: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_Storage.html#USER_PIOPS
	 * 
	 * A value of less than zero will be ignored.
	 * @param dbIops
	 * @return
	 */
	public DatabaseDescriptor withDbIops(int dbIops) {
		this.dbIops = dbIops;
		return this;
	}
	
	public int getDbThroughput() {
		return dbThroughput;
	}
	
	public DatabaseDescriptor withDbThroughput(int dbThroughput) {
		this.dbThroughput = dbThroughput;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(allocatedStorage, backupRetentionPeriodDays, dbIops, dbName, dbStorageType, dbThroughput, deletionPolicy,
				instanceClass, instanceIdentifier, maxAllocatedStorage, multiAZ, resourceName, snapshotIdentifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DatabaseDescriptor)) {
			return false;
		}
		DatabaseDescriptor other = (DatabaseDescriptor) obj;
		return allocatedStorage == other.allocatedStorage && backupRetentionPeriodDays == other.backupRetentionPeriodDays
				&& dbIops == other.dbIops && Objects.equals(dbName, other.dbName) && dbStorageType == other.dbStorageType
				&& dbThroughput == other.dbThroughput && deletionPolicy == other.deletionPolicy
				&& Objects.equals(instanceClass, other.instanceClass) && Objects.equals(instanceIdentifier, other.instanceIdentifier)
				&& maxAllocatedStorage == other.maxAllocatedStorage && multiAZ == other.multiAZ
				&& Objects.equals(resourceName, other.resourceName) && Objects.equals(snapshotIdentifier, other.snapshotIdentifier);
	}

	@Override
	public String toString() {
		return "DatabaseDescriptor [resourceName=" + resourceName + ", allocatedStorage=" + allocatedStorage + ", maxAllocatedStorage="
				+ maxAllocatedStorage + ", instanceClass=" + instanceClass + ", instanceIdentifier=" + instanceIdentifier
				+ ", snapshotIdentifier=" + snapshotIdentifier + ", dbName=" + dbName + ", multiAZ=" + multiAZ + ", dbStorageType="
				+ dbStorageType + ", dbIops=" + dbIops + ", backupRetentionPeriodDays=" + backupRetentionPeriodDays + ", deletionPolicy="
				+ deletionPolicy + ", dbThroughput=" + dbThroughput + "]";
	}

}
