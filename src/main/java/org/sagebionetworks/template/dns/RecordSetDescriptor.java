package org.sagebionetworks.template.dns;

import com.amazonaws.services.route53.model.AliasTarget;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecordSetDescriptor {

	private String name;
	private String type;
	// null if alias
	private String ttl;
	// Should be one or the other for following
	private List<String> resourceRecords;
	private AliasTargetDescriptor aliasTargetDescriptor;

	public RecordSetDescriptor() {}

	public RecordSetDescriptor(ResourceRecordSet resourceRecordSet) {
		this.setName(resourceRecordSet.getName());
		this.setType(resourceRecordSet.getType());
		this.setTtl(resourceRecordSet.getTTL().toString());
		if (resourceRecordSet.getResourceRecords().size() > 0) {
			this.setResourceRecords(resourceRecordSet.getResourceRecords().stream().map(r -> r.getValue()).collect(Collectors.toList()));
		}
		if (resourceRecordSet.getAliasTarget() != null) {
			this.setAliasTargetDescriptor(new AliasTargetDescriptor(resourceRecordSet.getAliasTarget()));
		}
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
		ResourceRecordSet resourceRecordSet = new ResourceRecordSet();
		resourceRecordSet.setName(this.getName());
		resourceRecordSet.setType(this.getType());
		if (this.getTtl() != null) {
			resourceRecordSet.setTTL(Long.parseLong(this.getTtl()));
		}
		if (this.getResourceRecords() != null && this.getResourceRecords().size() > 0) {
			List<ResourceRecord> records = new ArrayList<>();
			for (String s: this.getResourceRecords()) {
				ResourceRecord rec = new ResourceRecord().withValue(s);
				records.add(rec);
			}
			resourceRecordSet.setResourceRecords(records);
		}
		if (this.getAliasTargetDescriptor() != null) {
			AliasTargetDescriptor desc = this.getAliasTargetDescriptor();
			AliasTarget aliasTarget = new AliasTarget().withDNSName(desc.getDnsName()).withHostedZoneId(desc.getHostedZoneId()).withEvaluateTargetHealth(desc.getEvaluateTargetHealth());
			resourceRecordSet.setAliasTarget(aliasTarget);
		}
		return resourceRecordSet;
	}
}

