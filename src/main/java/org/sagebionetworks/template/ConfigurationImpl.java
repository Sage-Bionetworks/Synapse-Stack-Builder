package org.sagebionetworks.template;

import static org.sagebionetworks.template.Constants.CONFIGURATION_BUCKET_TEMPLATE;

import java.util.Properties;

public class ConfigurationImpl implements Configuration {

	Properties props;
	
	public ConfigurationImpl() {
		// no defaults by default
		String defaultFilePath = null;
		initialize(defaultFilePath);
	}
	
	@Override
	public void initializeWithDefaults(String defaultFilePath) {
		initialize(defaultFilePath);
	}
	
	/**
	 * Setup the properties object.
	 * @param defaultFilePath
	 */
	private void initialize(String defaultFilePath) {
		props = new Properties();
		if(defaultFilePath != null) {
			// add all default properties
			props.putAll(PropertyUtils.loadPropertiesFromClasspath(defaultFilePath));
		}
		// override the defaults from the system.
		props.putAll(System.getProperties());
	}

	@Override
	public String getProperty(String key) {
		if(key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		String value = props.getProperty(key);
		if(value == null) {
			throw new ConfigurationPropertyNotFound(key);
		}
		return value;
	}

	@Override
	public String[] getComaSeparatedProperty(String key) {
		String csv = getProperty(key);
		String[] split = csv.split(",");
		// trim
		for(int i=0; i<split.length;i++) {
			split[i] = split[i].trim();
		}
		return split;
	}

	@Override
	public int getIntegerProperty(String key) {
		return Integer.parseInt(getProperty(key));
	}

	@Override
	public boolean getBooleanProperty(String key) {
		return Boolean.parseBoolean(getProperty(key));
	}

	@Override
	public String getConfigurationBucket() {
		String stack = getProperty(Constants.PROPERTY_KEY_STACK);
		return String.format(CONFIGURATION_BUCKET_TEMPLATE, stack);
	}

}
