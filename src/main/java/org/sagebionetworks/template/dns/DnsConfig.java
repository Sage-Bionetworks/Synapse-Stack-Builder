package org.sagebionetworks.template.dns;

import java.util.List;
import java.util.Objects;

public class DnsConfig {

	private String hostedZoneId;
	private List<RecordSetDescriptor> recordSetDescriptorList;

	public DnsConfig() {};

	public DnsConfig(String hostedZoneId, List<RecordSetDescriptor> recordSetDescriptorList) {
		this.setHostedZoneId(hostedZoneId);
		this.setRecordSetDescriptorList(recordSetDescriptorList);
	}

	static void validateRecordSetDescriptor(RecordSetDescriptor descriptor) {
		if (descriptor.getName() == null) {
			throw new IllegalArgumentException("Name cannot be null");
		}
		String rsType = descriptor.getType();
		if (rsType == null) {
			throw new IllegalArgumentException(("Type cannot be null"));
		}
		// TODO: Check type is valid value
		if (descriptor.getAliasTargetDescriptor() != null) {
			if (! "A".equals(rsType)) {
				throw new IllegalArgumentException("If alias target specified then recordDescriptor.type must be 'A'");
			}
			if (descriptor.getResourceRecords() != null) {
				throw new IllegalArgumentException("If alias target specified then resourceRecords must be null");
			}
			// Validating the alios is done in the class
		} else {
			if (descriptor.getResourceRecords() == null) {
				throw new IllegalArgumentException("If alias target not specified then resourceRecords must be specified");
			}
		}
	}

	public String getHostedZoneId() { return hostedZoneId; }

	public void setHostedZoneId(String hostedZoneId) {
		if (hostedZoneId == null) {
			throw new IllegalArgumentException("HostedZoneId cannot be null");
		}
		this.hostedZoneId = hostedZoneId;
	}
	public List<RecordSetDescriptor> getRecordSetDescriptorList() {
		return recordSetDescriptorList;
	}

	public void setRecordSetDescriptorList(List<RecordSetDescriptor> recordSetDescriptorList) {
		if (recordSetDescriptorList == null) {
			throw new IllegalArgumentException("RecordSetDescriptorList cannot be null");
		}
		if (recordSetDescriptorList.size() < 1) {
			throw new IllegalArgumentException("RecordSetDescriptorList must contain at least one element");
		}
		for (RecordSetDescriptor rsd: recordSetDescriptorList) {
			validateRecordSetDescriptor(rsd);
		}
		this.recordSetDescriptorList = recordSetDescriptorList;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DnsConfig dnsConfig = (DnsConfig) o;
		return hostedZoneId.equals(dnsConfig.hostedZoneId) && recordSetDescriptorList.equals(dnsConfig.recordSetDescriptorList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hostedZoneId, recordSetDescriptorList);
	}

	@Override
	public String toString() {
		return "DnsConfig{" +
				"hostedZoneId='" + hostedZoneId + '\'' +
				", recordSetDescriptorList=" + recordSetDescriptorList +
				'}';
	}
}
