package org.sagebionetworks.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

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
	
	@Test
	public void testGetComaSeparatedProperty() {
		String key = "org.sagebionetworks.somekey";
		String value = "foo , bar,foobar";
		System.setProperty(key, value);
		// call under test
		String[] result = provider.getComaSeparatedProperty(key);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals("foo", result[0]);
		assertEquals("bar", result[1]);
		assertEquals("foobar", result[2]);
	}
	
	@Test
	public void testLoadPropertiesFromClasspath() {
		// call under test
		Properties props = provider.loadPropertiesFromClasspath("templates/repo/defaults.properties");
		assertNotNull(props);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testLoadPropertiesFromClasspathNotFound() {
		// call under test
		Properties props = provider.loadPropertiesFromClasspath("doesNotExist");
		assertNotNull(props);
	}
}
