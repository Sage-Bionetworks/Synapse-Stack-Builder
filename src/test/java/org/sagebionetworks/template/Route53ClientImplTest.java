package org.sagebionetworks.template;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.model.AliasTarget;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ResourceRecordSet;
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
		assertEquals(1, req.getChangeBatch().getChanges().size());
		Change change = req.getChangeBatch().getChanges().get(0);
		assertEquals(ChangeAction.UPSERT.name(), change.getAction());
		ResourceRecordSet rrs = change.getResourceRecordSet();
		assertNotNull(rrs);
		assertEquals("targetName", rrs.getResourceRecords().get(0).getValue());
	}
	@Test
	void testBatchingMoreThanBatchSizeChangeResourceRecordSets() {
		when(mockAliasTarget.getDNSName()).thenReturn("target1");
		when(mockResourceRecordSet.getAliasTarget()).thenReturn(mockAliasTarget);
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
		assertEquals("target1", rrs.getAliasTarget().getDNSName());
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
		assertEquals(2, req.getChangeBatch().getChanges().size());
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
		assertEquals(1, req.getChangeBatch().getChanges().size());
	}

}