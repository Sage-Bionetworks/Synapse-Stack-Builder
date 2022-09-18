package org.sagebionetworks.template;

import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import org.sagebionetworks.template.dns.RecordSetDescriptor;

import java.util.List;

public interface Route53Client {

	List<ResourceRecordSet> listResourceRecordSets(String hostedZoneId);
	void changeResourceRecordSets(String hostedZoneId, List<RecordSetDescriptor> recordSetDescriptors, int batchSize);

}
