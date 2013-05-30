package org.sagebionetworks.stack;

import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.GetChangeRequest;
import com.amazonaws.services.route53.model.GetChangeResult;
import com.amazonaws.services.route53.model.GetHostedZoneRequest;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 *
 * @author xschildw
 */
public class Activator {
	private AmazonRoute53Client client;
	private InputConfiguration config;
	private String stackInstance, instanceRole;
	private HostedZone backEndHostedZone, portalHostedZone;

	public Activator(AmazonClientFactory factory, Properties props, String stackInstance, String instanceRole) throws IOException {
		config = new InputConfiguration(props);
		factory.setCredentials(config.getAWSCredentials());
		
		this.client = factory.createRoute53Client();
		this.stackInstance = stackInstance;
		this.instanceRole = instanceRole;

	}
	
	/*
	 *	For each service except portal, we should have a CNAME <svcPrefix>-<instanceRole>.<stackSubdomain>.sagebase.org pointing to <svcPrefix>-<instanceRole>-<stackInstance>.<stackSubdomain>.sagebase.org
	 *	For portal, we should have a CNAME synapse[-staging].<stackSubdomain>.sagebase.org pointing to <svcPrefix>-<instanceRole>-<stackInstance>.<stackSubdomain>.synapse.org
	 * 
	 *	ActivateStack changes these CNAMEs to point to a new stack instance
	 */
	public void activateStack() throws IOException {
		backEndHostedZone = getHostedZoneByName("sagebase.org");
		portalHostedZone = getHostedZoneByName("synapse.org");
		if ((null == backEndHostedZone) || (null == portalHostedZone)) {
			throw new IllegalStateException("Invalid hosted zones setup!");
		}
		
		// BackEnd services
		List<String> backEndSvcPrefixes = Arrays.asList(Constants.PREFIX_REPO, Constants.PREFIX_WORKERS);
		String r53SubdomainName = backEndHostedZone.getName();
		Map<String, String> genericToInstanceCNAMEMap = new HashMap<String, String>();
		for (String backEndSvcPrefix :backEndSvcPrefixes) {
			genericToInstanceCNAMEMap.put(getBackEndGenericCNAME(backEndSvcPrefix, instanceRole, backEndHostedZone.getName()), getBackEndInstanceCNAME(backEndSvcPrefix, instanceRole, stackInstance, backEndHostedZone.getName()));
		}
		
		// For each map entry, append append delete/create to list of changes
		List<Change> changes = new ArrayList<Change>();
		for (String k :genericToInstanceCNAMEMap.keySet()) {
			List<Change> lc = createChangesForCNAME(k, genericToInstanceCNAMEMap.get(k));
			changes.addAll(lc);
		}
		
		// Apply changes
		applyChanges(changes);
		
		// Portal
		String r53DomainName = portalHostedZone.getName();
		if ("staging".equals(instanceRole)) {
			// Point CNAME to new target
			List<Change> portalChanges = createChangesForCNAME(getPortalGenericCNAME(), getPortalInstanceCNAME(instanceRole, stackInstance, backEndHostedZone.getName()));
			applyChanges(portalChanges);
		} else {
			// Point the A-record for synapse.org to the elb of the target portal
		}
	}
	
	public List<Change> createChangesForCNAME(String cName, String newTarget) {
		List<Change> lc = new ArrayList<Change>();
		// Change  srcSvcGenericCNAME record to point to destSvcCNAME
		ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest();
		req.setHostedZoneId(backEndHostedZone.getId());
		req.setStartRecordType(RRType.CNAME);
		req.setStartRecordName(cName);
		req.setMaxItems("1");
		ListResourceRecordSetsResult lrRes = client.listResourceRecordSets(req);
		ResourceRecordSet rrs = null;
		if ((lrRes.getResourceRecordSets().size() > 0) && (cName.equals(lrRes.getResourceRecordSets().get(0).getName()))) {
			rrs = lrRes.getResourceRecordSets().get(0);
		}
		if (rrs != null) { // Generate a delete and a create request
			lc = createChangeList(backEndHostedZone, rrs, newTarget);
		}
		return lc;
	}
	
	public List<Change> createChangeList(HostedZone hz, ResourceRecordSet rrs, String newValue) {
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
	
	public void applyChanges(List<Change> changes) {
		String swapComment = "StackActivator - making stack:" + stackInstance + " to " + instanceRole + ".";
		ChangeBatch batch = new ChangeBatch().withChanges(changes).withComment(swapComment);
		ChangeResourceRecordSetsRequest req = new ChangeResourceRecordSetsRequest().withChangeBatch(batch).withHostedZoneId(backEndHostedZone.getId());
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
	
	public HostedZone getHostedZoneByName(String hostedZoneName) {
		HostedZone hostedZone = null;
		ListHostedZonesResult lhzRes = client.listHostedZones();
		List<HostedZone> hostedZones = lhzRes.getHostedZones();
		for (HostedZone hz :hostedZones) {
			if (hostedZoneName.equals(hz.getName())) {
				hostedZone = hz;
				break;
			}
		}
		return hostedZone;
	}
	
	public String getBackEndGenericCNAME(String svcPrefix, String instanceRole, String hostedZoneName) {
		return "%s-%s.%s".format(svcPrefix, instanceRole, hostedZoneName);
	}
	
	public String getBackEndInstanceCNAME(String svcPrefix, String instanceRole, String stackInstance, String hostedZoneName) {
		return "%s-%s-%s.%s".format(svcPrefix, instanceRole, stackInstance, hostedZoneName);
	}
	
	public String getPortalGenericCNAME() {
		return "staging.synapse.org";
	}
	
	public String getPortalInstanceCNAME(String instanceRole, String stackInstance, String hostedZoneName) {
		return "portal-%s-%s.%s".format(instanceRole, stackInstance, instanceRole, hostedZoneName);
	}
}
