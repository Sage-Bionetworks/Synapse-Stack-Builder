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
	
	/**
	 * For properties that are comma separated lists of values,
	 * get the values as String[] array.
	 * @param key
	 * @return
	 */
	public String[] getComaSeparatedProperty(String key);
}
