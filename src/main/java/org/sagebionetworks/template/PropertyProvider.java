package org.sagebionetworks.template;

import java.util.Properties;

/**
 * Abstraction for loading properties.
 *
 */
public interface PropertyProvider {

	/**
	 * Get a property value for a key.
	 * 
	 * @param key
	 * @return
	 */
	String getProperty(String key);
	
	/**
	 * For properties that are comma separated lists of values,
	 * get the values as String[] array.
	 * @param key
	 * @return
	 */
	public String[] getComaSeparatedProperty(String key);
	
	/**
	 * Get the System.properties.
	 * @return
	 */
	public Properties getSystemProperties();
	
	/**
	 * Load properties from the classpath.
	 * 
	 * @param path
	 * @return
	 */
	public Properties loadPropertiesFromClasspath(String path);
}
