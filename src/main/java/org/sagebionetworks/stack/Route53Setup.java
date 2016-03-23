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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 *
 * @author xschildw
 */
public class Route53Setup implements ResourceProcessor {
	
	private static Logger log = Logger.getLogger(Route53Setup.class);
	
	public AmazonRoute53Client route53Client;
	private InputConfiguration config;
	private GeneratedResources resources;
	
	private HostedZone hostedZone;

	public Route53Setup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		initialize(factory, config, resources);
	}
	
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		if(factory == null) throw new IllegalArgumentException("AWSClientFactory cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");

		this.route53Client = factory.createRoute53Client();
		this.config = config;
		this.resources = resources;
		
		this.hostedZone = getHostedZone(config.getStackSubdomain());
	}

	public void describeResources() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	
	public void setupResources() throws InterruptedException {
		
		List<Change> changes = buildChangesList(Constants.ROUTE53_PREFIXES);
		if (changes.size() > 0) {
			ChangeBatch changeBatch = new ChangeBatch().withChanges(changes);

			ChangeResourceRecordSetsRequest cReq = new ChangeResourceRecordSetsRequest().withHostedZoneId(hostedZone.getId()).withChangeBatch(changeBatch);
			ChangeResourceRecordSetsResult cRes = route53Client.changeResourceRecordSets(cReq);
			GetChangeRequest gcReq = new GetChangeRequest(cRes.getChangeInfo().getId());
			GetChangeResult gcRes = route53Client.getChange(gcReq);
			// TODO: No real need to wait here, could just exit
			while (! "INSYNC".equals(gcRes.getChangeInfo().getStatus())) {
				Thread.sleep(1000L);
				gcRes =route53Client.getChange(gcReq);
				String s = gcRes.getChangeInfo().getStatus();
			}
		}
	}

	public void teardownResources() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	public HostedZone getHostedZone(String hostedZoneDomainName) {
		HostedZone zone = null;
		if (! hostedZoneDomainName.endsWith(".")) {
			hostedZoneDomainName = hostedZoneDomainName + ".";
		}
		
		ListHostedZonesResult res = route53Client.listHostedZones();
		List<HostedZone> l = res.getHostedZones();
		// Should only be one hz in our case, no need to handle getIstruncated() etc.
		for (HostedZone hz: l) {
			if (hz.getName().equals(hostedZoneDomainName)) {
				zone = hz;
				break;
			}
		}
		if (zone == null) {
			throw new IllegalArgumentException("Hosted zone for domain " + hostedZoneDomainName + " could not be found.");
		}
		return zone;
	}
	
	// TODO:	See if this could be done with a single call to listResourceRecordSets() to return all the CNAMEs,
	//			pass in a list of recordNames and getting back a list of ResourceRecordSets
	ResourceRecordSet getResourceRecordSetForRecordName(String recordName) {
		ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest();
		req.setHostedZoneId(hostedZone.getId());
		req.setStartRecordType(RRType.CNAME);
		req.setStartRecordName(recordName);
		req.setMaxItems("1");
		ListResourceRecordSetsResult res = route53Client.listResourceRecordSets(req);
		ResourceRecordSet rrs = null;
		if ((res.getResourceRecordSets().size() > 0) && (recordName.equals(res.getResourceRecordSets().get(0).getName()))) {
			rrs = res.getResourceRecordSets().get(0);
		}
		return rrs;
	}
	
	List<Change> buildChangesList(List<String> prefixes) {
		List<Change> changes = new ArrayList<Change>();
		
		for (String prefix: prefixes) {
			String rName = config.getEnvironmentSubdomainCNAME(prefix);
			String rValue = config.getEnvironmentCNAMEPrefix(prefix) + ".us-east-1.elasticbeanstalk.com";
			ResourceRecordSet rrs = getResourceRecordSetForRecordName(rName);
			if (rrs == null) {
				ResourceRecord rr = new ResourceRecord().withValue(rValue);
				rrs = new ResourceRecordSet().withType(RRType.CNAME).withName(rName).withTTL(300L).withResourceRecords(rr);
				Change change = new Change().withAction(ChangeAction.CREATE).withResourceRecordSet(rrs);
				changes.add(change);
			}
		}
		return changes;
		
	}

}
