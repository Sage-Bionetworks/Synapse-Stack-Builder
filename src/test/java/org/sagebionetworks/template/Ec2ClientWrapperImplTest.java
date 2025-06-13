package org.sagebionetworks.template;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.config.Configuration;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.InstanceTypeOffering;
import software.amazon.awssdk.services.ec2.model.Subnet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Ec2ClientWrapperImplTest {

	public static final String INSTANCE_TYPE = "c6.xlarge";
	@Mock
	Ec2Client mockEC2;
	@Mock
	Configuration mockConfig;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;
	@Captor
	ArgumentCaptor<DescribeSubnetsRequest> describeSubnetRequestCaptor;
	@Captor
	ArgumentCaptor<DescribeInstanceTypeOfferingsRequest> describeInstanceOfferingsRequestCaptor;
	Ec2ClientWrapper ec2ClientWrapper;

	@BeforeEach
	void beforeEach() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		ec2ClientWrapper = new Ec2ClientWrapperImpl(mockEC2, mockConfig, mockLoggerFactory);
	}

	@Test
	void getAvailabityZoneToSubnetMap() {
		// Expect 6 subnets mapped to 6 zones
		List<Subnet> expectedSubnets = generateSubnets(6);
		DescribeSubnetsResponse expectedDescribeSubnetsResult = DescribeSubnetsResponse.builder().subnets(expectedSubnets).build();
		when(mockEC2.describeSubnets(describeSubnetRequestCaptor.capture())).thenReturn(expectedDescribeSubnetsResult);
		// Call under test
		Map<String, String> azToSubnetMap = ec2ClientWrapper.getAvailabityZoneToSubnetMap(Arrays.asList("subnet1", "subnet2", "subnet3", "subnet4", "subnet5", "subnet6"));
		assertNotNull(azToSubnetMap);
		assertEquals(expectedSubnets.size(), azToSubnetMap.size());
		assertEquals(1, describeSubnetRequestCaptor.getValue().filters().size());
		Filter f = describeSubnetRequestCaptor.getValue().filters().get(0);
		assertEquals("subnet-id", f.name());
		assertEquals(Arrays.asList("subnet1", "subnet2", "subnet3", "subnet4", "subnet5", "subnet6"), f.values());
	}

	@Test
	void getAvailabilityZonesForInstanceType() {
		List<InstanceTypeOffering> expectedOfferings = generateInstanceOfferings(INSTANCE_TYPE);
		DescribeInstanceTypeOfferingsResponse expectedDescribeInstanceTyepOfferingsResult = DescribeInstanceTypeOfferingsResponse.builder().instanceTypeOfferings(expectedOfferings).build();
		when(mockEC2.describeInstanceTypeOfferings(describeInstanceOfferingsRequestCaptor.capture())).thenReturn(expectedDescribeInstanceTyepOfferingsResult);

		// Call under test
		List<String> azsForInstanceType = ec2ClientWrapper.getAvailabilityZonesForInstanceType(INSTANCE_TYPE);

		assertNotNull(azsForInstanceType);
		assertEquals(4, azsForInstanceType.size());
		DescribeInstanceTypeOfferingsRequest r = describeInstanceOfferingsRequestCaptor.getValue();
		assertEquals(1, r.filters().size());
		Filter f = r.filters().get(0);
		assertEquals("instance-type", f.name());
		assertEquals(1, f.values().size());
		assertEquals(INSTANCE_TYPE, f.values().get(0));
	}

	@Test
	void getAvailableSubnetsForInstanceType() {
		List<Subnet> expectedSubnets = generateSubnets(6);
		DescribeSubnetsResponse expectedDescribeSubnetsResult = DescribeSubnetsResponse.builder().subnets(expectedSubnets).build();
		List<InstanceTypeOffering> expectedOfferings = generateInstanceOfferings(INSTANCE_TYPE);
		DescribeInstanceTypeOfferingsResponse expectedDescribeInstanceTypeOfferingsResult = DescribeInstanceTypeOfferingsResponse.builder().instanceTypeOfferings(expectedOfferings).build();
		when(mockEC2.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(expectedDescribeSubnetsResult);
		when(mockEC2.describeInstanceTypeOfferings(any(DescribeInstanceTypeOfferingsRequest.class))).thenReturn(expectedDescribeInstanceTypeOfferingsResult);
		List<String> subnets = Arrays.asList("subnet1", "subnet2", "subnet3", "subnet4", "subnet5", "subnet6");

		// Call under test
		List<String> availableSubnets = ec2ClientWrapper.getAvailableSubnetsForInstanceType(INSTANCE_TYPE, subnets);

		assertNotNull(availableSubnets);
		List<String> sortedAvailableSubnets = availableSubnets.stream().sorted().collect(Collectors.toList());
		assertEquals(sortedAvailableSubnets, availableSubnets);
		List<String> expectedAvailableSubnets = Arrays.asList("subnet1", "subnet2", "subnet4", "subnet5");
		assertEquals(expectedAvailableSubnets, availableSubnets);
	}

	@Test
	void getAvailableSubnetsForInstanceTypeTooSmall() {
		List<Subnet> expectedSubnets = generateSubnets(6);
		DescribeSubnetsResponse expectedDescribeSubnetsResult = DescribeSubnetsResponse.builder().subnets(expectedSubnets).build();
		List<InstanceTypeOffering> expectedOfferings = generateInstanceOfferings(INSTANCE_TYPE);
		DescribeInstanceTypeOfferingsResponse expectedDescribeInstanceTyepOfferingsResult = DescribeInstanceTypeOfferingsResponse.builder().build();
		when(mockEC2.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(expectedDescribeSubnetsResult);
		when(mockEC2.describeInstanceTypeOfferings(any(DescribeInstanceTypeOfferingsRequest.class))).thenReturn(expectedDescribeInstanceTyepOfferingsResult);
		List<String> subnets = Arrays.asList("subnet1", "subnet2", "subnet3", "subnet4", "subnet5", "subnet6");

		// Call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			ec2ClientWrapper.getAvailableSubnetsForInstanceType(INSTANCE_TYPE, subnets);});
		assertEquals(String.format("Could not find 2 available subnets for type %s in %s ", INSTANCE_TYPE, subnets), e.getMessage());

	}

	/**
	 * This maps subnet1 to us-east-1a, subnet2 to us-east-1b etc.
	 * @param numSubnets
	 * @return
	 */
	private List<Subnet> generateSubnets(int numSubnets) {
		List<Subnet> l = new ArrayList<>(numSubnets);
		for (int i = 0; i < numSubnets; i++) {
			Subnet s = Subnet.builder().subnetId(String.format("subnet%d", i+1)).availabilityZone(String.format("us-east-1%s", String.valueOf((char)(i + 'a')))).build();
			l.add(s);
		}
		return l;
	}

	/**
	 * This simulates offerings in subnet1, subnet2, subnet4 and subnet5 for any instance type
	 * @param instanceType
	 * @return
	 */
	private List<InstanceTypeOffering> generateInstanceOfferings(String instanceType) {
		String[] offeredAzs = {"us-east-1b", "us-east-1a", "us-east-1d", "us-east-1e"};
		List<InstanceTypeOffering> l = new ArrayList<>(4);
		for (int i = 0; i < 4; i++) {
			InstanceTypeOffering io = InstanceTypeOffering.builder().instanceType(instanceType).location(offeredAzs[i]).build();
			l.add(io);
		}
		return l;
	}

	/**
	 * This simulates offerings in subnet2 for any instance type
	 * @param instanceType
	 * @return
	 */
	private List<InstanceTypeOffering> generateInstanceOfferingsTooSmall(String instanceType) {
		String[] offeredAzs = {"us-east-1b"};
		List<InstanceTypeOffering> l = new ArrayList<>(1);
		InstanceTypeOffering io = InstanceTypeOffering.builder().instanceType(instanceType).location(offeredAzs[0]).build();
		l.add(io);
		return l;
	}

}