package org.sagebionetworks.template.s3;

import java.util.Objects;

public class S3IntArchiveConfiguration {

	private Integer archiveAccessDays;
	private Integer deepArchiveAccessDays;
	private S3TagFilter tagFilter;

	public Integer getArchiveAccessDays() {
		return archiveAccessDays;
	}

	public S3IntArchiveConfiguration withArchiveAccessDays(Integer archiveAccessDays) {
		this.archiveAccessDays = archiveAccessDays;
		return this;
	}

	public Integer getDeepArchiveAccessDays() {
		return deepArchiveAccessDays;
	}

	public S3IntArchiveConfiguration withDeepArchiveAccessDays(Integer deepArchiveAccessDays) {
		this.deepArchiveAccessDays = deepArchiveAccessDays;
		return this;
	}

	public S3TagFilter getTagFilter() {
		return tagFilter;
	}

	public S3IntArchiveConfiguration withTagFilter(S3TagFilter tagFilter) {
		this.tagFilter = tagFilter;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(archiveAccessDays, deepArchiveAccessDays, tagFilter);
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
		S3IntArchiveConfiguration other = (S3IntArchiveConfiguration) obj;
		return Objects.equals(archiveAccessDays, other.archiveAccessDays)
				&& Objects.equals(deepArchiveAccessDays, other.deepArchiveAccessDays) && Objects.equals(tagFilter, other.tagFilter);
	}

	@Override
	public String toString() {
		return "S3IntArchiveConfiguration [archiveAccessDays=" + archiveAccessDays + ", deepArchiveAccessDays=" + deepArchiveAccessDays
				+ ", tagFilter=" + tagFilter + "]";
	}

}
