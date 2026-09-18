package org.sagebionetworks.template;

import java.util.List;

public interface RdsClientWrapper {

	/**
	 * The availability zones where the given DB instance class can be created for the given engine.
	 * Unlike EC2 instance types, DB instance classes are not offered in every zone of a region.
	 */
	public List<String> getAvailabilityZonesForDBInstanceClass(String engine, String engineVersion, String dbInstanceClass);

	/**
	 * Filter the given subnets down to those in an availability zone that offers every one of the given
	 * DB instance classes. Throws an IllegalArgumentException when fewer than minCount subnets remain,
	 * since a DB subnet group that includes a zone the instance class is not offered in lets RDS pick
	 * that zone for the standby and silently abandon a Multi-AZ conversion.
	 */
	public List<String> getAvailableSubnetsForDBInstanceClasses(String engine, String engineVersion,
			List<String> dbInstanceClasses, List<String> subnets, int minCount);

	/**
	 * Verify the deployed Multi-AZ state of a DB instance matches what was requested, throwing an
	 * IllegalStateException when it does not. A conversion that is still in flight is accepted.
	 */
	public void validateMultiAZ(String dbInstanceIdentifier, boolean expectedMultiAZ);

}
