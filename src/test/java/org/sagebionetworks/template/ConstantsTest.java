package org.sagebionetworks.template;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

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
		assertEquals("us-east-1-synapse-dev-vpc-2", vpcPrefix);
	}

	@Test
	public void testCreateCamelCaseName_allCapitalizedLetters(){
		String result = Constants.createCamelCaseName("MY_TEST_STRING", "_");
		assertEquals("MyTestString", result);
	}

	@Test
	public void testCeateCamelCaseName_givenCollection(){
		List<String> result = Constants.createCamelCaseName(Arrays.asList("TEST_Name_oNe", "test2"), "_");
		assertEquals(Arrays.asList("TestNameOne","Test2"), result);
	}
}
