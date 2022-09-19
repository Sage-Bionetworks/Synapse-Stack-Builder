package org.sagebionetworks.template;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.dns.AliasTargetDescriptor;
import org.sagebionetworks.template.dns.RecordSetDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Route53ClientImplTest {

	@Mock
	private AmazonRoute53 mockR53Client;

	@Mock
	private Configuration mockConfig;

	@Mock
	private  LoggerFactory mockLoggerFactory;

	@InjectMocks
	private Route53ClientImpl route53Client;

	@Mock
	private RecordSetDescriptor mockRecordSetDescriptor;
	@Mock
	private AliasTargetDescriptor mockAliasTargetDescriptor;

	@Captor
	private ArgumentCaptor<ChangeResourceRecordSetsRequest> changeResourceRecordSetsRequestArgumentCaptor;

	@Test
	void testBatchingMoreThanBatchSizeChangeResourceRecordSets() {
		when(mockAliasTargetDescriptor.getDnsName()).thenReturn("target1");
		when(mockRecordSetDescriptor.getTtl()).thenReturn("900");
		// 2 records, batches of 1
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor, mockAliasTargetDescriptor);
		List<RecordSetDescriptor> descriptors = new ArrayList<>();
		descriptors.add(mockRecordSetDescriptor);
		descriptors.add(mockRecordSetDescriptor);
		// call under test
		route53Client.changeResourceRecordSets("hostedZoneId", descriptors, 1);
		// 2 calls, 2 batches of 1 record
		verify(mockR53Client, times(2)).changeResourceRecordSets(changeResourceRecordSetsRequestArgumentCaptor.capture());
		assertEquals(2, changeResourceRecordSetsRequestArgumentCaptor.getAllValues().size());
		ChangeResourceRecordSetsRequest req1 = changeResourceRecordSetsRequestArgumentCaptor.getAllValues().get(0);
		ChangeResourceRecordSetsRequest req2 = changeResourceRecordSetsRequestArgumentCaptor.getAllValues().get(1);
		assertEquals(1, req1.getChangeBatch().getChanges().size());
		assertEquals(1, req2.getChangeBatch().getChanges().size());
		Change change1 = req1.getChangeBatch().getChanges().get(0);
		validateChange(change1);
		Change change2 = req2.getChangeBatch().getChanges().get(0);
		validateChange(change2);
	}

	private static void validateChange(Change change) {
		assertEquals(ChangeAction.UPSERT.name(), change.getAction());
		ResourceRecordSet rrs = change.getResourceRecordSet();
		assertNotNull(rrs);
		assertEquals(900, rrs.getTTL());
		assertEquals("target1", rrs.getAliasTarget().getDNSName());
	}

	@Test
	void testBatchingEqualsBatchSizeChangeResourceRecordSets() {
		when(mockAliasTargetDescriptor.getDnsName()).thenReturn("target1");
		when(mockRecordSetDescriptor.getTtl()).thenReturn("900");
		// 2 records, batches of 2
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor, mockAliasTargetDescriptor);
		List<RecordSetDescriptor> descriptors = new ArrayList<>();
		descriptors.add(mockRecordSetDescriptor);
		descriptors.add(mockRecordSetDescriptor);
		// call under test
		route53Client.changeResourceRecordSets("hostedZoneId", descriptors, 2);
		// 1 call, 1 batch of 2 records
		verify(mockR53Client, times(1)).changeResourceRecordSets(changeResourceRecordSetsRequestArgumentCaptor.capture());
		assertEquals(1, changeResourceRecordSetsRequestArgumentCaptor.getAllValues().size());
		ChangeResourceRecordSetsRequest req = changeResourceRecordSetsRequestArgumentCaptor.getValue();
		assertEquals(2, req.getChangeBatch().getChanges().size());
	}

	@Test
	void testBatchingLessThanBatchSizeChangeResourceRecordSets() {
		when(mockAliasTargetDescriptor.getDnsName()).thenReturn("target1");
		when(mockRecordSetDescriptor.getTtl()).thenReturn("900");
		// 1 record, batches of 2
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		List<RecordSetDescriptor> descriptors = new ArrayList<>();
		descriptors.add(mockRecordSetDescriptor);
		// call under test
		route53Client.changeResourceRecordSets("hostedZoneId", descriptors, 2);
		// 1 call, 1 batch of 1 record
		verify(mockR53Client, times(1)).changeResourceRecordSets(changeResourceRecordSetsRequestArgumentCaptor.capture());
		assertEquals(1, changeResourceRecordSetsRequestArgumentCaptor.getAllValues().size());
		ChangeResourceRecordSetsRequest req = changeResourceRecordSetsRequestArgumentCaptor.getValue();
		assertEquals(1, req.getChangeBatch().getChanges().size());
	}

	@Test
	void testToResourceRecordSetCNAME() {
		when(mockRecordSetDescriptor.getName()).thenReturn("someName");
		when(mockRecordSetDescriptor.getType()).thenReturn("CNAME");
		when(mockRecordSetDescriptor.getTtl()).thenReturn("900");
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(Collections.singletonList("someValue"));
		// call under test
		ResourceRecordSet rrs = Route53ClientImpl.toResourceRecordSet(mockRecordSetDescriptor);
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
	void testToResourceRecordSetCANotAlias() {
		when(mockRecordSetDescriptor.getName()).thenReturn("someName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockRecordSetDescriptor.getTtl()).thenReturn("900");
		when(mockRecordSetDescriptor.getResourceRecords()).thenReturn(Arrays.asList("1.2.3.4", "1.5.6.7"));
		// call under test
		ResourceRecordSet rrs = Route53ClientImpl.toResourceRecordSet(mockRecordSetDescriptor);
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
		when(mockRecordSetDescriptor.getName()).thenReturn("someName");
		when(mockRecordSetDescriptor.getType()).thenReturn("A");
		when(mockAliasTargetDescriptor.getDnsName()).thenReturn("someDnsTarget");
		when(mockAliasTargetDescriptor.getEvaluateTargetHealth()).thenReturn(false);
		when(mockAliasTargetDescriptor.getHostedZoneId()).thenReturn("targetZoneId");
		when(mockRecordSetDescriptor.getAliasTargetDescriptor()).thenReturn(mockAliasTargetDescriptor);
		// call under test
		ResourceRecordSet rrs = Route53ClientImpl.toResourceRecordSet(mockRecordSetDescriptor);
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