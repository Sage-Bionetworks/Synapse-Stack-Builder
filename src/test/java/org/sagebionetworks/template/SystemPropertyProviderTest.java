package org.sagebionetworks.template;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class SystemPropertyProviderTest {
	
	SystemPropertyProvider provider;
	
	@Before
	public void before() {
		provider = new SystemPropertyProvider();
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetPropertyDoesNotExist() {
		// call under test
		provider.getProperty("Does not exist");
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetPropertyNullKey() {
		String key = null;
		// call under test
		provider.getProperty(key);
	}
	
	@Test
	public void testGetProperty() {
		String key = "org.sagebionetworks.somekey";
		String value = "some value";
		System.setProperty(key, value);
		// call under test
		String result = provider.getProperty(key);
		assertEquals(value, result);
	}
}
