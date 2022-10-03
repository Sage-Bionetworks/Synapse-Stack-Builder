package org.sagebionetworks.template;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.model.AliasTarget;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.dns.AliasTargetDescriptor;
import org.sagebionetworks.template.dns.RecordSetDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class Route53ClientImpl implements Route53Client {

	AmazonRoute53 r53Client;
	Configuration config;
	Logger logger;

	@Inject
	Route53ClientImpl(AmazonRoute53 r53Client, Configuration configuration, LoggerFactory loggerFactory) {
		this.r53Client = r53Client;
		this.config = configuration;
		this.logger = loggerFactory.getLogger(Route53ClientImpl.class);
	}

	@Override
	public List<ResourceRecordSet> listResourceRecordSets(String hostedZoneId) {
		ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest().withHostedZoneId(hostedZoneId).withMaxItems("200");
		ListResourceRecordSetsResult res = r53Client.listResourceRecordSets(req);
		return res.getResourceRecordSets();
	}

	@Override
	public void changeResourceRecordSets(String hostedZoneId, List<RecordSetDescriptor> recordSetDescriptors, int batchSize) {
		List<List<RecordSetDescriptor>> batchedDescriptors = Lists.partition(recordSetDescriptors, batchSize);
		batchedDescriptors.stream().map(l -> buildChangesList(l)).forEach(cl -> submitBatch(hostedZoneId, cl));
	}

	List<Change> buildChangesList(List<RecordSetDescriptor> l) {
		List<Change> changes = l.stream().map(rd -> rd.toResourceRecordSet())
				.map(rrs -> new Change(ChangeAction.UPSERT, rrs))
				.collect(Collectors.toList());
		return changes;
	}

	void submitBatch(String hostedZoneId, List<Change> changes) {
		ChangeBatch batch = new ChangeBatch(changes);
		ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest();
		request.setChangeBatch(batch);
		request.setHostedZoneId(hostedZoneId);
		ChangeResourceRecordSetsResult result = r53Client.changeResourceRecordSets(request);
	}

}
