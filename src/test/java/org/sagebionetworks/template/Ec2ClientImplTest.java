package org.sagebionetworks.template;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypeOfferingsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceTypeOffering;
import com.amazonaws.services.ec2.model.Subnet;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.config.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Ec2ClientImplTest {

	public static final String INSTANCE_TYPE = "c6.xlarge";
	@Mock
	AmazonEC2 mockEC2;
	@Mock
	Configuration mockConfig;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;
	@Mock
	DescribeSubnetsResult mockDescribeSubnetsResult;
	@Mock
	DescribeInstanceTypeOfferingsResult mockDescribeInstanceTyepOfferingsResult;
	@Captor
	ArgumentCaptor<DescribeSubnetsRequest> describeSubnetRequestCaptor;
	@Captor
	ArgumentCaptor<DescribeInstanceTypeOfferingsRequest> describeInstanceOfferingsRequestCaptor;
	Ec2Client ec2Client;

	@BeforeEach
	void beforeEach() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		ec2Client = new Ec2ClientImpl(mockEC2, mockConfig, mockLoggerFactory);
	}

	@Test
	void getAvailabityZoneToSubnetMap() {
		when(mockEC2.describeSubnets(describeSubnetRequestCaptor.capture())).thenReturn(mockDescribeSubnetsResult);
		// Expect 6 subnets mapped to 6 zones
		List<Subnet> expectedSubnets = generateSubnets(6);
		when(mockDescribeSubnetsResult.getSubnets()).thenReturn(expectedSubnets);
		// Call under test
		Map<String, String> azToSubnetMap = ec2Client.getAvailabityZoneToSubnetMap(Arrays.asList("subnet1", "subnet2", "subnet3", "subnet4", "subnet5", "subnet6"));
		assertNotNull(azToSubnetMap);
		assertEquals(expectedSubnets.size(), azToSubnetMap.size());
		assertEquals(1, describeSubnetRequestCaptor.getValue().getFilters().size());
		Filter f = describeSubnetRequestCaptor.getValue().getFilters().get(0);
		assertEquals("subnet-id", f.getName());
		assertEquals(Arrays.asList("subnet1", "subnet2", "subnet3", "subnet4", "subnet5", "subnet6"), f.getValues());
	}

	@Test
	void getAvailabilityZonesForInstanceType() {
		when(mockEC2.describeInstanceTypeOfferings(describeInstanceOfferingsRequestCaptor.capture())).thenReturn(mockDescribeInstanceTyepOfferingsResult);
		List<InstanceTypeOffering> expectedOfferings = generateInstanceOfferings(INSTANCE_TYPE);
		when(mockDescribeInstanceTyepOfferingsResult.getInstanceTypeOfferings()).thenReturn(expectedOfferings);
		// Call under test
		List<String> azsForInstanceType = ec2Client.getAvailabilityZonesForInstanceType(INSTANCE_TYPE);
		assertNotNull(azsForInstanceType);
		assertEquals(4, azsForInstanceType.size());
		DescribeInstanceTypeOfferingsRequest r = describeInstanceOfferingsRequestCaptor.getValue();
		assertEquals(1, r.getFilters().size());
		Filter f = r.getFilters().get(0);
		assertEquals("instance-type", f.getName());
		assertEquals(1, f.getValues().size());
		assertEquals(INSTANCE_TYPE, f.getValues().get(0));
	}

	@Test
	void getAvailableSubnetsForInstanceType() {
		when(mockEC2.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(mockDescribeSubnetsResult);
		when(mockEC2.describeInstanceTypeOfferings(any(DescribeInstanceTypeOfferingsRequest.class))).thenReturn(mockDescribeInstanceTyepOfferingsResult);
		List<Subnet> expectedSubnets = generateSubnets(6);
		when(mockDescribeSubnetsResult.getSubnets()).thenReturn(expectedSubnets);
		List<InstanceTypeOffering> expectedOfferings = generateInstanceOfferings(INSTANCE_TYPE);
		when(mockDescribeInstanceTyepOfferingsResult.getInstanceTypeOfferings()).thenReturn(expectedOfferings);
		List<String> subnets = Arrays.asList("subnet1", "subnet2", "subnet3", "subnet4", "subnet5", "subnet6");
		// Call under test
		List<String> availableSubnets = ec2Client.getAvailableSubnetsForInstanceType(INSTANCE_TYPE, subnets);
		assertNotNull(availableSubnets);
		List<String> sortedAvailableSubnets = availableSubnets.stream().sorted().collect(Collectors.toList());
		assertEquals(sortedAvailableSubnets, availableSubnets);
		List<String> expectedAvailableSubnets = Arrays.asList("subnet1", "subnet2", "subnet4", "subnet5");
		assertEquals(expectedAvailableSubnets, availableSubnets);
	}

	@Test
	void getAvailableSubnetsForInstanceTypeTooSmall() {
		when(mockEC2.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(mockDescribeSubnetsResult);
		when(mockEC2.describeInstanceTypeOfferings(any(DescribeInstanceTypeOfferingsRequest.class))).thenReturn(mockDescribeInstanceTyepOfferingsResult);
		List<Subnet> expectedSubnets = generateSubnets(6);
		when(mockDescribeSubnetsResult.getSubnets()).thenReturn(expectedSubnets);
		List<InstanceTypeOffering> expectedOfferings = generateInstanceOfferingsTooSmall(INSTANCE_TYPE);
		when(mockDescribeInstanceTyepOfferingsResult.getInstanceTypeOfferings()).thenReturn(expectedOfferings);
		List<String> subnets = Arrays.asList("subnet1", "subnet2", "subnet3", "subnet4", "subnet5", "subnet6");
		// Call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {ec2Client.getAvailableSubnetsForInstanceType(INSTANCE_TYPE, subnets);});
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
			Subnet s = new Subnet().withSubnetId(String.format("subnet%d", i+1)).withAvailabilityZone(String.format("us-east-1%s", String.valueOf((char)(i + 'a'))));
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
			InstanceTypeOffering io = new InstanceTypeOffering().withInstanceType(instanceType).withLocation(offeredAzs[i]);
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
		InstanceTypeOffering io = new InstanceTypeOffering().withInstanceType(instanceType).withLocation(offeredAzs[0]);
		l.add(io);
		return l;
	}

}