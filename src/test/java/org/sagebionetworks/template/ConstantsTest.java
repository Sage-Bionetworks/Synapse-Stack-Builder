package org.sagebionetworks.template;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConstantsTest {

	@Test
	public void testCreateAvailabilityZoneName() {
		String result = Constants.createCamelCaseName("us-east-1b");
		assertEquals("UsEast1b", result);
	}
}
