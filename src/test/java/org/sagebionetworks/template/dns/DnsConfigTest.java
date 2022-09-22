package org.sagebionetworks.template.dns;

import com.amazonaws.services.route53.model.ResourceRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DnsConfigTest {

	@Mock
	private RecordSetDescriptor mockRecordSetDescriptor;

	@Mock
	private AliasTargetDescriptor mockAliasTargetDescriptor;

	@BeforeEach
	void setUp() {
	}

	@Test
	void testValidateRecordSetDescriptorNullName() {
		when(mockRecordSetDescriptor.getName()).thenReturn(null);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfig.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("Name cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorNullType() {
		when(mockRecordSetDescriptor.getName()).thenReturn("name");
		when(mockRecordSetDescriptor.getType()).thenReturn(null);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfig.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("Type cannot be null", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAliasNotA() {
		when(mockRecordSetDescriptor.getName()).thenReturn("name");
		when(mockRecordSetDescriptor.getType()).thenReturn("CNAME");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfig.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("If alias target specified then recordDescriptor.type must be 'A'", thrown.getMessage());
	}

	@Test
	void testValidateRecordSetDescriptorAliasAResRec() {
		when(mockRecordSetDescriptor.getName()).thenReturn("name");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(Collections.singletonList("aResRecord"));
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfig.validateRecordSetDescriptor(mockRecordSetDescriptor);
		});
		assertEquals("If alias target specified then resourceRecords must be null", thrown.getMessage());
	}

	@Test
	void testSetHostedZoneIdNull() {
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfig dnsConfig = new DnsConfig();
			dnsConfig.setHostedZoneId(null);
		});
	}

	@Test
	void testSetRecordSetDescriptorListNull() {
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfig dnsConfig = new DnsConfig();
			dnsConfig.setRecordSetDescriptorList(null);
		});
		assertEquals("RecordSetDescriptorList cannot be null", thrown.getMessage());
	}

	@Test
	void testSetRecordSetDescriptorListEmpty() {
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsConfig dnsConfig = new DnsConfig();
			dnsConfig.setRecordSetDescriptorList(new ArrayList<RecordSetDescriptor>());
		});
		assertEquals("RecordSetDescriptorList must contain at least one element", thrown.getMessage());
	}
}