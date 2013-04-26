package org.sagebionetworks.stack.config;

/**
 *
 * @author xschildw
 */
public enum ConfigSslPrefix {
	GENERIC {
		@Override
		public String toString() {
			return "generic";
		}
	},
	PORTAL {
		@Override
		public String toString() {
			return "portal";
		}
	}
}
