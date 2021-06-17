package org.sagebionetworks.template.config;

public interface Configuration {
	
	/**
	 * Initialize the configuration using the provided defaults property file.
	 * 
	 * @param defaultFilePath path to a property file on the classpath.
	 */
	public void initializeWithDefaults(String defaultFilePath);

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
	 * Get a value as an integer.
	 * @param key
	 * @return
	 */
	public int getIntegerProperty(String key);
	
	/**
	 * Get a value as a boolean.
	 * @param key
	 * @return
	 */
	public boolean getBooleanProperty(String key);
	
	/**
	 * Get the S3 bucket used for configuration.
	 * 
	 * @return
	 */
	public String getConfigurationBucket();
	
	
}
