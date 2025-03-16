package org.sagebionetworks.template;

import org.sagebionetworks.template.dns.RecordSetDescriptor;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

import java.util.List;

public interface Route53Client {

	List<ResourceRecordSet> listResourceRecordSets(String hostedZoneId);
	void changeResourceRecordSets(String hostedZoneId, List<RecordSetDescriptor> recordSetDescriptors, int batchSize);

}
