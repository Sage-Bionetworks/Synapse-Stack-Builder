package org.sagebionetworks.template.dns;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.Route53Client;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DnsBuilderImplTest {

	@Mock
	private DnsConfig mockConfig;

	@Mock
	private Route53Client mockRoute53Client;

	@InjectMocks
	DnsBuilderImpl dnsBuilder;

	@Mock
	private List<RecordSetDescriptor> mockRecordSetDescriptors;

	@Test
	void testBuildDns() {
		when(mockConfig.getHostedZoneId()).thenReturn("hostedZoneId");
		when(mockConfig.getRecordSetDescriptorList()).thenReturn(mockRecordSetDescriptors);
		// call under test
		dnsBuilder.buildDns();
		verify(mockRoute53Client).changeResourceRecordSets(eq("hostedZoneId"), eq(mockRecordSetDescriptors), eq(10));
	}

}