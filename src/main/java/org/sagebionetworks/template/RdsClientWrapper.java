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
	 * DB instance classes and is not one of the excluded zones. Throws an IllegalArgumentException when
	 * fewer than minCount subnets remain, since a DB subnet group that includes a zone that cannot host
	 * the instance class lets RDS pick that zone for the standby and abandon a Multi-AZ conversion.
	 * <p>
	 * The zones are excluded by name because RDS can report a class as orderable in a zone and then
	 * reject it there as unsupported when placing the standby (PLFM-9965).
	 */
	public List<String> getAvailableSubnetsForDBInstanceClasses(String engine, String engineVersion,
			List<String> dbInstanceClasses, List<String> subnets, List<String> excludedZones, int minCount);

	/**
	 * Verify the deployed Multi-AZ state of a DB instance matches what was requested, throwing an
	 * IllegalStateException that includes the instance's recent RDS events when it does not. A
	 * conversion that is still in flight is accepted.
	 */
	public void validateMultiAZ(String dbInstanceIdentifier, boolean expectedMultiAZ);

}
