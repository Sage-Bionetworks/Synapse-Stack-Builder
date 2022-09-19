package org.sagebionetworks.template.dns;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DnsConfigValidatorImplTest {

	@Mock
	DnsConfig mockDnsConfig;
	@InjectMocks
	private DnsConfigValidatorImpl dnsConfigValidator;

	@Mock
	private RecordSetDescriptor mockRecordSetDescriptor;
	@Mock
	private AliasTargetDescriptor mockAliasTargetDescriptor;

	@Test
	void testValidateNullHostedZoneId() {
		when(mockDnsConfig.getHostedZoneId()).thenReturn(null);
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validate(mockDnsConfig);
		});
		assertEquals("HostedZoneId cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateNullRecordSetDescriptorList() {
		when(mockDnsConfig.getHostedZoneId()).thenReturn("hostedZoneId");
		when(mockDnsConfig.getRecordSetDescriptorList()).thenReturn(null);
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validate(mockDnsConfig);
		});
		assertEquals("RecordSetDescriptorList cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateEmptyRecordSetDescriptorList() {
		when(mockDnsConfig.getHostedZoneId()).thenReturn("hostedZoneId");
		when(mockDnsConfig.getRecordSetDescriptorList()).thenReturn(new ArrayList<RecordSetDescriptor>());
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validate(mockDnsConfig);
		});
		assertEquals("RecordSetDescriptorList cannot be empty", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorMissingName() {
		when(mockRecordSetDescriptor.getName()).thenReturn(null);
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("Name cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorMissingType() {
		when(mockRecordSetDescriptor.getName()).thenReturn("dnsName");
		when(mockRecordSetDescriptor.getType()).thenReturn(null);
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("Type cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAliasNotA() {
		when(mockRecordSetDescriptor.getName()).thenReturn("dnsName");
		when(mockRecordSetDescriptor.getType()).thenReturn("CNAME");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(new AliasTargetDescriptor());
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("If alias target specified then recordDescriptor.type must be 'A'", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAliasAndResourceRecs() {
		when(mockRecordSetDescriptor.getName()).thenReturn("dnsName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(new AliasTargetDescriptor());
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(new ArrayList<String>());
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("If alias target specified then resourceRecords must be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAliasNoDNSName() {
		when(mockRecordSetDescriptor.getName()).thenReturn("dnsName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockAliasTargetDescriptor.getDnsName()).thenReturn(null);
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(null);
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("AliasTarget.DnsName cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAliasEvalHealthTrue() {
		when(mockRecordSetDescriptor.getName()).thenReturn("dnsName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockAliasTargetDescriptor.getDnsName()).thenReturn("targetDnsName");
		when(mockAliasTargetDescriptor.getEvaluateTargetHealth()).thenReturn(true);
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(null);
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("AliasTarget.EvaluateTargetHealth must be false", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAliasBadHostedZoneId() {
		when(mockRecordSetDescriptor.getName()).thenReturn("dnsName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockAliasTargetDescriptor.getDnsName()).thenReturn("targetDnsName");
		when(mockAliasTargetDescriptor.getEvaluateTargetHealth()).thenReturn(false);
		when(mockAliasTargetDescriptor.getHostedZoneId()).thenReturn("someRandomeZone");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(null);
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("AliasTarget.HostedZoneId must be 'Z2FDTNDATAQYW2'", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorNotAliasNoRecords() {
		when(mockRecordSetDescriptor.getName()).thenReturn("dnsName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(null);
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(null);
		// call under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dnsConfigValidator.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("If alias target not specified then resourceRecords must be specified", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorANotAlias() {
		when(mockRecordSetDescriptor.getName()).thenReturn("dnsName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(null);
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(Collections.singletonList("1.2.3.4"));
		// call under test
		dnsConfigValidator.validateRecordSetDescriptor(mockRecordSetDescriptor);
	}

}