package org.sagebionetworks.template;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;

import org.sagebionetworks.template.config.Configuration;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.LocationType;
import software.amazon.awssdk.services.ec2.model.Subnet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Ec2ClientWrapperImpl implements Ec2ClientWrapper {

	Ec2Client ec2;
	Configuration configuration;
	Logger logger;

	@Inject
	public Ec2ClientWrapperImpl(Ec2Client ec2Client, Configuration configuration, LoggerFactory loggerFactory) {
		this.ec2 = ec2Client;
		this.configuration = configuration;
		this.logger = loggerFactory.getLogger(Ec2ClientWrapperImpl.class);
	}

	@Override
	public List<String> getAvailabilityZonesForInstanceType(String instanceType) {
		Filter filterType = Filter.builder().name("instance-type").values(instanceType).build();
		Collection<Filter> filters = Arrays.asList(filterType);
		DescribeInstanceTypeOfferingsRequest req = DescribeInstanceTypeOfferingsRequest.builder()
				.filters(filters)
				.locationType(LocationType.AVAILABILITY_ZONE).build();
		DescribeInstanceTypeOfferingsResponse res = ec2.describeInstanceTypeOfferings(req);
		List<String> l = res.instanceTypeOfferings().stream().map(e -> e.location()).collect(Collectors.toList());
		return l;
	}

	@Override
	public Map<String, String> getAvailabityZoneToSubnetMap(List<String> subnetIds) {
		Filter filter = Filter.builder().name("subnet-id").values(subnetIds).build();
		DescribeSubnetsRequest req = DescribeSubnetsRequest.builder().filters(filter).build();
		DescribeSubnetsResponse res = ec2.describeSubnets(req);
		List<Subnet> subnets = res.subnets();
		Map<String, String> map = subnets.stream().collect(Collectors.toMap(Subnet::availabilityZone, Subnet::subnetId));
		return map;
	}

	@Override
	public List<String> getAvailableSubnetsForInstanceType(String instanceType, List<String> subnets) {
		return getAvailableSubnetsForInstanceTypes(List.of(instanceType), subnets, 2);
	}

	@Override
	public List<String> getAvailableSubnetsForInstanceTypes(List<String> instanceTypes, List<String> subnets, int minCount) {
		Map<String, String> zoneToSubnetMap = getAvailabityZoneToSubnetMap(subnets);
		// Availability zones that offer every requested instance type.
		Set<String> commonZones = null;
		for (String instanceType : instanceTypes) {
			List<String> zones = getAvailabilityZonesForInstanceType(instanceType);
			if (commonZones == null) {
				commonZones = new HashSet<>(zones);
			} else {
				commonZones.retainAll(zones);
			}
		}
		List<String> availableSubnets = commonZones.stream().map(zoneToSubnetMap::get).filter(Objects::nonNull).sorted().collect(Collectors.toList());
		if (availableSubnets.size() < minCount) {
			throw new IllegalArgumentException(String.format("Could not find %d available subnets for types %s in %s ", minCount, instanceTypes, subnets));
		}
		return availableSubnets;
	}

}
