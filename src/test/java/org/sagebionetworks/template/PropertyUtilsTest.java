package org.sagebionetworks.template;

import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Test;

public class PropertyUtilsTest {
	
	@Test
	public void testLoadPropertiesFromClasspath() {
		// call under test
		Properties props = PropertyUtils.loadPropertiesFromClasspath(Constants.DEFAULT_REPO_PROPERTIES);
		assertNotNull(props);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testLoadPropertiesFromClasspathNotFound() {
		// call under test
		PropertyUtils.loadPropertiesFromClasspath("does not exist");
	}

}
