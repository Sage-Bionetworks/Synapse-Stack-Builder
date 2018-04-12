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
		builder.withNetworkMask(22);
		builder.withNumberPublicSubnets(2);
		builder.withNumberPrivateSubnets(1);
		// call under test
		SubnetGroup[] results = builder.build();
		assertNotNull(results);
		assertEquals(2, results.length);

		// red
		SubnetGroup red = results[0];
		assertEquals(Color.Red.name(), red.getColor());
		// red public
		assertNotNull(red.getPublicCidrs());
		assertEquals(2, red.getPublicCidrs().length);
		assertEquals("10.21.0.0/22", red.getPublicCidrs()[0]);
		assertEquals("10.21.4.0/22", red.getPublicCidrs()[1]);
		// red private
		assertNotNull(red.getPrivateCidrs());
		assertEquals(1, red.getPrivateCidrs().length);
		assertEquals("10.21.8.0/22", red.getPrivateCidrs()[0]);

		// blue
		SubnetGroup blue = results[1];
		assertEquals(Color.Blue.name(), blue.getColor());
		// blue public
		assertNotNull(blue.getPublicCidrs());
		assertEquals(2, blue.getPublicCidrs().length);
		assertEquals("10.21.12.0/22", blue.getPublicCidrs()[0]);
		assertEquals("10.21.16.0/22", blue.getPublicCidrs()[1]);
		// blue private
		assertNotNull(red.getPrivateCidrs());
		assertEquals(1, red.getPrivateCidrs().length);
		assertEquals("10.21.20.0/22", blue.getPrivateCidrs()[0]);
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
		builder.withNetworkMask(15);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testWithSubnetMaskTooHigh() {
		SubnetBuilder builder = new SubnetBuilder();
		// call under test
		builder.withNetworkMask(33);
	}
}
