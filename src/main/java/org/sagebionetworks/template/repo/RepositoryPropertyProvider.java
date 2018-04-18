package org.sagebionetworks.template.repo;

/**
 * Abstraction for getting Repository configuration properties.
 * @author John
 *
 */
public interface RepositoryPropertyProvider {
	
	/**
	 * Get a property value for a key.
	 * 
	 * @param key
	 * @return
	 */
	String get(String key);
	
	/**
	 * Get a value as an integer.
	 * @param key
	 * @return
	 */
	int getInteger(String key);
	
	/**
	 * Get a value as a boolean.
	 * @param key
	 * @return
	 */
	boolean getBoolean(String key);

}
