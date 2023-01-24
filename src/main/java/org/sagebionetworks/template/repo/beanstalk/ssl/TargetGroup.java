package org.sagebionetworks.template.repo.beanstalk.ssl;

import java.util.Objects;
import java.util.StringJoiner;

public class TargetGroup {

	private final int port;
	private final String shortName;
	private final String fullName;

	public TargetGroup(String stack, String instance, int number, int port) {
		super();
		this.port = port;
		this.fullName = new StringJoiner("-").add(stack).add(instance).add("" + number).add("" + port).toString();
		this.shortName = fullName.replaceAll("-", "");
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * @return the fullName
	 */
	public String getFullName() {
		return fullName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fullName, port, shortName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TargetGroup)) {
			return false;
		}
		TargetGroup other = (TargetGroup) obj;
		return Objects.equals(fullName, other.fullName) && port == other.port
				&& Objects.equals(shortName, other.shortName);
	}

	@Override
	public String toString() {
		return "TargetGroup [port=" + port + ", shortName=" + shortName + ", fullName=" + fullName + "]";
	}

}
