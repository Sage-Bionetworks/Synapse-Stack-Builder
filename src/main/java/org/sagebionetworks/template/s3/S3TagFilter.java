package org.sagebionetworks.template.s3;

import java.util.Objects;

public class S3TagFilter {

	private String name;
	private String value;

	public String getName() {
		return name;
	}

	public S3TagFilter withName(String name) {
		this.name = name;
		return this;
	}

	public String getValue() {
		return value;
	}

	public S3TagFilter withValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		S3TagFilter other = (S3TagFilter) obj;
		return Objects.equals(name, other.name) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "S3TagFilter [name=" + name + ", value=" + value + "]";
	}

}
