package org.sagebionetworks.template.s3;

import java.util.Objects;

/**
 * Expires (permanently deletes) the objects of a bucket that carry a given tag.
 * <p>
 * Note that S3 measures the number of days from the creation of the object, not from the moment the tag
 * was applied, so this cannot be used to express a waiting period that starts when something is tagged.
 */
public class S3TagExpirationConfiguration {

	private Integer days;
	private S3TagFilter tagFilter;

	public Integer getDays() {
		return days;
	}

	public S3TagExpirationConfiguration withDays(Integer days) {
		this.days = days;
		return this;
	}

	public S3TagFilter getTagFilter() {
		return tagFilter;
	}

	public S3TagExpirationConfiguration withTagFilter(S3TagFilter tagFilter) {
		this.tagFilter = tagFilter;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(days, tagFilter);
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
		S3TagExpirationConfiguration other = (S3TagExpirationConfiguration) obj;
		return Objects.equals(days, other.days) && Objects.equals(tagFilter, other.tagFilter);
	}

	@Override
	public String toString() {
		return "S3TagExpirationConfiguration [days=" + days + ", tagFilter=" + tagFilter + "]";
	}

}
