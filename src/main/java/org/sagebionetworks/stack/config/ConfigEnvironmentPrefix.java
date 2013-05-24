package org.sagebionetworks.stack.config;

/**
 *
 * @author xschildw
 */
public enum ConfigEnvironmentPrefix {
	RDS("rds"),
	SEARCH("search"),
	AUTH("auth"),
	REPO("repo"),
	PORTAL("portal"),
	DYNAMO("dynamo"),
	FILE("file");
	
	private String prefix;
	
	ConfigEnvironmentPrefix(String s) {
		this.prefix = s;
	}
	
	public String getPrefix() {
		return this.prefix;
	}
}
