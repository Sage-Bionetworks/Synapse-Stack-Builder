package org.sagebionetworks.template;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;

import org.sagebionetworks.template.config.Configuration;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.LocationType;
import software.amazon.awssdk.services.ec2.model.Subnet;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Ec2ClientImpl implements Ec2Client {

	software.amazon.awssdk.services.ec2.Ec2Client ec2;
	Configuration configuration;
	Logger logger;

	@Inject
	public Ec2ClientImpl(software.amazon.awssdk.services.ec2.Ec2Client ec2Client, Configuration configuration, LoggerFactory loggerFactory) {
		this.ec2 = ec2Client;
		this.configuration = configuration;
		this.logger = loggerFactory.getLogger(Ec2ClientImpl.class);
	}

	@Override
	public List<String> getAvailabilityZonesForInstanceType(String instanceType) {
		Filter filterType = Filter.builder().name("instance-type").values(instanceType).build();
		Collection<Filter> filters = Arrays.asList(filterType);
		DescribeInstanceTypeOfferingsRequest req = DescribeInstanceTypeOfferingsRequest.builder()
				.filters(filters)
				.locationType(LocationType.AVAILABILITY_ZONE)
				.build();
		DescribeInstanceTypeOfferingsResponse res = ec2.describeInstanceTypeOfferings(req);
		List<String> l = res.instanceTypeOfferings()
				.stream()
				.map(e -> e.location())
				.collect(Collectors.toList());
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
		Map<String, String> zoneToSubnetMap = getAvailabityZoneToSubnetMap(subnets);
		List<String> availableZones = getAvailabilityZonesForInstanceType(instanceType);
		List<String> availableSubnets = availableZones.stream().map(z -> zoneToSubnetMap.get(z)).filter(Objects::nonNull).sorted().collect(Collectors.toList());
		if (availableSubnets.size() < 2) {
			throw new IllegalArgumentException(String.format("Could not find 2 available subnets for type %s in %s ", instanceType, subnets));
		}
		return availableSubnets;
	}

}
