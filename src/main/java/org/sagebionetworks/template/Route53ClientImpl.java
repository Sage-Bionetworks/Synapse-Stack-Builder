package org.sagebionetworks.template;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.dns.RecordSetDescriptor;

import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeBatch;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

import java.util.List;
import java.util.stream.Collectors;

public class Route53ClientImpl implements Route53Client {

	software.amazon.awssdk.services.route53.Route53Client r53Client;
	Configuration config;
	Logger logger;

	@Inject
	Route53ClientImpl(software.amazon.awssdk.services.route53.Route53Client r53Client, Configuration configuration, LoggerFactory loggerFactory) {
		this.r53Client = r53Client;
		this.config = configuration;
		this.logger = loggerFactory.getLogger(Route53ClientImpl.class);
	}

	@Override
	public List<ResourceRecordSet> listResourceRecordSets(String hostedZoneId) {
		ListResourceRecordSetsRequest req = ListResourceRecordSetsRequest.builder()
				.hostedZoneId(hostedZoneId)
				.maxItems("200")
				.build();
		;
		ListResourceRecordSetsResponse res = r53Client.listResourceRecordSets(req);
		return res.resourceRecordSets();
	}

	@Override
	public void changeResourceRecordSets(String hostedZoneId, List<RecordSetDescriptor> recordSetDescriptors, int batchSize) {
		List<List<RecordSetDescriptor>> batchedDescriptors = Lists.partition(recordSetDescriptors, batchSize);
		batchedDescriptors.stream().map(l -> buildChangesList(l)).forEach(cl -> submitBatch(hostedZoneId, cl));
	}

	List<Change> buildChangesList(List<RecordSetDescriptor> l) {
		List<Change> changes = l.stream()
				.map(rd -> rd.toResourceRecordSet())
				.map(rrs -> Change.builder().action(ChangeAction.UPSERT).resourceRecordSet(rrs).build())
				.collect(Collectors.toList());
		return changes;
	}

	void submitBatch(String hostedZoneId, List<Change> changes) {
		ChangeBatch batch = ChangeBatch.builder().changes(changes).build();
		ChangeResourceRecordSetsRequest request = ChangeResourceRecordSetsRequest.builder().changeBatch(batch).hostedZoneId(hostedZoneId).build();
		ChangeResourceRecordSetsResponse result = r53Client.changeResourceRecordSets(request);
	}

}
