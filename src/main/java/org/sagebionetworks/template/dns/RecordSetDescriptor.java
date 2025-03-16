package org.sagebionetworks.template.dns;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.route53.model.AliasTarget;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

public class RecordSetDescriptor {

	private String name;
	private String type;
	private String ttl; 	// null if alias

	// Should be one or the other for following
	private List<String> resourceRecords;
	private AliasTargetDescriptor aliasTargetDescriptor;

	public RecordSetDescriptor() {}

	public RecordSetDescriptor(String name, String type, String ttl, List<String> resourceRecords, AliasTargetDescriptor aliasTargetDescriptor) {
		this.name = name;
		this.type = type;
		this.ttl = ttl;
		this.resourceRecords = resourceRecords;
		this.aliasTargetDescriptor = aliasTargetDescriptor;
	}

	public RecordSetDescriptor(ResourceRecordSet resourceRecordSet) {
		this.name = resourceRecordSet.name();
		this.type = resourceRecordSet.type().toString();
		this.ttl = resourceRecordSet.ttl() != null ? resourceRecordSet.ttl().toString() : null;
		if (resourceRecordSet.resourceRecords() != null && resourceRecordSet.resourceRecords().size() > 0) {
			this.resourceRecords = resourceRecordSet.resourceRecords().stream().map(r -> r.value()).collect(Collectors.toList());
		} else {
			this.resourceRecords = null;
		}
		this.aliasTargetDescriptor = resourceRecordSet.aliasTarget() != null ? new AliasTargetDescriptor(resourceRecordSet.aliasTarget()) : null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) { this.name = name; }

	public String getType() { return type; }

	public void setType(String type) { this.type = type; }

	public String getTTL() {
		return ttl;
	}

	public void setTTL(String ttl) { this.ttl = ttl; }

	// can be null
	public List<String> getResourceRecords() {
		return resourceRecords;
	}

	public void setResourceRecords(List<String>resourceRecords) { this.resourceRecords = resourceRecords; }

	// can be null
	public AliasTargetDescriptor getAliasTargetDescriptor() {
		return aliasTargetDescriptor;
	}

	public void setAliasTargetDescriptor(AliasTargetDescriptor aliasTargetDescriptor) { this.aliasTargetDescriptor = aliasTargetDescriptor; }

	// can be null
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RecordSetDescriptor that = (RecordSetDescriptor) o;
		return name.equals(that.name) && type.equals(that.type) && Objects.equals(ttl, that.ttl) && Objects.equals(resourceRecords, that.resourceRecords) && Objects.equals(aliasTargetDescriptor, that.aliasTargetDescriptor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, ttl, resourceRecords, aliasTargetDescriptor);
	}

	@Override
	public String toString() {
		return "RecordSetDescriptor{" +
				", name='" + name + '\'' +
				", type='" + type + '\'' +
				", ttl='" + ttl + '\'' +
				", resourceRecords=" + resourceRecords +
				", aliasTargetDescriptor=" + aliasTargetDescriptor +
				'}';
	}

	public ResourceRecordSet toResourceRecordSet() {
		ResourceRecordSet.Builder builder = ResourceRecordSet.builder().name(this.getName()).type(this.getType());
		if (this.getTTL() != null) {
			builder.ttl(Long.parseLong(this.getTTL()));
		}
		if (this.getResourceRecords() != null && this.getResourceRecords().size() > 0) {
			List<ResourceRecord> records = new ArrayList<>();
			for (String s: this.getResourceRecords()) {
				ResourceRecord rec = ResourceRecord.builder().value(s).build();;
				records.add(rec);
			}
			builder.resourceRecords(records);
		}
		if (this.getAliasTargetDescriptor() != null) {
			AliasTargetDescriptor desc = this.getAliasTargetDescriptor();
			AliasTarget aliasTarget = AliasTarget.builder()
					.dnsName(desc.getDnsName())
					.hostedZoneId(desc.getHostedZoneId())
					.evaluateTargetHealth(desc.getEvaluateTargetHealth())
					.build();
			;
			builder.aliasTarget(aliasTarget);
		}
		return builder.build();
	}
}

