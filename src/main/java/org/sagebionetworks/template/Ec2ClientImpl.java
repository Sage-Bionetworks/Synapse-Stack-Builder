package org.sagebionetworks.template;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypeOfferingsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceTypeOffering;
import com.amazonaws.services.ec2.model.LocationType;
import com.amazonaws.services.ec2.model.Subnet;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;

import org.sagebionetworks.template.config.Configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Ec2ClientImpl implements Ec2Client {

	AmazonEC2 ec2;
	Configuration configuration;
	Logger logger;

	@Inject
	public Ec2ClientImpl(AmazonEC2 ec2Client, Configuration configuration, LoggerFactory loggerFactory) {
		this.ec2 = ec2Client;
		this.configuration = configuration;
		this.logger = loggerFactory.getLogger(Ec2ClientImpl.class);
	}

	@Override
	public List<String> getAvailabilityZonesForInstanceType(String instanceType) {
		Filter filterType = new Filter().withName("instance-type").withValues(instanceType);
		Collection<Filter> filters = Arrays.asList(filterType);
		DescribeInstanceTypeOfferingsRequest req = new DescribeInstanceTypeOfferingsRequest()
				.withFilters(filters)
				.withLocationType(LocationType.AvailabilityZone);
		DescribeInstanceTypeOfferingsResult res = ec2.describeInstanceTypeOfferings(req);
		List<String> l = res.getInstanceTypeOfferings().stream().map(e -> e.getLocation()).collect(Collectors.toList());
		return l;
	}

	@Override
	public Map<String, String> getAvailabityZoneToSubnetMap(List<String> subnetIds) {
		Filter filter = new Filter().withName("subnet-id").withValues(subnetIds);
		DescribeSubnetsRequest req = new DescribeSubnetsRequest().withFilters(filter);
		DescribeSubnetsResult res = ec2.describeSubnets(req);
		List<Subnet> subnets = res.getSubnets();
		Map<String, String> map = subnets.stream().collect(Collectors.toMap(Subnet::getAvailabilityZone, Subnet::getSubnetId));
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
