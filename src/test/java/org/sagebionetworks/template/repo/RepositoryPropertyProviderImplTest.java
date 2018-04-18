package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import static org.sagebionetworks.template.Constants.*;

public class RepositoryPropertyProviderImplTest {

	RepositoryPropertyProvider props;

	@Before
	public void before() {
		props = new RepositoryPropertyProviderImpl();
	}

	@Test
	public void testGet() {
		String value = props.get(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS);
		assertEquals("db.t2.small", value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetDoesNotExist() {
		// call under test
		String value = props.get("does not exist");
		assertEquals("db.t2.small", value);
	}

	@Test
	public void testSystemOverrideDefault() {
		// Load the default value.
		String defaultValue = props.get(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS);
		assertEquals("db.t2.small", defaultValue);
		// Override the default by string a sytem property
		String overrrideValue = "this is the override";
		System.setProperty(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS, overrrideValue);
		// reload
		props = new RepositoryPropertyProviderImpl();
		assertEquals(overrrideValue, props.get(PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS));
	}
	
	@Test
	public void testGetInteger() {
		int value = props.getInteger(PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE);
		assertEquals(5, value);
	}
	
	@Test
	public void testGetBoolean() {
		boolean value = props.getBoolean(PROPERTY_KEY_REPO_RDS_MULTI_AZ);
		assertFalse(value);
	}

}
