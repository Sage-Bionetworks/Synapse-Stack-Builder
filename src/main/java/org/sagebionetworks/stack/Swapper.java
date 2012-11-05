package org.sagebionetworks.stack;

import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.GetChangeRequest;
import com.amazonaws.services.route53.model.GetChangeResult;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 *
 * @author xschildw
 */
public class Swapper {
	private AmazonRoute53Client client;
	private InputConfiguration config;
	private String stack, srcStackInstance, destStackInstance;
	private HostedZone hostedZone;

	public Swapper(AmazonClientFactory factory, Properties props, String stack, String srcStackInstance, String destStackInstance) throws IOException {
		config = new InputConfiguration(props);
		factory.setCredentials(config.getAWSCredentials());
		
		this.client = factory.createRoute53Client();
		this.stack = stack;
		this.srcStackInstance = srcStackInstance;
		this.destStackInstance = destStackInstance;
	}
	
	public void swapStack() throws IOException {
		List<String> svcPrefixes = Arrays.asList(Constants.PREFIX_AUTH, Constants.PREFIX_PORTAL, Constants.PREFIX_REPO, Constants.PREFIX_SEARCH);
		String r53SubdomainName;
		
		// Assume single hosted zone for now
		ListHostedZonesResult res = client.listHostedZones();
		hostedZone = res.getHostedZones().get(0);
		r53SubdomainName = hostedZone.getName();
		
		// For each service, append append delete/create to list of changes
		List<Change> swapChanges = new ArrayList<Change>();
		for (String svcPrefix: svcPrefixes) {
			List<Change> lc = createChangesForSvc(svcPrefix);
			swapChanges.addAll(lc);
		}
		
		// Apply changes
		String swapComment = "StackSwapper - swapping from" + stack + ":" + srcStackInstance + " to " + stack + ":" + destStackInstance;
		ChangeBatch batch = new ChangeBatch().withChanges(swapChanges).withComment(swapComment);
		ChangeResourceRecordSetsRequest req = new ChangeResourceRecordSetsRequest().withChangeBatch(batch).withHostedZoneId(hostedZone.getId());
		ChangeResourceRecordSetsResult cRes = client.changeResourceRecordSets(req);
		GetChangeRequest gcReq = new GetChangeRequest(cRes.getChangeInfo().getId());
		GetChangeResult gcRes = client.getChange(gcReq);
		// TODO: No real need to wait here, could just exit
		while (! "INSYNC".equals(gcRes.getChangeInfo().getStatus())) {
			try {
				Thread.sleep(1000L);
				gcRes = client.getChange(gcReq);
				String s = gcRes.getChangeInfo().getStatus();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private List<Change> createChangesForSvc(String svcPrefix) {
		HostedZone hz;
		List<Change> l = new ArrayList<Change>();
		String srcSvcGenericCNAME = svcPrefix + "." + stack + "." + hostedZone.getName();
		String srcSvcCNAME = svcPrefix + "." + stack + "." + srcStackInstance + "." + hostedZone.getName();
		String destSvcGenericCNAME = svcPrefix + "." + stack + "." + hostedZone.getName();
		String destSvcCNAME = svcPrefix + "." + stack + "." + destStackInstance + "." + hostedZone.getName();

		// Change  srcSvcGenericCNAME record to point to destSvcCNAME
		ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest();
		req.setHostedZoneId(hostedZone.getId());
		req.setStartRecordType(RRType.CNAME);
		req.setStartRecordName(srcSvcGenericCNAME);
		req.setMaxItems("1");
		ListResourceRecordSetsResult lrRes = client.listResourceRecordSets(req);
		ResourceRecordSet rrs = null;
		if ((lrRes.getResourceRecordSets().size() > 0) && (srcSvcCNAME.equals(lrRes.getResourceRecordSets().get(0).getName()))) {
			rrs = lrRes.getResourceRecordSets().get(0);
		}
		if (rrs != null) { // Generate a delete and a create request
			List<Change> lc = createChangeList(hostedZone, rrs, destSvcCNAME);
		}
		return l;
	}
	
	private List<Change> createChangeList(HostedZone hz, ResourceRecordSet rrs, String newValue) {
		List<Change> changeList = new ArrayList<Change>();
		Change c;
		
		c = new Change().withAction(ChangeAction.DELETE).withResourceRecordSet(rrs);
		changeList.add(c);
		ResourceRecord rr = new ResourceRecord().withValue(newValue);
		ResourceRecordSet newRrs = new ResourceRecordSet().withName(rrs.getName()).withType(RRType.CNAME).withTTL(300L).withResourceRecords(rr);
		c = new Change().withAction(ChangeAction.CREATE).withResourceRecordSet(newRrs);
		changeList.add(c);
		
		return changeList;
	}
	
}
