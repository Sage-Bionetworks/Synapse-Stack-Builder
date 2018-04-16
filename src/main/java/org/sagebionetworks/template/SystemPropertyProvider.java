package org.sagebionetworks.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provider for System Properties.
 *
 */
public class SystemPropertyProvider implements PropertyProvider {

	@Override
	public String getProperty(String key) {
		if(key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		String value = System.getProperty(key);
		if(value == null) {
			throw new IllegalArgumentException("Missing property value for key: '"+key+"'");
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
	public Properties getSystemProperties() {
		return System.getProperties();
	}
	

	@Override
	public Properties loadPropertiesFromClasspath(String path) {
		InputStream in = SystemPropertyProvider.class.getClassLoader().getResourceAsStream(path);
		if(in == null) {
			throw new IllegalArgumentException("Cannot find: "+path+" on the classpath.");
		}
		Properties props = new Properties();
		try {
			props.load(in);
		} catch (IOException e) {
			// convert to runtime.
			throw new RuntimeException(e);
		}
		return props;
	}

}
