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

	public Activator(AmazonClientFactory factory, InputConfiguration inputConfig, String stackInstance, String instanceRole) throws IOException {
		config = inputConfig;
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
		
		// BackEnd services CNAMEs map
		Map<String, String> genericToInstanceCNAMEMap = mapBackendGenericCNAMEToInstanceCNAME();
		
		// For each map entry, append append delete/create to list of changes
		List<Change> changes = createChangesForCNAMEs(genericToInstanceCNAMEMap);
		
		// Apply changes
		applyChanges(changes);
		
		// Portal

		// Portal service CNAMEs map
		Map<String, String> portalGenericToInstanceCNAMEMap = mapPortalGenericCNAMEToInstanceCNAME();
		changes = createChangesForCNAMEs(portalGenericToInstanceCNAMEMap);
		applyChanges(changes);
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
	
	public List<Change> createChangesForCNAMEs(Map<String, String> map) {
		List<Change> changes = new ArrayList<Change>();
		for (String k :map.keySet()) {
			List<Change> lc = createChangesForCNAME(k, map.get(k));
			changes.addAll(lc);
		}
		return changes;
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
	
	public Map<String, String> mapBackendGenericCNAMEToInstanceCNAME() {
		Map<String, String> mapGenericToInstanceNames = new HashMap<String, String>();
		List<String> backEndSvcPrefixes = Arrays.asList(Constants.PREFIX_REPO);
		String r53DomainName = backEndHostedZone.getName();
		for (String backEndSvcPrefix :backEndSvcPrefixes) {
			mapGenericToInstanceNames.put(getBackEndGenericCNAME(backEndSvcPrefix, instanceRole, r53DomainName), getBackEndInstanceCNAME(backEndSvcPrefix, instanceRole, "prod", r53DomainName));
		}
		return mapGenericToInstanceNames;
	}
	
	public Map<String, String> mapPortalGenericCNAMEToInstanceCNAME() {
		Map<String, String> mapGenericToInstanceNames = new HashMap<String, String>();
		List<String> portalSvcPrefixes = Arrays.asList(Constants.PREFIX_PORTAL);
		String r53GenericSubdomainName = portalHostedZone.getName();
		String r53InstanceSubdomainName = backEndHostedZone.getName();
		for (String portalSvcPrefix: portalSvcPrefixes) {
			mapGenericToInstanceNames.put(getPortalGenericCNAME(portalSvcPrefix, instanceRole, r53GenericSubdomainName), getPortalInstanceCNAME(portalSvcPrefix, instanceRole, "prod", r53InstanceSubdomainName));
		}
		return mapGenericToInstanceNames;
	}
	
	public String getBackEndGenericCNAME(String svcPrefix, String instanceRole, String hostedZoneName) {
		String s = String.format("%s-%s.%s", svcPrefix, instanceRole, hostedZoneName);
		return s;
	}
	
	public String getBackEndInstanceCNAME(String svcPrefix, String instanceRole, String stackInstance, String hostedZoneName) {
		return String.format("%s-%s-%s.%s", svcPrefix, instanceRole, stackInstance, hostedZoneName);
	}
	
	public String getPortalGenericCNAME(String svcPrefix, String instanceRole, String hostedZoneName) {
		if (! "portal".equals(svcPrefix)) {
			throw new IllegalArgumentException("SvcPrefix must be 'portal'.");
		}
		if ((!"prod".equals(instanceRole)) | (!"staging".equals(instanceRole))) {
			throw new IllegalArgumentException("InstanceRole must be 'prod' or 'staging'.");
		}
		String r53Subdomain = null;
		if ("prod".equals(instanceRole)) {
			r53Subdomain = "www";
		} else {
			r53Subdomain = "staging";
		}
		return String.format("%s.%s", r53Subdomain, hostedZoneName);
	}
	
	public String getPortalInstanceCNAME(String svcPrefix, String instanceRole, String stackInstance, String hostedZoneName) {
		if (! "portal".equals(svcPrefix)) {
			throw new IllegalArgumentException("SvcPrefix must be 'portal'.");
		}
		if ((!"prod".equals(instanceRole)) | (!"staging".equals(instanceRole))) {
			throw new IllegalArgumentException("InstanceRole must be 'prod' or 'staging'.");
		}
		return String.format("%s-%s-%s.%s", svcPrefix, instanceRole, stackInstance, instanceRole, hostedZoneName);
	}
}
