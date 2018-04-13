package org.sagebionetworks.template.vpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SubnetBuilderTest {

	@Test
	public void testBuild() {
		SubnetBuilder builder = new SubnetBuilder();
		builder.withCidrPrefix("10.21");
		builder.withColors(Color.Red, Color.Blue);
		builder.withSubnetMask(22);
		builder.withColorGroupNetMaskSubnetMask(20);
		builder.withPublicAvailabilityZones("us-east-1c","us-east-1b");
		builder.withPrivateAvailabilityZones("us-east-1a");
		// call under test
		SubnetGroup[] results = builder.build();
		assertNotNull(results);
		assertEquals(2, results.length);

		// red
		SubnetGroup color = results[0];
		assertEquals("10.21.0.0/20", color.getCidr());
		assertEquals("Red", color.getColor());
		Subnet[] subnets = color.getSubnets();
		assertNotNull(subnets);
		assertEquals(3, subnets.length);
		// red public 0
		Subnet subnet = subnets[0];
		assertEquals("RedPublic0Subnet", subnet.getName());
		assertEquals("10.21.0.0/22", subnet.getCidr());
		assertEquals("Public", subnet.getType());
		// red public 1
		subnet = subnets[1];
		assertEquals("RedPublic1Subnet", subnet.getName());
		assertEquals("10.21.4.0/22", subnet.getCidr());
		assertEquals("Public", subnet.getType());
		
		// red private 0
		subnet = subnets[2];
		assertEquals("RedPrivate0Subnet", subnet.getName());
		assertEquals("10.21.8.0/22", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		
		// blue
		color = results[1];
		assertEquals("10.21.16.0/20", color.getCidr());
		assertEquals("Blue", color.getColor());
		subnets = color.getSubnets();
		assertNotNull(subnets);
		assertEquals(3, subnets.length);
		// blue public 0
		subnet = subnets[0];
		assertEquals("BluePublic0Subnet", subnet.getName());
		assertEquals("10.21.16.0/22", subnet.getCidr());
		assertEquals("Public", subnet.getType());
		// blue public 1
		subnet = subnets[1];
		assertEquals("BluePublic1Subnet", subnet.getName());
		assertEquals("10.21.20.0/22", subnet.getCidr());
		assertEquals("Public", subnet.getType());
		
		// blue private 0
		subnet = subnets[2];
		assertEquals("BluePrivate0Subnet", subnet.getName());
		assertEquals("10.21.24.0/22", subnet.getCidr());
		assertEquals("Private", subnet.getType());
	}
	
	@Test
	public void testCreateSubnet() {
		long addressLong =(long) Math.pow(2, 32)-1;
		int networkMask = 8;
		Color color = Color.Red;
		SubnetType type = SubnetType.Public;
		int index = 0;
		String availabilityZone = "us-east-1c";
		// Call under test
		Subnet result = SubnetBuilder.createSubnet(availabilityZone, addressLong, networkMask, color, type, index);
		assertNotNull(result);
		assertEquals("RedPublic0Subnet", result.getName());
		assertEquals("255.255.255.255/8", result.getCidr());
		assertEquals("Public", result.getType());
		assertEquals("us-east-1c", result.getAvailabilityZone());
	}
	
	@Test
	public void testCreateSubnetName() {
		Color color = Color.Blue;
		SubnetType type = SubnetType.Private;
		int index = 1;
		// call under test
		String name = SubnetBuilder.createSubnetName(color, type, index);
		assertEquals("BluePrivate1Subnet", name);
	}

	@Test
	public void testCreateCIDR() {
		long addressValue = 0L;
		int subnetMask = 16;
		// call under test
		String result = SubnetBuilder.createCIDR(addressValue, subnetMask);
		assertEquals("0.0.0.0/16", result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithCidrPrefixNull() {
		String prefix = null;
		SubnetBuilder builder = new SubnetBuilder();
		// call under test
		builder.withCidrPrefix(prefix);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithCidrPrefixWrongFormat() {
		String prefix = "wrong";
		SubnetBuilder builder = new SubnetBuilder();
		// call under test
		builder.withCidrPrefix(prefix);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithSubnetMaskTooLow() {
		SubnetBuilder builder = new SubnetBuilder();
		// call under test
		builder.withSubnetMask(15);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testWithSubnetMaskTooHigh() {
		SubnetBuilder builder = new SubnetBuilder();
		// call under test
		builder.withSubnetMask(33);
	}
}
