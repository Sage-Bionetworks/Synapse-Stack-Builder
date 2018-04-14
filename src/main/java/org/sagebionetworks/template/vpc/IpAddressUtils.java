package org.sagebionetworks.template.vpc;

public class IpAddressUtils {
	
	public static final long MAX_IP_V4_ADDRESS = (long) Math.pow(2, 32)-1;

	/**
	 * Convert an IPv4 address string to its corresponding long value.
	 * @param ipaddress
	 * @return
	 */
	public static long ipV4AddressToInteger(String ipaddress) {
		if(ipaddress == null) {
			throw new IllegalArgumentException("Address cannot be null");
		}
		String[] split = ipaddress.split("\\.");
		if(split.length != 4) {
			throw new IllegalArgumentException("Unknown IP address format: "+ipaddress);
		}
		long value = Integer.parseInt(split[3]);
		value += Long.parseLong(split[2]) << 8;
		value += Long.parseLong(split[1]) << 16;
		value += Long.parseLong(split[0]) << 24;
		return value;
	}
	
	/**
	 * Convert the long value of an IP address to an IPv4 address string.
	 * @param intValue
	 * @return
	 */
	public static String integerToIpV4Address(long value) {
		if(value >  MAX_IP_V4_ADDRESS) {
			throw new IllegalArgumentException("Value too large for IPv4"); 
		}
		StringBuilder builder = new StringBuilder();
		// shift by 24, 16, 8, and 0
		for(int i = 24; i >= 0; i -=8) {
			if(i < 24) {
				builder.append(".");
			}
			long shift = value >> i;
			builder.append(shift);
			value -= shift << i;
		}
		return builder.toString();
 	}
}
