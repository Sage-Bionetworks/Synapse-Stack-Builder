package org.sagebionetworks.template;

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

}
