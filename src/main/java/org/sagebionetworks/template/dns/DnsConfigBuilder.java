package org.sagebionetworks.template.dns;

import java.util.List;
import java.util.Objects;

public class DnsConfigBuilder {

	private String hostedZoneId;
	private List<RecordSetDescriptor> recordSetDescriptorList;

	public DnsConfigBuilder() {
	}

	public String getHostedZoneId() {
		return hostedZoneId;
	}

	public DnsConfigBuilder hostedZoneId(String hostedZoneId) {
		this.setHostedZoneId(hostedZoneId);
		return this;
	}

	public void setHostedZoneId(String hostedZoneId) {
		this.hostedZoneId = hostedZoneId;
	}

	public List<RecordSetDescriptor> getRecordSetDescriptorList() {
		return recordSetDescriptorList;
	}

	public DnsConfigBuilder recordSetDescriptorList(List<RecordSetDescriptor> recordSetDescriptors) {
		this.recordSetDescriptorList = recordSetDescriptors;
		return this;
	}

	public void setRecordSetDescriptorList(List<RecordSetDescriptor> recordSetDescriptorList) {
		this.recordSetDescriptorList = recordSetDescriptorList;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DnsConfigBuilder that = (DnsConfigBuilder) o;
		return hostedZoneId.equals(that.hostedZoneId) && recordSetDescriptorList.equals(that.recordSetDescriptorList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hostedZoneId, recordSetDescriptorList);
	}

	@Override
	public String toString() {
		return "DnsConfigBuilder{" +
				"hostedZoneId='" + hostedZoneId + '\'' +
				", recordSetDescriptorList=" + recordSetDescriptorList +
				'}';
	}

	static void validateRecordSetDescriptor(RecordSetDescriptor recordSetDescriptor) {
		if (recordSetDescriptor.getName() == null) {
			throw new IllegalArgumentException("RecordSetDescriptor.name cannot be null");
		}
		String rsType = recordSetDescriptor.getType();
		if (rsType == null) {
			throw new IllegalArgumentException(("RecordSetDescriptor.type cannot be null"));
		}
		// TTL can be null if alias
		// TODO: Check type is valid value
		if (recordSetDescriptor.getAliasTargetDescriptor() != null) {
			if (! "A".equals(rsType)) {
				throw new IllegalArgumentException("If alias target specified then RecordDescriptor.type must be 'A'");
			}
			if (recordSetDescriptor.getResourceRecords() != null) {
				throw new IllegalArgumentException("If alias target specified then RecordDescriptor.resourceRecords must be null");
			}
			validateAliasTargetDescriptor(recordSetDescriptor.getAliasTargetDescriptor());
		} else {
			if (recordSetDescriptor.getResourceRecords() == null) {
				throw new IllegalArgumentException("If alias target not specified then RecordDescriptor.resourceRecords must be specified");
			}
			// TODO: Could do more validation based on type
		}
	}

	static void validateAliasTargetDescriptor(AliasTargetDescriptor aliasTargetDescriptor) {
		if (aliasTargetDescriptor.getDnsName() == null) {
			throw new IllegalArgumentException("AliasTargetDescriptor.dnsName cannot be null");
		}
		if (aliasTargetDescriptor.getHostedZoneId() == null) {
			throw new IllegalArgumentException("AliasTargetDescriptor.hostedZoneId cannot be null");
		}
	}

	DnsConfig build() {
		// basic validation
		if (this.hostedZoneId == null) {
			throw new IllegalArgumentException("DnsConfigBuilder.hostedZoneId cannot be null");
		}
		if (this.recordSetDescriptorList == null || this.recordSetDescriptorList.size() < 1) {
			throw new IllegalArgumentException("DnsConfigBuilder.recordSetDescriptorList must have at least one element");
		}
		// recordsets validation
		for (RecordSetDescriptor rsd: this.recordSetDescriptorList) {
			validateRecordSetDescriptor(rsd);
		}
		DnsConfig config = new DnsConfig(hostedZoneId, recordSetDescriptorList);
		return config;
	}
}
