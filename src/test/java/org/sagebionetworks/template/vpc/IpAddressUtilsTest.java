package org.sagebionetworks.template.vpc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IpAddressUtilsTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testIpV4AddressToIntegerNullAddress() {
		String ipAddress = null;
		// call under test
		IpAddressUtils.ipV4AddressToInteger(ipAddress);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testIpV4AddressToIntegerTooManyParts() {
		String ipAddress = "1.1.1.1.1";
		// call under test
		IpAddressUtils.ipV4AddressToInteger(ipAddress);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testIpV4AddressToIntegerNotEnoughParts() {
		String ipAddress = "1.1.1";
		// call under test
		IpAddressUtils.ipV4AddressToInteger(ipAddress);
	}
	
	@Test
	public void testIpV4ToIntegerBackToIpV4() {
		String ipAddress = "250.125.2.1";
		// call under test
		long value = IpAddressUtils.ipV4AddressToInteger(ipAddress);
		assertEquals(4202496513L, value);
		// call under test
		String ipAddressClone = IpAddressUtils.integerToIpV4Address(value);
		assertEquals(ipAddress, ipAddressClone);
	}

	@Test
	public void testIpV4ToIntegerBackToIpV4Zero() {
		String ipAddress = "0.0.0.0";
		// call under test
		long value = IpAddressUtils.ipV4AddressToInteger(ipAddress);
		assertEquals(0L, value);
		// call under test
		String ipAddressClone = IpAddressUtils.integerToIpV4Address(value);
		assertEquals(ipAddress, ipAddressClone);
	}
	
	@Test
	public void testIpV4ToIntegerBackToIpV4Max() {
		String ipAddress = "255.255.255.255";
		// call under test
		long value = IpAddressUtils.ipV4AddressToInteger(ipAddress);
		assertEquals(4294967295L, value);
		// call under test
		String ipAddressClone = IpAddressUtils.integerToIpV4Address(value);
		assertEquals(ipAddress, ipAddressClone);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testIntegerToIpV4AddressTooLarge() {
		String ipAddress = "255.255.255.255";
		long value = IpAddressUtils.ipV4AddressToInteger(ipAddress);
		// call under test
		IpAddressUtils.integerToIpV4Address(value+1);
	}

}
