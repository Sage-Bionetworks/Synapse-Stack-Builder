package org.sagebionetworks.template.dns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DnsConfigBuilderTest {

	@Mock
	private AliasTargetDescriptor mockAliasTargetDescriptor;
	@Mock
	private RecordSetDescriptor mockRecordSetDescriptor;

	@Test
	void testValidateRecordSetDescriptorNullName() {
		when(mockRecordSetDescriptor.getName()).thenReturn(null);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfigBuilder.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("RecordSetDescriptor.name cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorNullType() {
		when(mockRecordSetDescriptor.getName()).thenReturn("recordName");
		when(mockRecordSetDescriptor.getType()).thenReturn(null);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfigBuilder.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("RecordSetDescriptor.type cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAliasNotA() {
		when(mockRecordSetDescriptor.getName()).thenReturn("recordName");
		when(mockRecordSetDescriptor.getType()).thenReturn("CNAME");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfigBuilder.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("If alias target specified then RecordDescriptor.type must be 'A'", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAliasDescAndResourceRecs() {
		when(mockRecordSetDescriptor.getName()).thenReturn("recordName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(new ArrayList<>());
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfigBuilder.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("If alias target specified then RecordDescriptor.resourceRecords must be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAInvalidAliasDescriptor() {
		when(mockRecordSetDescriptor.getName()).thenReturn("recordName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(null);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfigBuilder.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("AliasTargetDescriptor.dnsName cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorNoANoResRecs() {
		when(mockRecordSetDescriptor.getName()).thenReturn("recordName");
		when(mockRecordSetDescriptor.getType()).thenReturn("CNAME");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(null);
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(null);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfigBuilder.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("If alias target not specified then RecordDescriptor.resourceRecords must be specified", thrown.getMessage());
	}

	@Test
	void testValidateAliasTargetDescriptorNullDnsName() {
		when(mockAliasTargetDescriptor.getDnsName()).thenReturn(null);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfigBuilder.validateAliasTargetDescriptor(mockAliasTargetDescriptor);
		});
		assertEquals("AliasTargetDescriptor.dnsName cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateAliasTargetDescriptorNullHostedZoneId() {
		when(mockAliasTargetDescriptor.getDnsName()).thenReturn("dnsName");
		when(mockAliasTargetDescriptor.getHostedZoneId()).thenReturn(null);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfigBuilder.validateAliasTargetDescriptor(mockAliasTargetDescriptor);
		});
		assertEquals("AliasTargetDescriptor.hostedZoneId cannot be null", thrown.getMessage());
	}

	@Test
	void testBuildNullHostedZoneId() {
		DnsConfigBuilder builder = new DnsConfigBuilder();
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			builder.build();
		});
		assertEquals("DnsConfigBuilder.hostedZoneId cannot be null", thrown.getMessage());
	}

	@Test
	void testBuildNullRecordSetDescriptorList() {
		DnsConfigBuilder builder = new DnsConfigBuilder().hostedZoneId("hostedZoneId");
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			builder.build();
		});
		assertEquals("DnsConfigBuilder.recordSetDescriptorList must have at least one element", thrown.getMessage());
	}

	@Test
	void testBuildEmptyRecordSetDescriptorList() {
		DnsConfigBuilder builder = new DnsConfigBuilder().hostedZoneId("hostedZoneId").recordSetDescriptorList(new ArrayList<>());
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			builder.build();
		});
		assertEquals("DnsConfigBuilder.recordSetDescriptorList must have at least one element", thrown.getMessage());
	}

	@Test
	void testBuildInvalidRecordSetDescriptorList() {
		DnsConfigBuilder builder = new DnsConfigBuilder().hostedZoneId("hostedZoneId").recordSetDescriptorList(Collections.singletonList(mockRecordSetDescriptor));
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			builder.build();
		});
		assertEquals("RecordSetDescriptor.name cannot be null", thrown.getMessage());
	}

	@Test
	void testBuild() {
		when(mockRecordSetDescriptor.getName()).thenReturn("recordName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(Collections.singletonList("1.2.3.4"));
		DnsConfigBuilder builder = new DnsConfigBuilder().hostedZoneId("hostedZoneId").recordSetDescriptorList(Collections.singletonList(mockRecordSetDescriptor));
		// call under test
		DnsConfig dnsConfig = builder.build();
		assertNotNull(dnsConfig);
		assertEquals("hostedZoneId", dnsConfig.getHostedZoneId());
		assertNotNull(dnsConfig.getRecordSetDescriptorList());
		assertEquals(1, dnsConfig.getRecordSetDescriptorList().size());
		RecordSetDescriptor desc = dnsConfig.getRecordSetDescriptorList().get(0);
		assertEquals("recordName", desc.getName());
		assertEquals("A", desc.getType());
		assertNotNull(desc.getResourceRecords());
		assertEquals(1, desc.getResourceRecords().size());
		assertEquals("1.2.3.4", desc.getResourceRecords().get(0));
	}
}