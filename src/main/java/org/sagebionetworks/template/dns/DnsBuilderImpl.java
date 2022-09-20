package org.sagebionetworks.template.dns;

import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.sagebionetworks.template.Route53Client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.sagebionetworks.template.Constants.ROUTE53_DNS_CONFIG_FILE;

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
	public void listDns(String hostedZoneId) {
		List<ResourceRecordSet> resourceRecordSets = route53Client.listResourceRecordSets(hostedZoneId);
		System.out.println(resourceRecordSets);
	}

}
