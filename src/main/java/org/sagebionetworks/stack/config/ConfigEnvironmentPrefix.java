package org.sagebionetworks.stack.config;

/**
 *
 * @author xschildw
 */
public enum ConfigEnvironmentPrefix {
	RDS {
			@Override
			public String toString() {
				return "rds";
			}
	},
	SEARCH {
			@Override
			public String toString() {
				return "search";
			}
	},
	AUTH {
			@Override
			public String toString() {
				return "auth";
			}
	},
	REPO {
			@Override
			public String toString() {
				return "repo";
			}
	},
	PORTAL {
			@Override
			public String toString() {
				return "portal";
			}
	},
	DYNAMO {
			@Override
			public String toString() {
				return "dynamo";
			}
	},
	FILE {
			@Override
			public String toString() {
				return "file";
			}
	};
	
	
}
