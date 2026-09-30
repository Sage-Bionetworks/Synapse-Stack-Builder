package org.sagebionetworks.template;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.AvailabilityZone;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.DescribeOrderableDbInstanceOptionsRequest;
import software.amazon.awssdk.services.rds.model.OrderableDBInstanceOption;
import software.amazon.awssdk.services.rds.model.PendingModifiedValues;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RdsClientWrapperImpl implements RdsClientWrapper {

	RdsClient rds;
	Ec2ClientWrapper ec2ClientWrapper;
	Logger logger;

	@Inject
	public RdsClientWrapperImpl(RdsClient rdsClient, Ec2ClientWrapper ec2ClientWrapper, LoggerFactory loggerFactory) {
		this.rds = rdsClient;
		this.ec2ClientWrapper = ec2ClientWrapper;
		this.logger = loggerFactory.getLogger(RdsClientWrapperImpl.class);
	}

	@Override
	public List<String> getAvailabilityZonesForDBInstanceClass(String engine, String engineVersion,
			String dbInstanceClass) {
		DescribeOrderableDbInstanceOptionsRequest req = DescribeOrderableDbInstanceOptionsRequest.builder()
				.engine(engine).engineVersion(engineVersion).dbInstanceClass(dbInstanceClass).build();
		return rds.describeOrderableDBInstanceOptionsPaginator(req).stream()
				.flatMap(res -> res.orderableDBInstanceOptions().stream())
				.flatMap(option -> option.availabilityZones().stream())
				.map(AvailabilityZone::name)
				.distinct().collect(Collectors.toList());
	}

	@Override
	public List<String> getAvailableSubnetsForDBInstanceClasses(String engine, String engineVersion,
			List<String> dbInstanceClasses, List<String> subnets, int minCount) {
		if (dbInstanceClasses == null || dbInstanceClasses.isEmpty()) {
			throw new IllegalArgumentException("At least one DB instance class is required");
		}
		Map<String, String> zoneToSubnetMap = ec2ClientWrapper.getAvailabityZoneToSubnetMap(subnets);
		// Availability zones that offer every requested DB instance class.
		Set<String> commonZones = null;
		for (String dbInstanceClass : dbInstanceClasses) {
			List<String> zones = getAvailabilityZonesForDBInstanceClass(engine, engineVersion, dbInstanceClass);
			if (commonZones == null) {
				commonZones = new HashSet<>(zones);
			} else {
				commonZones.retainAll(zones);
			}
		}
		List<String> availableSubnets = commonZones.stream().map(zoneToSubnetMap::get).filter(Objects::nonNull).sorted()
				.collect(Collectors.toList());
		if (availableSubnets.size() < minCount) {
			throw new IllegalArgumentException(String.format(
					"Could not find %d available subnets for DB instance classes %s in %s", minCount, dbInstanceClasses,
					subnets));
		}
		return availableSubnets;
	}

	@Override
	public void validateMultiAZ(String dbInstanceIdentifier, boolean expectedMultiAZ) {
		DBInstance instance = describeInstance(dbInstanceIdentifier);
		boolean actualMultiAZ = Boolean.TRUE.equals(instance.multiAZ());
		if (actualMultiAZ == expectedMultiAZ) {
			return;
		}
		// A conversion can still be in flight when the stack reaches a stable state.
		PendingModifiedValues pending = instance.pendingModifiedValues();
		if (pending != null && Objects.equals(pending.multiAZ(), expectedMultiAZ)) {
			logger.warn(String.format("Multi-AZ change for %s is still pending, expected %s but found %s",
					dbInstanceIdentifier, expectedMultiAZ, actualMultiAZ));
			return;
		}
		/*
		 * RDS accepts a Multi-AZ conversion, then abandons it without any failure event when the DB
		 * instance class is not offered in the availability zone it picks for the standby, so a
		 * successful stack can still be left single-AZ (PLFM-9965).
		 */
		throw new IllegalStateException(String.format(
				"Database %s should have MultiAZ=%s but is MultiAZ=%s. RDS accepted the change and then abandoned it,"
						+ " most likely because the instance class is not offered in the standby's availability zone."
						+ " Check 'aws rds describe-events --source-identifier %s --source-type db-instance'.",
				dbInstanceIdentifier, expectedMultiAZ, actualMultiAZ, dbInstanceIdentifier));
	}

	DBInstance describeInstance(String dbInstanceIdentifier) {
		DescribeDbInstancesRequest req = DescribeDbInstancesRequest.builder()
				.dbInstanceIdentifier(dbInstanceIdentifier).build();
		try {
			DescribeDbInstancesResponse res = rds.describeDBInstances(req);
			return res.dbInstances().stream().findFirst().orElseThrow(() -> new IllegalStateException(
					"Database does not exist: " + dbInstanceIdentifier));
		} catch (DbInstanceNotFoundException e) {
			throw new IllegalStateException("Database does not exist: " + dbInstanceIdentifier, e);
		}
	}

}
