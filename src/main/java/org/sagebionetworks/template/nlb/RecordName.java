package org.sagebionetworks.template.nlb;

import java.util.Objects;

import org.sagebionetworks.util.ValidateArgument;

/**
 * Represents a DNS record's name. For example, given: 'www.synapse.org' will
 * produce a shortName = 'wwwsynapseorg' and a longName= 'www-synapse-org'.
 * These names are used to identify various cloud formation resources associated
 * with a DNS record.
 *
 */
public class RecordName {

	private final String shortName;
	private final String longName;

	public RecordName(String name) {
		ValidateArgument.required(name, "name");
		name = name.toLowerCase();
		shortName = name.replaceAll("\\.", "");
		longName = name.replaceAll("\\.", "-");
	}

	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * @return the longName
	 */
	public String getLongName() {
		return longName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(longName, shortName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RecordName)) {
			return false;
		}
		RecordName other = (RecordName) obj;
		return Objects.equals(longName, other.longName) && Objects.equals(shortName, other.shortName);
	}

	@Override
	public String toString() {
		return "DomainName [shortName=" + shortName + ", longName=" + longName + "]";
	}

}
