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
		builder.withSubnetMask(21);
		builder.withColorGroupNetMaskSubnetMask(20);
		builder.withAvailabilityZones("us-east-1c","us-east-1b");
		// call under test
		Subnets allSubnets = builder.build();
		assertNotNull(allSubnets);
		// there should be one public subnet for each AZ.
		assertNotNull(allSubnets.getPublicSubnets());
		assertEquals(2, allSubnets.getPublicSubnets().length);
		// there should be one private sub-net group for each color.
		assertNotNull(allSubnets.getPrivateSubnetGroups());
		assertEquals(2, allSubnets.getPrivateSubnetGroups().length);
		// public 1c
		Subnet subnet = allSubnets.getPublicSubnets()[0];
		assertEquals("PublicUsEast1c", subnet.getName());
		assertEquals("10.21.0.0/20", subnet.getCidr());
		assertEquals("us-east-1c", subnet.getAvailabilityZone());
		assertEquals("Public", subnet.getType());
		// public 1b
		subnet = allSubnets.getPublicSubnets()[1];
		assertEquals("PublicUsEast1b", subnet.getName());
		assertEquals("10.21.16.0/20", subnet.getCidr());
		assertEquals("us-east-1b", subnet.getAvailabilityZone());
		assertEquals("Public", subnet.getType());

		// red
		SubnetGroup color = allSubnets.getPrivateSubnetGroups()[0];
		assertEquals("10.21.32.0/20", color.getCidr());
		assertEquals("Red", color.getColor());
		Subnet[] subnets = color.getSubnets();
		assertNotNull(subnets);
		assertEquals(2, subnets.length);
		// red private 1c
		subnet = subnets[0];
		assertEquals("RedPrivateUsEast1c", subnet.getName());
		assertEquals("10.21.32.0/21", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// red private 1b
		subnet = subnets[1];
		assertEquals("RedPrivateUsEast1b", subnet.getName());
		assertEquals("10.21.40.0/21", subnet.getCidr());
		assertEquals("Private", subnet.getType());

		// blue
		color = allSubnets.getPrivateSubnetGroups()[1];
		assertEquals("10.21.48.0/20", color.getCidr());
		assertEquals("Blue", color.getColor());
		subnets = color.getSubnets();
		assertNotNull(subnets);
		assertEquals(2, subnets.length);
		// blue private 1c
		subnet = subnets[0];
		assertEquals("BluePrivateUsEast1c", subnet.getName());
		assertEquals("10.21.48.0/21", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// blue private 1b
		subnet = subnets[1];
		assertEquals("BluePrivateUsEast1b", subnet.getName());
		assertEquals("10.21.56.0/21", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		
	}
	
	@Test
	public void testCreateSubnet() {
		long addressLong =(long) Math.pow(2, 32)-1;
		int networkMask = 8;
		Color color = Color.Red;
		SubnetType type = SubnetType.Public;
		String availabilityZone = "us-east-1c";
		// Call under test
		Subnet result = SubnetBuilder.createSubnet(availabilityZone, addressLong, networkMask, color, type);
		assertNotNull(result);
		assertEquals("RedPublicUsEast1c", result.getName());
		assertEquals("255.255.255.255/8", result.getCidr());
		assertEquals("Public", result.getType());
		assertEquals("us-east-1c", result.getAvailabilityZone());
	}
	
	@Test
	public void testCreateSubnetName() {
		Color color = Color.Blue;
		SubnetType type = SubnetType.Private;
		String availabilityZone = "us-east-1c";
		// call under test
		String name = SubnetBuilder.createSubnetName(color, type, availabilityZone);
		assertEquals("BluePrivateUsEast1c", name);
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
