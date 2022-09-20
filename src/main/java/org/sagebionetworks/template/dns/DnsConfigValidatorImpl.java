package org.sagebionetworks.template.dns;

import java.io.IOException;

public class DnsConfigValidatorImpl implements DnsConfigValidator {

	public DnsConfigValidatorImpl() {}

	@Override
	public void validate(DnsConfig dnsConfig) throws IOException {
		if (dnsConfig.getHostedZoneId() == null) {
			throw new IllegalArgumentException("HostedZoneId cannot be null");
		}
		if (dnsConfig.getRecordSetDescriptorList() == null) {
			throw new IllegalArgumentException("RecordSetDescriptorList cannot be null");
		}
		if (dnsConfig.getRecordSetDescriptorList().size() < 1) {
			throw new IllegalArgumentException("RecordSetDescriptorList cannot be empty");
		}
		for (RecordSetDescriptor desc: dnsConfig.getRecordSetDescriptorList()) {
			validateRecordSetDescriptor(desc);
		}
	}

	void validateRecordSetDescriptor(RecordSetDescriptor descriptor) {
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
			AliasTargetDescriptor aliasTargetDescriptor = descriptor.getAliasTargetDescriptor();
			if (aliasTargetDescriptor.getDnsName() == null) {
				throw new IllegalArgumentException("AliasTarget.DnsName cannot be null");
			}
			if (aliasTargetDescriptor.getEvaluateTargetHealth() != false) {
				throw new IllegalArgumentException("AliasTarget.EvaluateTargetHealth must be false");
			}
		} else {
			if (descriptor.getResourceRecords() == null) {
				throw new IllegalArgumentException("If alias target not specified then resourceRecords must be specified");
			}
		}
	}
}
