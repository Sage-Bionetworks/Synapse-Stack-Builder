package org.sagebionetworks.stack.config;

/**
 *
 * @author xschildw
 */
public enum ConfigSslPrefix {
	GENERIC("generic") 
	,
	PORTAL("portal")
	;
	private String prefix;
	
	ConfigSslPrefix(String cfg) {
		this.prefix = cfg;
	}
	
	public String getPrefix() {
		return this.prefix;
	}
}
