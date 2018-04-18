package org.sagebionetworks.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtils {

	/**
	 * Load a property file form the classpath.
	 * @param path
	 * @return
	 */
	public static Properties loadPropertiesFromClasspath(String path) {
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
