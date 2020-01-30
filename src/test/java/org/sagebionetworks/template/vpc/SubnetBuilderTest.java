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
	public void testBuildNew() {
		SubnetBuilder builder = new SubnetBuilder();
		builder.withCidrPrefix("10.21");
		builder.withColors(Color.Red, Color.Blue);
		builder.withSubnetMask(24);
		builder.withColorGroupNetMaskSubnetMask(21);
		builder.withAvailabilityZones("us-east-1a", "us-east-1b", "us-east-1c", "us-east-1d", "us-east-1e", "us-east-1f");
		// call under test
		Subnets allSubnets = builder.build();
		assertNotNull(allSubnets);
		// there should be one public subnet for each AZ.
		assertNotNull(allSubnets.getPublicSubnets());
		assertEquals(6, allSubnets.getPublicSubnets().length);
		// there should be one private sub-net group for each color.
		assertNotNull(allSubnets.getPrivateSubnetGroups());
		assertEquals(2, allSubnets.getPrivateSubnetGroups().length);

		// public 1a
		Subnet subnet = allSubnets.getPublicSubnets()[0];
		assertEquals("PublicUsEast1a", subnet.getName());
		assertEquals("10.21.0.0/21", subnet.getCidr());
		assertEquals("us-east-1a", subnet.getAvailabilityZone());
		assertEquals("Public", subnet.getType());
		// public 1b
		subnet = allSubnets.getPublicSubnets()[1];
		assertEquals("PublicUsEast1b", subnet.getName());
		assertEquals("10.21.8.0/21", subnet.getCidr());
		assertEquals("us-east-1b", subnet.getAvailabilityZone());
		assertEquals("Public", subnet.getType());
		// public 1c
		subnet = allSubnets.getPublicSubnets()[2];
		assertEquals("PublicUsEast1c", subnet.getName());
		assertEquals("10.21.16.0/21", subnet.getCidr());
		assertEquals("us-east-1c", subnet.getAvailabilityZone());
		assertEquals("Public", subnet.getType());
		// public 1d
		subnet = allSubnets.getPublicSubnets()[3];
		assertEquals("PublicUsEast1d", subnet.getName());
		assertEquals("10.21.24.0/21", subnet.getCidr());
		assertEquals("us-east-1d", subnet.getAvailabilityZone());
		assertEquals("Public", subnet.getType());
		// public 1e
		subnet = allSubnets.getPublicSubnets()[4];
		assertEquals("PublicUsEast1e", subnet.getName());
		assertEquals("10.21.32.0/21", subnet.getCidr());
		assertEquals("us-east-1e", subnet.getAvailabilityZone());
		assertEquals("Public", subnet.getType());
		// public 1f
		subnet = allSubnets.getPublicSubnets()[5];
		assertEquals("PublicUsEast1f", subnet.getName());
		assertEquals("10.21.40.0/21", subnet.getCidr());
		assertEquals("us-east-1f", subnet.getAvailabilityZone());
		assertEquals("Public", subnet.getType());

		// red
		SubnetGroup color = allSubnets.getPrivateSubnetGroups()[0];
		assertEquals("10.21.48.0/21", color.getCidr());
		assertEquals("Red", color.getColor());
		Subnet[] subnets = color.getSubnets();
		assertNotNull(subnets);
		assertEquals(6, subnets.length);
		// red private 1a
		subnet = subnets[0];
		assertEquals("RedPrivateUsEast1a", subnet.getName());
		assertEquals("10.21.48.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// red private 1b
		subnet = subnets[1];
		assertEquals("RedPrivateUsEast1b", subnet.getName());
		assertEquals("10.21.49.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// red private 1c
		subnet = subnets[2];
		assertEquals("RedPrivateUsEast1c", subnet.getName());
		assertEquals("10.21.50.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// red private 1d
		subnet = subnets[3];
		assertEquals("RedPrivateUsEast1d", subnet.getName());
		assertEquals("10.21.51.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// red private 1e
		subnet = subnets[4];
		assertEquals("RedPrivateUsEast1e", subnet.getName());
		assertEquals("10.21.52.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// red private 1f
		subnet = subnets[5];
		assertEquals("RedPrivateUsEast1f", subnet.getName());
		assertEquals("10.21.53.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());

		// blue
		color = allSubnets.getPrivateSubnetGroups()[1];
		assertEquals("10.21.56.0/21", color.getCidr());
		assertEquals("Blue", color.getColor());
		subnets = color.getSubnets();
		assertNotNull(subnets);
		assertEquals(6, subnets.length);
		// blue private 1c
		subnet = subnets[0];
		assertEquals("BluePrivateUsEast1a", subnet.getName());
		assertEquals("10.21.56.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// blue private 1b
		subnet = subnets[1];
		assertEquals("BluePrivateUsEast1b", subnet.getName());
		assertEquals("10.21.57.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// blue private 1c
		subnet = subnets[2];
		assertEquals("BluePrivateUsEast1c", subnet.getName());
		assertEquals("10.21.58.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// blue private 1d
		subnet = subnets[3];
		assertEquals("BluePrivateUsEast1d", subnet.getName());
		assertEquals("10.21.59.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// blue private 1e
		subnet = subnets[4];
		assertEquals("BluePrivateUsEast1e", subnet.getName());
		assertEquals("10.21.60.0/24", subnet.getCidr());
		assertEquals("Private", subnet.getType());
		// blue private 1f
		subnet = subnets[5];
		assertEquals("BluePrivateUsEast1f", subnet.getName());
		assertEquals("10.21.61.0/24", subnet.getCidr());
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
