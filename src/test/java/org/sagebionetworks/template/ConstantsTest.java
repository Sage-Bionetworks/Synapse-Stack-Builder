package org.sagebionetworks.template;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConstantsTest {

	@Test
	public void testCreateAvailabilityZoneName() {
		String result = Constants.createCamelCaseName("us-east-1b", "-");
		assertEquals("UsEast1b", result);
	}
	
	@Test
	public void testCreateVpcExportPrefix() {
		String stack = "dev";
		// call under test
		String vpcPrefix = Constants.createVpcExportPrefix(stack);
		assertEquals("us-east-1-synapse-dev-vpc", vpcPrefix);
	}
}
