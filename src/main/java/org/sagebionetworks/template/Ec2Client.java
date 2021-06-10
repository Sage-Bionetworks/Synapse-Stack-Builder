package org.sagebionetworks.template;

import java.util.List;
import java.util.Map;

public interface Ec2Client {

	public List<String> getAvailabilityZonesForInstanceType(String instanceType);
	public Map<String, String> getAvailabityZoneToSubnetMap(List<String> subnetIds);
	public List<String> getAvailableSubnetsForInstanceType(String instanceType, List<String> subnets);

}
