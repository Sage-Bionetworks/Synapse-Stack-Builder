package org.sagebionetworks.template.dns;

import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.google.inject.Inject;
import org.sagebionetworks.template.Route53Client;
import org.sagebionetworks.template.TemplateUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DnsBuilderImpl implements DnsBuilder {

	public static final int BATCH_SIZE = 10;

	@Inject
	Route53Client route53Client;

	@Override
	public void buildDns(DnsConfig dnsConfig) {
		String hostedZoneId = dnsConfig.getHostedZoneId();
		route53Client.changeResourceRecordSets(hostedZoneId, dnsConfig.getRecordSetDescriptorList(), BATCH_SIZE);
	}

	@Override
	public void listDns(String hostedZoneId) throws IOException {
		List<RecordSetDescriptor> recordSetDescriptors = new ArrayList<>();
		List<ResourceRecordSet> resourceRecordSets = route53Client.listResourceRecordSets(hostedZoneId);
		for (ResourceRecordSet rrs: resourceRecordSets) {
			if (! Arrays.asList("A", "CNAME").contains(rrs.getType())) {
				continue;
			}
			RecordSetDescriptor descriptor = new RecordSetDescriptor(rrs);
			recordSetDescriptors.add(descriptor);
		}
		DnsConfig dnsConfig = new DnsConfig(hostedZoneId, recordSetDescriptors);
		System.out.println(TemplateUtils.prettyPrint(dnsConfig));
	}

}
