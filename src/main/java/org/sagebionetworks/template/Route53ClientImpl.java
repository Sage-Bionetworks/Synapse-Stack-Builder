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
		ListIterator<RecordSetDescriptor> it = recordSetDescriptors.listIterator();
		List<Change> changes = new ArrayList<>();
		int count = batchSize;
		while (it.hasNext()) {
			ResourceRecordSet resourceRecordSet = toResourceRecordSet(it.next());
			Change change = new Change(ChangeAction.UPSERT, resourceRecordSet);
			changes.add(change);
			count -= 1;
			if (count == 0) { // We have a batch
				submitBatch(hostedZoneId, changes);
				if (it.hasNext()) {
					count = batchSize;
					changes = new ArrayList<>();
				}
			}
		}
		// Last batch
		if (count > 0) {
			submitBatch(hostedZoneId, changes);
		}
	}

	void submitBatch(String hostedZoneId, List<Change> changes) {
		ChangeBatch batch = new ChangeBatch(changes);
		ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest();
		request.setChangeBatch(batch);
		request.setHostedZoneId(hostedZoneId);
		ChangeResourceRecordSetsResult result = r53Client.changeResourceRecordSets(request);
	}

	public static ResourceRecordSet toResourceRecordSet(RecordSetDescriptor descriptor) {
		ResourceRecordSet resourceRecordSet = new ResourceRecordSet();
		resourceRecordSet.setName(descriptor.getName());
		resourceRecordSet.setType(descriptor.getType());
		resourceRecordSet.setTTL(Long.parseLong(descriptor.getTtl()));
		if (descriptor.getResourceRecords() != null && descriptor.getResourceRecords().size() > 0) {
			List<ResourceRecord> records = new ArrayList<>();
			for (String s: descriptor.getResourceRecords()) {
				ResourceRecord rec = new ResourceRecord().withValue(s);
				records.add(rec);
			}
			resourceRecordSet.setResourceRecords(records);
		}
		if (descriptor.getAliasTargetDescriptor() != null) {
			AliasTargetDescriptor desc = descriptor.getAliasTargetDescriptor();
			AliasTarget aliasTarget = new AliasTarget().withDNSName(desc.getDnsName()).withHostedZoneId(desc.getHostedZoneId()).withEvaluateTargetHealth(desc.getEvaluateTargetHealth());
			resourceRecordSet.setAliasTarget(aliasTarget);
		}
		return resourceRecordSet;
	}

	public static RecordSetDescriptor recordSetDescriptor(ResourceRecordSet resourceRecordSet) {
		RecordSetDescriptor descriptor = new RecordSetDescriptor();
		descriptor.setName(resourceRecordSet.getName());
		descriptor.setType(resourceRecordSet.getType());
		descriptor.setTtl(resourceRecordSet.getTTL().toString());
		if (resourceRecordSet.getResourceRecords().size() > 0) {
			descriptor.setResourceRecords(resourceRecordSet.getResourceRecords().stream().map(r -> r.getValue()).collect(Collectors.toList()));
		}
		if (resourceRecordSet.getAliasTarget() != null) {
			descriptor.setAliasTargetDescriptor(new AliasTargetDescriptor(resourceRecordSet.getAliasTarget()));
		}
		return descriptor;
	}

}
