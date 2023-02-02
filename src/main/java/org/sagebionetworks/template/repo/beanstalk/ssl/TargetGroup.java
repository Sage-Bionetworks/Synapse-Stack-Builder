package org.sagebionetworks.template.repo.beanstalk.ssl;

import java.util.Objects;
import java.util.StringJoiner;

import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

public class TargetGroup {

	private final String shortName;
	private final String fullName;

	public TargetGroup(EnvironmentType type, String stack, String instance, int number) {
		super();
		this.fullName = new StringJoiner("-").add(type.getShortName()).add(stack).add(instance).add("" + number).toString();
		this.shortName = fullName.replaceAll("-", "");
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
		return Objects.hash(fullName, shortName);
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
		return Objects.equals(fullName, other.fullName) && Objects.equals(shortName, other.shortName);
	}


	@Override
	public String toString() {
		return "TargetGroup [shortName=" + shortName + ", fullName=" + fullName + "]";
	}


}
