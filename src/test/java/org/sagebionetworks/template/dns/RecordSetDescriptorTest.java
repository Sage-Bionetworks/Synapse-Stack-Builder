package org.sagebionetworks.template.dns;

import com.amazonaws.services.route53.model.ResourceRecordSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RecordSetDescriptorTest {

	private RecordSetDescriptor recordSetDescriptor;
	private AliasTargetDescriptor aliasTargetDescriptor;

	@BeforeEach
	void setupAll() {
		aliasTargetDescriptor = new AliasTargetDescriptor();
		recordSetDescriptor = new RecordSetDescriptor();
	}

	@Test
	void testToResourceRecordSetCNAME() {
		recordSetDescriptor.setName("someName");
		recordSetDescriptor.setType("CNAME");
		recordSetDescriptor.setTTL("900");
		recordSetDescriptor.setResourceRecords(Collections.singletonList("someValue"));
		// call under test
		ResourceRecordSet rrs = recordSetDescriptor.toResourceRecordSet();
		assertNotNull(rrs);
		assertEquals("someName", rrs.getName());
		assertEquals("CNAME", rrs.getType());
		assertEquals(900, rrs.getTTL());
		assertNull(rrs.getAliasTarget());
		assertNotNull(rrs.getResourceRecords());
		assertEquals(1, rrs.getResourceRecords().size());
		assertEquals("someValue", rrs.getResourceRecords().get(0).getValue());
	}

	@Test
	void testToResourceRecordSetANotAlias() {
		recordSetDescriptor.setName("someName");
		recordSetDescriptor.setType("A");
		recordSetDescriptor.setTTL("900");
		recordSetDescriptor.setResourceRecords(Arrays.asList("1.2.3.4", "1.5.6.7"));
		// call under test
		ResourceRecordSet rrs = recordSetDescriptor.toResourceRecordSet();
		assertNotNull(rrs);
		assertEquals("someName", rrs.getName());
		assertEquals("A", rrs.getType());
		assertEquals(900, rrs.getTTL());
		assertNull(rrs.getAliasTarget());
		assertNotNull(rrs.getResourceRecords());
		assertEquals(2, rrs.getResourceRecords().size());
		assertEquals("1.2.3.4", rrs.getResourceRecords().get(0).getValue());
		assertEquals("1.5.6.7", rrs.getResourceRecords().get(1).getValue());
	}

	@Test
	void testToResourceRecordSetCAAlias() {
		recordSetDescriptor.setName("someName");
		recordSetDescriptor.setType("A");
		aliasTargetDescriptor.setDnsName("someDnsTarget");
		aliasTargetDescriptor.setEvaluateTargetHealth(false);
		aliasTargetDescriptor.setHostedZoneId("targetZoneId");
		recordSetDescriptor.setAliasTargetDescriptor(aliasTargetDescriptor);
		// call under test
		ResourceRecordSet rrs = recordSetDescriptor.toResourceRecordSet();
		assertNotNull(rrs);
		assertEquals("someName", rrs.getName());
		assertEquals("A", rrs.getType());
		assertNull(rrs.getTTL());
		assertEquals(0, rrs.getResourceRecords().size());
		assertNotNull(rrs.getAliasTarget());
		assertEquals("someDnsTarget", rrs.getAliasTarget().getDNSName());
		assertFalse(rrs.getAliasTarget().getEvaluateTargetHealth());
		assertEquals("targetZoneId", rrs.getAliasTarget().getHostedZoneId());
	}

}