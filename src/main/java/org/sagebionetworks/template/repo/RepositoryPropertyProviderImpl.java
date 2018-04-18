package org.sagebionetworks.template.repo;

import static org.sagebionetworks.template.Constants.DEFAULT_REPO_PROPERTIES;

import java.util.Properties;

import org.sagebionetworks.template.PropertyUtils;

public class RepositoryPropertyProviderImpl implements RepositoryPropertyProvider {
	
	Properties props;
	
	public RepositoryPropertyProviderImpl() {
		props = new Properties();
		// add all default properties
		props.putAll(PropertyUtils.loadPropertiesFromClasspath(DEFAULT_REPO_PROPERTIES));
		// override the defaults from the system.
		props.putAll(System.getProperties());
	}

	@Override
	public String get(String key) {
		if(key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		String value = props.getProperty(key);
		if(value == null) {
			throw new IllegalArgumentException("Missing property value for key: '"+key+"'");
		}
		return value;
	}

	@Override
	public int getInteger(String key) {
		return Integer.parseInt(get(key));
	}

	@Override
	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(get(key));
	}

}
