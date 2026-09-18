package org.sagebionetworks.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.AvailabilityZone;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.DescribeOrderableDbInstanceOptionsRequest;
import software.amazon.awssdk.services.rds.model.DescribeOrderableDbInstanceOptionsResponse;
import software.amazon.awssdk.services.rds.model.OrderableDBInstanceOption;
import software.amazon.awssdk.services.rds.model.PendingModifiedValues;
import software.amazon.awssdk.services.rds.paginators.DescribeOrderableDBInstanceOptionsIterable;

@ExtendWith(MockitoExtension.class)
public class RdsClientWrapperImplTest {

	private static final String ENGINE = "mysql";
	private static final String ENGINE_VERSION = "8.4.8";
	private static final String REPO_CLASS = "db.r8g.2xlarge";
	private static final String TABLE_CLASS = "db.m6g.large";
	private static final String INSTANCE_ID = "prod-606-db";

	@Mock
	RdsClient mockRds;
	@Mock
	Ec2ClientWrapper mockEc2ClientWrapper;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;
	@Captor
	ArgumentCaptor<DescribeOrderableDbInstanceOptionsRequest> optionsRequestCaptor;
	@Captor
	ArgumentCaptor<DescribeDbInstancesRequest> instancesRequestCaptor;

	RdsClientWrapper wrapper;

