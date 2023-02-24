package org.sagebionetworks.template.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.template.Constants.DEFAULT_REPO_PROPERTIES;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REPO_RDS_MULTI_AZ;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.config.ConfigurationImpl;

@ExtendWith(MockitoExtension.class)
public class ConfigurationImplTest {

	@InjectMocks
	ConfigurationImpl config;

	@Test
	public void testGetDeafult() {
		config.initializeWithDefaults(DEFAULT_REPO_PROPERTIES);
		String value = config.getProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE);
		assertEquals("5", value);
	}

	@Test
	public void testGetDoesNotExist() {
		assertThrows(ConfigurationPropertyNotFound.class, () -> {
			// call under test
			config.getProperty("does not exist");
		});
	}

	@Test
	public void testGetPropertyNullKey() {
		String key = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			config.getProperty(key);
		});
	}

	@Test
	public void testSystemOverrideDefault() {
		config.initializeWithDefaults(DEFAULT_REPO_PROPERTIES);
		// Load the default value.
		String defaultValue = config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS);
		assertEquals("db.t3.small", defaultValue);
		// Override the default by string a sytem property
		String overrrideValue = "this is the override";
		System.setProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS, overrrideValue);
		// reload
		config = new ConfigurationImpl();
		config.initializeWithDefaults(DEFAULT_REPO_PROPERTIES);
		assertEquals(overrrideValue, config.getProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS));
	}

	@Test
	public void testGetInteger() {
		config.initializeWithDefaults(DEFAULT_REPO_PROPERTIES);
		int value = config.getIntegerProperty(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE);
		assertEquals(5, value);
	}

	@Test
	public void testGetBoolean() {
		config.initializeWithDefaults(DEFAULT_REPO_PROPERTIES);
		boolean value = config.getBooleanProperty(PROPERTY_KEY_REPO_RDS_MULTI_AZ);
		assertFalse(value);
	}

	@Test
	public void testGetProperty() {
		String key = "org.sagebionetworks.somekey";
		String value = "some value";
		System.setProperty(key, value);
		config = new ConfigurationImpl();
		// call under test
		String result = config.getProperty(key);
		assertEquals(value, result);
	}

	@Test
	public void testGetComaSeparatedProperty() {
		String key = "org.sagebionetworks.somekey";
		String value = "foo , bar,foobar";
		System.setProperty(key, value);
		config = new ConfigurationImpl();
		// call under test
		String[] result = config.getComaSeparatedProperty(key);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals("foo", result[0]);
		assertEquals("bar", result[1]);
		assertEquals("foobar", result[2]);
	}

	@Test
	public void testGetComaSeparatedPropertySingle() {
		String key = "org.sagebionetworks.somekey";
		String value = "foo";
		System.setProperty(key, value);
		config = new ConfigurationImpl();
		// call under test
		String[] result = config.getComaSeparatedProperty(key);
		assertNotNull(result);
		assertEquals(1, result.length);
		assertEquals("foo", result[0]);
	}

}
