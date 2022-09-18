package org.sagebionetworks.template.dns;

import java.util.List;
import java.util.Objects;

public class DnsConfig {

	private String hostedZoneId;
	private List<RecordSetDescriptor> recordSetDescriptorList;

	public DnsConfig() {};

	public DnsConfig(String hostedZoneId, List<RecordSetDescriptor> recordSetDescriptorList) {
		this.hostedZoneId = hostedZoneId;
		this.recordSetDescriptorList = recordSetDescriptorList;
	}

	public String getHostedZoneId() { return hostedZoneId; }

	public void setHostedZoneId(String hostedZoneId) {
		this.hostedZoneId = hostedZoneId;
	}
	public List<RecordSetDescriptor> getRecordSetDescriptorList() {
		return recordSetDescriptorList;
	}

	public void setRecordSetDescriptorList(List<RecordSetDescriptor> recordSetDescriptorList) {
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
