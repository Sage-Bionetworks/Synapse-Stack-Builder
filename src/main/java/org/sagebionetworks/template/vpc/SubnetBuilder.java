package org.sagebionetworks.template.vpc;

/**
 * Helper to build sub-nets.  Sub-nets are group by a color.
 *
 */
public class SubnetBuilder {

	Color[] colors;
	int numberPublicSubnets;
	int numberPrivateSubnets;
	int networkMask;
	String cidrPrefix;
	
	/**
	 * The color groups to build.
	 * 
	 * @param colors
	 * @return
	 */
	public SubnetBuilder withColors(Color...colors) {
		this.colors = colors;
		return this;
	}
	
	/**
	 * The number of public sub-nets that should be included in each color group.
	 *  
	 * @param numberPublicSubnets
	 * @return
	 */
	public SubnetBuilder withNumberPublicSubnets(int numberPublicSubnets) {
		this.numberPublicSubnets = numberPublicSubnets;
		return this;
	}
	
	/**
	 * The number of private sub-nets that should be included in each color group.
	 * 
	 * @param numberPrivateSubnets
	 * @return
	 */
	public SubnetBuilder withNumberPrivateSubnets(int numberPrivateSubnets) {
		this.numberPrivateSubnets = numberPrivateSubnets;
		return this;
	}
	
	/**
	 * The CIDR prefix that will be applied to each sub-net.
	 * @param cidrPrefix
	 * @return
	 */
	public SubnetBuilder withCidrPrefix(String cidrPrefix) {
		if(cidrPrefix == null) {
			throw new IllegalArgumentException("CIDR prefix cannot be null");
		}
		String[] split = cidrPrefix.split("\\.");
		if(split.length != 2) {
			throw new IllegalArgumentException("CIDR prefix must two integers separated by dot. For example: '10.15'");
		}
		this.cidrPrefix = cidrPrefix;
		return this;
	}
	
	public SubnetBuilder withNetworkMask(int networkMask) {
		if(networkMask < 16 || networkMask > 32) {
			throw new IllegalArgumentException("Networkmask must be a number between 16 and 32");
		}
		this.networkMask = networkMask;
		return this;
	}
	
	/**
	 * Build the sub-nets for each color.
	 * 
	 * @return
	 */
	public SubnetGroup[] build() {
		SubnetGroup[] results = new SubnetGroup[colors.length];
		// This will be the IP address of the first sub-net
		String firstIpV4Address = this.cidrPrefix+".0.0";
		long addressLong = IpAddressUtils.ipV4AddressToInteger(firstIpV4Address);
		int numberBits = 32 - this.networkMask;
		long numberAddresses = (int)Math.pow(2.0, (double) numberBits);
		for(int i=0; i<colors.length; i++) {
			// create public sub-nets
			String[] publicCidrs = new String[this.numberPublicSubnets];
			for(int pub=0; pub < this.numberPublicSubnets; pub++) {
				publicCidrs[pub] = createCIDR(addressLong, this.networkMask);
				addressLong += numberAddresses;
			}
			// create private sub-nets
			String[] privateCidrs = new String[this.numberPrivateSubnets];
			for(int pri=0; pri < this.numberPrivateSubnets; pri++) {
				privateCidrs[pri] = createCIDR(addressLong, this.networkMask);
				addressLong += numberAddresses;
			}
			results[i] = new SubnetGroup(colors[i], publicCidrs, privateCidrs);
		}
		return results;
	}
	
	/**
	 * Create a CIDR using the given address and network mask.
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
}
