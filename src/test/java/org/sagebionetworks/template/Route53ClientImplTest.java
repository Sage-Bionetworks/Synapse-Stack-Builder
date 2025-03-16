package org.sagebionetworks.template;

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
import software.amazon.awssdk.services.route53.model.AliasTarget;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

import java.util.ArrayList;
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
	private software.amazon.awssdk.services.route53.Route53Client mockR53Client;

	@Mock
	private  LoggerFactory mockLoggerFactory;

	@Mock
	private Configuration mockConfig;

	@InjectMocks
	private Route53ClientImpl route53Client;

	@Mock
	private RecordSetDescriptor mockRecordSetDescriptor;
	@Mock
	private AliasTargetDescriptor mockAliasTargetDescriptor;

	@Mock
	private ResourceRecordSet mockResourceRecordSet;
	@Mock
	private AliasTarget mockAliasTarget;


	@Captor
	private ArgumentCaptor<ChangeResourceRecordSetsRequest> changeResourceRecordSetsRequestArgumentCaptor;

	@BeforeEach
	void setup() {
	}

	@Test
	void testChangeResourceRecordSets() {
		RecordSetDescriptor descriptor = new RecordSetDescriptor("name", "CNAME", "600", Collections.singletonList("targetName"), null);
		List<RecordSetDescriptor> descriptors = Collections.singletonList(descriptor);
		// call under test
		route53Client.changeResourceRecordSets("hostedZoneId", descriptors, 1);
		verify(mockR53Client, times(1)).changeResourceRecordSets(changeResourceRecordSetsRequestArgumentCaptor.capture());
		assertEquals(1, changeResourceRecordSetsRequestArgumentCaptor.getAllValues().size());
		ChangeResourceRecordSetsRequest req = changeResourceRecordSetsRequestArgumentCaptor.getAllValues().get(0);
		assertEquals(1, req.changeBatch().changes().size());
		Change change = req.changeBatch().changes().get(0);
		assertEquals(ChangeAction.UPSERT, change.action());
		ResourceRecordSet rrs = change.resourceRecordSet();
		assertNotNull(rrs);
		assertEquals("targetName", rrs.resourceRecords().get(0).value());
	}
	@Test
	void testBatchingMoreThanBatchSizeChangeResourceRecordSets() {
		when(mockAliasTarget.dnsName()).thenReturn("target1");
		when(mockResourceRecordSet.aliasTarget()).thenReturn(mockAliasTarget);
		when(mockRecordSetDescriptor.toResourceRecordSet()).thenReturn(mockResourceRecordSet);
		// 2 records, batches of 1
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
		assertEquals(1, req1.changeBatch().changes().size());
		assertEquals(1, req2.changeBatch().changes().size());
		Change change1 = req1.changeBatch().changes().get(0);
		validateChange(change1);
		Change change2 = req2.changeBatch().changes().get(0);
		validateChange(change2);
	}

	private static void validateChange(Change change) {
		assertEquals(ChangeAction.UPSERT, change.action());
		ResourceRecordSet rrs = change.resourceRecordSet();
		assertNotNull(rrs);
		assertEquals("target1", rrs.aliasTarget().dnsName());
	}

	@Test
	void testBatchingEqualsBatchSizeChangeResourceRecordSets() {
		when(mockRecordSetDescriptor.toResourceRecordSet()).thenReturn(mockResourceRecordSet);
		// 2 records, batches of 2
		List<RecordSetDescriptor> descriptors = new ArrayList<>();
		descriptors.add(mockRecordSetDescriptor);
		descriptors.add(mockRecordSetDescriptor);
		// call under test
		route53Client.changeResourceRecordSets("hostedZoneId", descriptors, 2);
		// 1 call, 1 batch of 2 records
		verify(mockR53Client, times(1)).changeResourceRecordSets(changeResourceRecordSetsRequestArgumentCaptor.capture());
		assertEquals(1, changeResourceRecordSetsRequestArgumentCaptor.getAllValues().size());
		ChangeResourceRecordSetsRequest req = changeResourceRecordSetsRequestArgumentCaptor.getValue();
		assertEquals(2, req.changeBatch().changes().size());
	}

	@Test
	void testBatchingLessThanBatchSizeChangeResourceRecordSets() {
		// 1 record, batches of 2
		List<RecordSetDescriptor> descriptors = new ArrayList<>();
		descriptors.add(mockRecordSetDescriptor);
		// call under test
		route53Client.changeResourceRecordSets("hostedZoneId", descriptors, 2);
		// 1 call, 1 batch of 1 record
		verify(mockR53Client, times(1)).changeResourceRecordSets(changeResourceRecordSetsRequestArgumentCaptor.capture());
		assertEquals(1, changeResourceRecordSetsRequestArgumentCaptor.getAllValues().size());
		ChangeResourceRecordSetsRequest req = changeResourceRecordSetsRequestArgumentCaptor.getValue();
		assertEquals(1, req.changeBatch().changes().size());
	}

}