	@BeforeEach
	public void before() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		wrapper = new RdsClientWrapperImpl(mockRds, mockEc2ClientWrapper, mockLoggerFactory);
	}

	@Test
	public void testGetAvailabilityZonesForDBInstanceClass() {
		DescribeOrderableDBInstanceOptionsIterable options = optionsIterable("us-east-1a", "us-east-1c", "us-east-1d");
		when(mockRds.describeOrderableDBInstanceOptionsPaginator(optionsRequestCaptor.capture())).thenReturn(options);

		// call under test
		List<String> zones = wrapper.getAvailabilityZonesForDBInstanceClass(ENGINE, ENGINE_VERSION, REPO_CLASS);

		assertEquals(Arrays.asList("us-east-1a", "us-east-1c", "us-east-1d"), zones);
		DescribeOrderableDbInstanceOptionsRequest request = optionsRequestCaptor.getValue();
		assertEquals(ENGINE, request.engine());
		assertEquals(ENGINE_VERSION, request.engineVersion());
		assertEquals(REPO_CLASS, request.dbInstanceClass());
	}

	/**
	 * The same zone appears on the option of every page it is offered in, so it must be de-duplicated.
	 */
	@Test
	public void testGetAvailabilityZonesForDBInstanceClassWithDuplicatesAcrossPages() {
		DescribeOrderableDBInstanceOptionsIterable options = iterableOf(
				List.of(pageOf("us-east-1a", "us-east-1c"), pageOf("us-east-1c", "us-east-1d")));
		when(mockRds.describeOrderableDBInstanceOptionsPaginator(any(DescribeOrderableDbInstanceOptionsRequest.class)))
				.thenReturn(options);

		// call under test
		List<String> zones = wrapper.getAvailabilityZonesForDBInstanceClass(ENGINE, ENGINE_VERSION, REPO_CLASS);

		assertEquals(Arrays.asList("us-east-1a", "us-east-1c", "us-east-1d"), zones);
	}

	@Test
	public void testGetAvailabilityZonesForDBInstanceClassNotOffered() {
		DescribeOrderableDBInstanceOptionsIterable options = optionsIterable();
		when(mockRds.describeOrderableDBInstanceOptionsPaginator(any(DescribeOrderableDbInstanceOptionsRequest.class)))
				.thenReturn(options);

		// call under test
		List<String> zones = wrapper.getAvailabilityZonesForDBInstanceClass(ENGINE, ENGINE_VERSION, REPO_CLASS);

		assertEquals(List.of(), zones);
	}

	@Test
	public void testGetAvailableSubnetsForDBInstanceClasses() {
		when(mockEc2ClientWrapper.getAvailabityZoneToSubnetMap(any())).thenReturn(zoneToSubnetMap());
		// The repo class is not offered in us-east-1b, so subnet-b must be dropped.
		DescribeOrderableDBInstanceOptionsIterable repoOptions = optionsIterable("us-east-1a", "us-east-1c",
				"us-east-1d");
		when(mockRds.describeOrderableDBInstanceOptionsPaginator(argThat(requestForClass(REPO_CLASS))))
				.thenReturn(repoOptions);

		// call under test
		List<String> subnets = wrapper.getAvailableSubnetsForDBInstanceClasses(ENGINE, ENGINE_VERSION,
				List.of(REPO_CLASS), List.of("subnet-a", "subnet-b", "subnet-c", "subnet-d"), 2);

		assertEquals(Arrays.asList("subnet-a", "subnet-c", "subnet-d"), subnets);
		verify(mockEc2ClientWrapper)
				.getAvailabityZoneToSubnetMap(List.of("subnet-a", "subnet-b", "subnet-c", "subnet-d"));
	}

	/**
	 * A subnet group is shared by every database of a stack, so only the zones that can host all of
	 * their instance classes can be included.
	 */
	@Test
	public void testGetAvailableSubnetsForDBInstanceClassesIsIntersection() {
		when(mockEc2ClientWrapper.getAvailabityZoneToSubnetMap(any())).thenReturn(zoneToSubnetMap());
		DescribeOrderableDBInstanceOptionsIterable repoOptions = optionsIterable("us-east-1a", "us-east-1c",
				"us-east-1d");
		DescribeOrderableDBInstanceOptionsIterable tableOptions = optionsIterable("us-east-1a", "us-east-1b",
				"us-east-1c");
		when(mockRds.describeOrderableDBInstanceOptionsPaginator(argThat(requestForClass(REPO_CLASS))))
				.thenReturn(repoOptions);
		when(mockRds.describeOrderableDBInstanceOptionsPaginator(argThat(requestForClass(TABLE_CLASS))))
				.thenReturn(tableOptions);

		// call under test
		List<String> subnets = wrapper.getAvailableSubnetsForDBInstanceClasses(ENGINE, ENGINE_VERSION,
				List.of(REPO_CLASS, TABLE_CLASS), List.of("subnet-a", "subnet-b", "subnet-c", "subnet-d"), 2);

		// only us-east-1a and us-east-1c offer both classes
		assertEquals(Arrays.asList("subnet-a", "subnet-c"), subnets);
	}

	/**
	 * A DB subnet group must span at least two zones, so the build must fail rather than deploy a
	 * database that cannot be Multi-AZ.
	 */
	@Test
	public void testGetAvailableSubnetsForDBInstanceClassesTooFew() {
		when(mockEc2ClientWrapper.getAvailabityZoneToSubnetMap(any())).thenReturn(zoneToSubnetMap());
		DescribeOrderableDBInstanceOptionsIterable repoOptions = optionsIterable("us-east-1a", "us-east-1c");
		DescribeOrderableDBInstanceOptionsIterable tableOptions = optionsIterable("us-east-1c", "us-east-1f");
		when(mockRds.describeOrderableDBInstanceOptionsPaginator(argThat(requestForClass(REPO_CLASS))))
				.thenReturn(repoOptions);
		when(mockRds.describeOrderableDBInstanceOptionsPaginator(argThat(requestForClass(TABLE_CLASS))))
				.thenReturn(tableOptions);
		List<String> classes = List.of(REPO_CLASS, TABLE_CLASS);
		List<String> allSubnets = List.of("subnet-a", "subnet-b", "subnet-c", "subnet-d");

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			wrapper.getAvailableSubnetsForDBInstanceClasses(ENGINE, ENGINE_VERSION, classes, allSubnets, 2);
		});
		assertEquals(String.format("Could not find 2 available subnets for DB instance classes %s in %s", classes,
				allSubnets), e.getMessage());
	}

	@Test
	public void testGetAvailableSubnetsForDBInstanceClassesWithNoClasses() {
		List<String> allSubnets = List.of("subnet-a", "subnet-b");

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			wrapper.getAvailableSubnetsForDBInstanceClasses(ENGINE, ENGINE_VERSION, List.of(), allSubnets, 2);
		});
		assertEquals("At least one DB instance class is required", e.getMessage());
		verifyNoMoreInteractions(mockRds);
	}

	@Test
	public void testValidateMultiAZMatches() {
		when(mockRds.describeDBInstances(instancesRequestCaptor.capture()))
				.thenReturn(instanceResponse(DBInstance.builder().dbInstanceIdentifier(INSTANCE_ID).multiAZ(true).build()));

		// call under test
		wrapper.validateMultiAZ(INSTANCE_ID, true);

		assertEquals(INSTANCE_ID, instancesRequestCaptor.getValue().dbInstanceIdentifier());
	}

	@Test
	public void testValidateMultiAZMatchesSingleAZ() {
		when(mockRds.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenReturn(
				instanceResponse(DBInstance.builder().dbInstanceIdentifier(INSTANCE_ID).multiAZ(false).build()));

		// call under test
		wrapper.validateMultiAZ(INSTANCE_ID, false);
	}

	/**
	 * This is the failure PLFM-9965 was opened for: the stack said Multi-AZ, RDS accepted the change
	 * and then abandoned it without an event, leaving the database single-AZ.
	 */
	@Test
	public void testValidateMultiAZAbandoned() {
		when(mockRds.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenReturn(
				instanceResponse(DBInstance.builder().dbInstanceIdentifier(INSTANCE_ID).multiAZ(false).build()));

		// call under test
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
			wrapper.validateMultiAZ(INSTANCE_ID, true);
		});
		assertTrue(e.getMessage().startsWith("Database prod-606-db should have MultiAZ=true but is MultiAZ=false."),
				e.getMessage());
	}

	/**
	 * A conversion can still be in flight when the stack reaches a stable state, which is not a
	 * failure.
	 */
	@Test
	public void testValidateMultiAZStillPending() {
		when(mockRds.describeDBInstances(any(DescribeDbInstancesRequest.class)))
				.thenReturn(instanceResponse(DBInstance.builder().dbInstanceIdentifier(INSTANCE_ID).multiAZ(false)
						.pendingModifiedValues(PendingModifiedValues.builder().multiAZ(true).build()).build()));

		// call under test
		wrapper.validateMultiAZ(INSTANCE_ID, true);

		verify(mockLogger).warn(
				"Multi-AZ change for prod-606-db is still pending, expected true but found false");
	}

	/**
	 * A pending change to something other than Multi-AZ does not excuse the mismatch.
	 */
	@Test
	public void testValidateMultiAZWithUnrelatedPendingChange() {
		when(mockRds.describeDBInstances(any(DescribeDbInstancesRequest.class)))
				.thenReturn(instanceResponse(DBInstance.builder().dbInstanceIdentifier(INSTANCE_ID).multiAZ(false)
						.pendingModifiedValues(PendingModifiedValues.builder().dbInstanceClass(REPO_CLASS).build())
						.build()));

		// call under test
		assertThrows(IllegalStateException.class, () -> {
			wrapper.validateMultiAZ(INSTANCE_ID, true);
		});
	}

	/**
	 * RDS reports MultiAZ as a Boolean, so an unset value must be read as single-AZ rather than
	 * throwing a NullPointerException.
	 */
	@Test
	public void testValidateMultiAZWithNullMultiAZ() {
		when(mockRds.describeDBInstances(any(DescribeDbInstancesRequest.class)))
				.thenReturn(instanceResponse(DBInstance.builder().dbInstanceIdentifier(INSTANCE_ID).build()));

		// call under test
		wrapper.validateMultiAZ(INSTANCE_ID, false);
	}

	@Test
	public void testValidateMultiAZWithUnknownInstance() {
		when(mockRds.describeDBInstances(any(DescribeDbInstancesRequest.class)))
				.thenThrow(DbInstanceNotFoundException.builder().message("nope").build());

		// call under test
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
			wrapper.validateMultiAZ(INSTANCE_ID, true);
		});
		assertEquals("Database does not exist: prod-606-db", e.getMessage());
	}

	@Test
	public void testValidateMultiAZWithEmptyResult() {
		when(mockRds.describeDBInstances(any(DescribeDbInstancesRequest.class)))
				.thenReturn(DescribeDbInstancesResponse.builder().build());

		// call under test
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
			wrapper.validateMultiAZ(INSTANCE_ID, true);
		});
		assertEquals("Database does not exist: prod-606-db", e.getMessage());
	}

	private static ArgumentMatcher<DescribeOrderableDbInstanceOptionsRequest> requestForClass(String dbInstanceClass) {
		return request -> request != null && dbInstanceClass.equals(request.dbInstanceClass());
	}

	private static DescribeDbInstancesResponse instanceResponse(DBInstance instance) {
		return DescribeDbInstancesResponse.builder().dbInstances(instance).build();
	}

	/**
	 * A single page of orderable options offering the given zones.
	 */
	private static DescribeOrderableDbInstanceOptionsResponse pageOf(String... zones) {
		List<AvailabilityZone> availabilityZones = Arrays.stream(zones)
				.map(zone -> AvailabilityZone.builder().name(zone).build()).collect(Collectors.toList());
		return DescribeOrderableDbInstanceOptionsResponse.builder()
				.orderableDBInstanceOptions(
						OrderableDBInstanceOption.builder().availabilityZones(availabilityZones).build())
				.build();
	}

	/**
	 * The paginator is a concrete class, so its stream is stubbed directly rather than driving it from
	 * an iterator.
	 */
	private static DescribeOrderableDBInstanceOptionsIterable iterableOf(
			List<DescribeOrderableDbInstanceOptionsResponse> pages) {
		DescribeOrderableDBInstanceOptionsIterable iterable = mock(DescribeOrderableDBInstanceOptionsIterable.class);
		when(iterable.stream()).thenReturn(pages.stream());
		return iterable;
	}

	/**
	 * A paginator returning a single page offering the given zones, or no pages at all when the class
	 * is not offered anywhere.
	 */
	private static DescribeOrderableDBInstanceOptionsIterable optionsIterable(String... zones) {
		return iterableOf(zones.length == 0 ? List.of() : List.of(pageOf(zones)));
	}

	private static Map<String, String> zoneToSubnetMap() {
		return Map.of("us-east-1a", "subnet-a", "us-east-1b", "subnet-b", "us-east-1c", "subnet-c", "us-east-1d",
				"subnet-d");
	}

}
