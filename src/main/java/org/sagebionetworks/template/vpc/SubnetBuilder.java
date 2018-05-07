package org.sagebionetworks.template.vpc;

import org.sagebionetworks.template.Constants;

/**
 * Helper to build sub-nets. Sub-nets are group by a color.
 *
 */
public class SubnetBuilder {

	Color[] colors;
	String[] availabilityZones;
	int colorGroupNetMask;
	int subnetMask;
	String cidrPrefix;

	/**
	 * The color groups to build.
	 * 
	 * @param colors
	 * @return
	 */
	public SubnetBuilder withColors(Color... colors) {
		this.colors = colors;
		return this;
	}

	/**
	 * A private and public subnet will be created for each provided Availability Zones.
	 * 
	 * @param numberPrivateSubnets
	 * @return
	 */
	public SubnetBuilder withAvailabilityZones(String...availabilityZones) {
		this.availabilityZones = availabilityZones;
		return this;
	}

	/**
	 * The CIDR prefix that will be applied to each sub-net.
	 * 
	 * @param cidrPrefix
	 * @return
	 */
	public SubnetBuilder withCidrPrefix(String cidrPrefix) {
		if (cidrPrefix == null) {
			throw new IllegalArgumentException("CIDR prefix cannot be null");
		}
		String[] split = cidrPrefix.split("\\.");
		if (split.length != 2) {
			throw new IllegalArgumentException("CIDR prefix must two integers separated by dot. For example: '10.15'");
		}
		this.cidrPrefix = cidrPrefix;
		return this;
	}
	
	/**
	 * The network mask to be applied to each subnet
	 * 
	 * @param networkMask
	 * @return
	 */
	public SubnetBuilder withSubnetMask(int networkMask) {
		validateNetworkMask(networkMask);
		this.subnetMask = networkMask;
		return this;
	}
	
	/**
	 * The network mask to be applied to each color group.
	 * 
	 * @param networkMask
	 * @return
	 */
	public SubnetBuilder withColorGroupNetMaskSubnetMask(int networkMask) {
		validateNetworkMask(networkMask);
		this.colorGroupNetMask = networkMask;
		return this;
	}

	private void validateNetworkMask(int networkMask) {
		if (networkMask < 16 || networkMask > 32) {
			throw new IllegalArgumentException("Network mask must be a number between 16 and 32");
		}
	}

	/**
	 * Build the sub-nets for each color.
	 * 
	 * @return
	 */
	public SubnetGroup[] build() {
		SubnetGroup[] results = new SubnetGroup[colors.length];
		// This will be the IP address of the first sub-net
		String firstIpV4Address = this.cidrPrefix + ".0.0";
		long startAddress = IpAddressUtils.ipV4AddressToInteger(firstIpV4Address);
		int subnetNumBits = 32 - this.subnetMask;
		int colorGroupNumBits = 32 - this.colorGroupNetMask;
		long numberGroupAddresses = (int) Math.pow(2.0, (double) colorGroupNumBits);
		long numberSubnetAddresses = (int) Math.pow(2.0, (double) subnetNumBits);
		for (int i = 0; i < colors.length; i++) {
			long addressLong = startAddress + (numberGroupAddresses*i);
			Color color = colors[i];
			String colorCidr = createCIDR(addressLong, this.colorGroupNetMask);
			Subnet[] subnets = new Subnet[this.availabilityZones.length * 2];
			// create public sub-nets
			for (int pub = 0; pub < this.availabilityZones.length; pub++) {
				String availabilityZone = this.availabilityZones[pub];
				subnets[pub] = createSubnet(availabilityZone, addressLong, subnetMask, color, SubnetType.Public);
				addressLong += numberSubnetAddresses;
			}
			// create private sub-nets
			for (int pri = 0; pri < this.availabilityZones.length; pri++) {
				String availabilityZone = this.availabilityZones[pri];
				subnets[this.availabilityZones.length + pri] = createSubnet(availabilityZone, addressLong, subnetMask, color,
						SubnetType.Private);
				addressLong += numberSubnetAddresses;
			}
			results[i] = new SubnetGroup(color,colorCidr, subnets);
		}
		return results;
	}

	/**
	 * Create a new Subnet
	 * @param addressLong Long value of the subnet's IPv4 address.
	 * @param networkMask The networkmaks of the subnet's CIDR
	 * @param color The color group of this subnet.
	 * @param type Public or Private
	 * @param index Index within the Color-Type.
	 * @return
	 */
	static Subnet createSubnet(String availabilityZone, long addressLong, int networkMask, Color color, SubnetType type) {
		String cidr = createCIDR(addressLong, networkMask);
		String name = createSubnetName(color, type, availabilityZone);
		return new Subnet(name, cidr, type, availabilityZone);
	}

	/**
	 * Create a CIDR using the given address and network mask.
	 * 
	 * @param addressLong
	 * @param networkMask
	 * @return
	 */
	static String createCIDR(Long addressLong, int networkMask) {
		StringBuilder builder = new StringBuilder();
		String address = IpAddressUtils.integerToIpV4Address(addressLong);
		builder.append(address);
		builder.append("/");
		builder.append(networkMask);
		return builder.toString();
	}

	/**
	 * Subnet name: 'ColorTypeIndexSubnet'
	 * 
	 * @param color
	 * @param type
	 * @param index
	 * @return
	 */
	static String createSubnetName(Color color, SubnetType type, String availabilityZone) {
		StringBuilder builder = new StringBuilder();
		builder.append(color.name());
		builder.append(type.name());
		builder.append(Constants.createCamelCaseName(availabilityZone));
		builder.append("Subnet");
		return builder.toString();
	}
}