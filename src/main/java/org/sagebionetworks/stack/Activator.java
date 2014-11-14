package org.sagebionetworks.stack;

import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ChangeStatus;
import com.amazonaws.services.route53.model.GetChangeRequest;
import com.amazonaws.services.route53.model.GetChangeResult;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.amazonaws.services.s3.AmazonS3Client;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 *
 * @author xschildw
 */
public class Activator {
	private AmazonRoute53Client route53Client;
	private AmazonS3Client s3Client;
	private InputConfiguration config;
	private String stackInstance, instanceRole;

	public Activator(AmazonClientFactory factory, InputConfiguration inputConfig, String stackInstance, String instanceRole) throws IOException {
		config = inputConfig;
		factory.setCredentials(config.getAWSCredentials());
		
		this.route53Client = factory.createRoute53Client();
		this.s3Client = factory.createS3Client();
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
		String hostedZoneId = getHostedZoneByName(Constants.R53_BACKEND_HOSTEDZONE_NAME).getId();
		Map<String, String> genericToInstanceCNAMEMap = ActivatorUtils.mapBackendGenericCNAMEToInstanceCNAME(instanceRole, stackInstance);
		List<Change> changes = createChangesForCNAMEs(hostedZoneId, genericToInstanceCNAMEMap);
		applyChanges(hostedZoneId, changes);
		
		// Portal
		hostedZoneId = getHostedZoneByName(Constants.R53_PORTAL_HOSTEDZONE_NAME).getId();
		Map<String, String> portalGenericToInstanceCNAMEMap = ActivatorUtils.mapPortalGenericCNAMEToInstanceCNAME(instanceRole, stackInstance);
		changes = createChangesForCNAMEs(hostedZoneId, portalGenericToInstanceCNAMEMap);
		applyChanges(hostedZoneId, changes);
	}
	
	public void saveActivationRecord(Long activationTime) {
		
	}
	
	public List<Change> createChangesForCNAME(String hostedZoneId, String cName, String newTarget) {
		List<Change> lc = null;
		ResourceRecordSet rrs = getResourceRecordSetByCNAME(hostedZoneId, cName);
		lc = createChangeList(rrs, newTarget);
		
		return lc;
	}
	
	
	public List<Change> createChangesForCNAMEs(String hostedZoneId, Map<String, String> map) {
		List<Change> changes = new ArrayList<Change>();
		for (String k :map.keySet()) {
			List<Change> lc = createChangesForCNAME(hostedZoneId, k, map.get(k));
			changes.addAll(lc);
		}
		return changes;
	}
	
	public List<Change> createChangeList(ResourceRecordSet rrs, String newValue) {
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
	
	public void applyChanges(String hostedZoneId, List<Change> changes) {
		String swapComment = "StackActivator - making stack: " + stackInstance + " to " + instanceRole + ".";
		ChangeBatch batch = new ChangeBatch().withChanges(changes).withComment(swapComment);
		ChangeResourceRecordSetsRequest req = new ChangeResourceRecordSetsRequest().withChangeBatch(batch).withHostedZoneId(hostedZoneId);
		ChangeResourceRecordSetsResult cRes = route53Client.changeResourceRecordSets(req);
		GetChangeRequest gcReq = new GetChangeRequest(cRes.getChangeInfo().getId());
		GetChangeResult gcRes = route53Client.getChange(gcReq);
		// TODO: No real need to wait here, could just exit
		while (! ChangeStatus.Deployed.name().equals(gcRes.getChangeInfo().getStatus())) {
			try {
				Thread.sleep(1000L);
				gcRes = route53Client.getChange(gcReq);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public HostedZone getHostedZoneByName(String hostedZoneName) {
		HostedZone hostedZone = null;
		ListHostedZonesResult lhzRes = route53Client.listHostedZones();
		List<HostedZone> hostedZones = lhzRes.getHostedZones();
		for (HostedZone hz :hostedZones) {
			if (hostedZoneName.equals(hz.getName())) {
				hostedZone = hz;
				break;
			}
		}
		return hostedZone;
	}
	
	public ResourceRecordSet getResourceRecordSetByCNAME(String hostedZoneId, String cName) {
		ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest();
		req.setHostedZoneId(hostedZoneId);
		req.setStartRecordType(RRType.CNAME);
		req.setStartRecordName(cName);
		req.setMaxItems("1");
		ListResourceRecordSetsResult lrRes = route53Client.listResourceRecordSets(req);
		ResourceRecordSet rrs = null;
		if ((lrRes.getResourceRecordSets() != null) && (lrRes.getResourceRecordSets().size() > 0) && (cName.equals(lrRes.getResourceRecordSets().get(0).getName()))) {
			rrs = lrRes.getResourceRecordSets().get(0);
		} else {
			throw new IllegalArgumentException("CNAME {0} not found in zone".format(cName));
		}
		return rrs;
	}

	
}
