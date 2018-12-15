package org.sagebionetworks.template;

import org.sagebionetworks.template.config.Configuration;

/**
 * Thrown when a property can not be found for a {@link Configuration}
 */
public class ConfigurationPropertyNotFound extends RuntimeException{
	private String missingKey;

	public ConfigurationPropertyNotFound(String missingKey){
		super("Missing property value for key: '"+missingKey+"'");
		this.missingKey = missingKey;
	}

	public String getMissingKey() {
		return missingKey;
	}
}
