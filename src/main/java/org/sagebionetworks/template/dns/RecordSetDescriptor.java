package org.sagebionetworks.template.dns;

import java.util.List;
import java.util.Objects;

public class RecordSetDescriptor {

	private String comment;
	private String hostedZoneId;
	private String name;
	private String type;
	private String ttl;
	// Should be one or the other for following
	private List<String> resourceRecords;
	private AliasTargetDescriptor aliasTargetDescriptor;

	public RecordSetDescriptor() {}

	public RecordSetDescriptor(String hostedZoneId, String name, String type) {
		this.hostedZoneId = hostedZoneId;
		this.name = name;
		this.type = type;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getHostedZoneId() {
		return hostedZoneId;
	}

	public void setHostedZoneId(String hostZoneId) {
		this.hostedZoneId = hostZoneId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTtl() {
		return ttl;
	}

	public void setTtl(String ttl) {
		this.ttl = ttl;
	}

	public List<String> getResourceRecords() {
		return resourceRecords;
	}

	public void setResourceRecords(List<String> resourceRecords) {
		this.resourceRecords = resourceRecords;
	}

	public AliasTargetDescriptor getAliasTargetDescriptor() {
		return aliasTargetDescriptor;
	}

	public void setAliasTargetDescriptor(AliasTargetDescriptor aliasTargetDescriptor) {
		this.aliasTargetDescriptor = aliasTargetDescriptor;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RecordSetDescriptor that = (RecordSetDescriptor) o;
		return Objects.equals(comment, that.comment) && Objects.equals(hostedZoneId, that.hostedZoneId) && name.equals(that.name) && type.equals(that.type) && Objects.equals(ttl, that.ttl) && Objects.equals(resourceRecords, that.resourceRecords) && Objects.equals(aliasTargetDescriptor, that.aliasTargetDescriptor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(comment, hostedZoneId, name, type, ttl, resourceRecords, aliasTargetDescriptor);
	}

	@Override
	public String toString() {
		return "RecordSetDescriptor{" +
				"comment='" + comment + '\'' +
				", hostZoneId='" + hostedZoneId + '\'' +
				", name='" + name + '\'' +
				", type='" + type + '\'' +
				", ttl='" + ttl + '\'' +
				", resourceRecords=" + resourceRecords +
				", aliasTargetDescriptor=" + aliasTargetDescriptor +
				'}';
	}
}

