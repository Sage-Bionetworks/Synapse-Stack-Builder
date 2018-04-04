package org.sagebionetworks.template;

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
}
