package org.sagebionetworks.template;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.AvailabilityZone;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.DescribeEventsRequest;
import software.amazon.awssdk.services.rds.model.DescribeOrderableDbInstanceOptionsRequest;
import software.amazon.awssdk.services.rds.model.Event;
import software.amazon.awssdk.services.rds.model.PendingModifiedValues;
import software.amazon.awssdk.services.rds.model.SourceType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RdsClientWrapperImpl implements RdsClientWrapper {

	/**
	 * How far back to look for the events that explain a failed Multi-AZ conversion, and how many of
	 * the most recent ones to report.
	 */
	static final int EVENT_LOOKBACK_MINUTES = 24 * 60;
	static final int MAX_REPORTED_EVENTS = 5;

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
			List<String> dbInstanceClasses, List<String> subnets, List<String> excludedZones, int minCount) {
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
		// RDS can list a class as orderable in a zone and still reject it there for a standby.
		if (commonZones.removeAll(excludedZones)) {
			logger.info(String.format("Excluded availability zones %s from the subnets for DB instance classes %s",
					excludedZones, dbInstanceClasses));
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
		 * RDS accepts a Multi-AZ conversion, then abandons it and returns the instance to 'available'
		 * when it cannot place the standby in the availability zone it picked, so a successful stack can
		 * still be left single-AZ (PLFM-9965). The only record of why is an RDS event such as "Instance
		 * type db.r8g.2xlarge in availability zone us-east-1b is unsupported", even when that zone is
		 * listed as orderable for the class.
		 */
		throw new IllegalStateException(String.format(
				"Database %s should have MultiAZ=%s but is MultiAZ=%s. RDS accepted the change and then abandoned it."
						+ " If an event below names an availability zone that does not support the instance class,"
						+ " add that zone to %s. Recent events: %s",
				dbInstanceIdentifier, expectedMultiAZ, actualMultiAZ,
				Constants.PROPERTY_KEY_RDS_EXCLUDED_AVAILABILITY_ZONES, getRecentEvents(dbInstanceIdentifier)));
	}

	/**
	 * The most recent RDS events of a DB instance, oldest first. Failing to read them must not hide
	 * the mismatch being reported, so an error is logged and no events are returned.
	 */
	List<String> getRecentEvents(String dbInstanceIdentifier) {
		DescribeEventsRequest req = DescribeEventsRequest.builder().sourceType(SourceType.DB_INSTANCE)
				.sourceIdentifier(dbInstanceIdentifier).duration(EVENT_LOOKBACK_MINUTES).build();
		try {
			List<Event> events = rds.describeEvents(req).events();
			return events.stream().skip(Math.max(0, events.size() - MAX_REPORTED_EVENTS))
					.map(event -> event.date() + " " + event.message()).collect(Collectors.toList());
		} catch (SdkException e) {
			logger.warn("Failed to read the RDS events of " + dbInstanceIdentifier, e);
			return List.of();
		}
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
