package org.sagebionetworks.template.repo;

/**
 * Descriptor for creating an AWS RDS instance.
 *
 */
public class DatabaseDescriptor {

	String resourceName;
	int allocatedStorage;
	String instanceClass;
	String instanceIdentifier;
	String dbName;
	boolean multiAZ;

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
	 * @return
	 */
	public int getAllocatedStorage() {
		return allocatedStorage;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + allocatedStorage;
		result = prime * result + ((dbName == null) ? 0 : dbName.hashCode());
		result = prime * result + ((instanceClass == null) ? 0 : instanceClass.hashCode());
		result = prime * result + ((instanceIdentifier == null) ? 0 : instanceIdentifier.hashCode());
		result = prime * result + (multiAZ ? 1231 : 1237);
		result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DatabaseDescriptor other = (DatabaseDescriptor) obj;
		if (allocatedStorage != other.allocatedStorage)
			return false;
		if (dbName == null) {
			if (other.dbName != null)
				return false;
		} else if (!dbName.equals(other.dbName))
			return false;
		if (instanceClass == null) {
			if (other.instanceClass != null)
				return false;
		} else if (!instanceClass.equals(other.instanceClass))
			return false;
		if (instanceIdentifier == null) {
			if (other.instanceIdentifier != null)
				return false;
		} else if (!instanceIdentifier.equals(other.instanceIdentifier))
			return false;
		if (multiAZ != other.multiAZ)
			return false;
		if (resourceName == null) {
			if (other.resourceName != null)
				return false;
		} else if (!resourceName.equals(other.resourceName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DatabaseDescriptor [resourceName=" + resourceName + ", allocatedStorage=" + allocatedStorage
				+ ", instanceClass=" + instanceClass + ", instanceIdentifier=" + instanceIdentifier + ", dbName="
				+ dbName + ", multiAZ=" + multiAZ + "]";
	}

}
