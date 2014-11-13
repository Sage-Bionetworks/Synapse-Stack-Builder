/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sagebionetworks.stack;

import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeInfo;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;

/**
 *
 * @author xavier
 */
public class ActivatorTest {
	
	Activator activator;
	InputConfiguration config;
	Properties props;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	AmazonRoute53Client mockClient;
	
	public ActivatorTest() {
	}
	
	@BeforeClass
	public static void setUpClass() {
	}
	
	@AfterClass
	public static void tearDownClass() {
	}
	
	@Before
	public void setUp() throws IOException {
		config = TestHelper.createActivatorTestConfiguration();
		mockClient = factory.createRoute53Client();
	}
	
	@After
	public void tearDown() {
	}
	
	@Test
	public void testGetBackEndGenericCNAME() throws IOException {
		activator = new Activator(factory, config, "123-0", "prod");
		String expectedCNAME = "repo-prod.prod.sagebase.org";
		String cname = activator.getBackEndGenericCNAME("repo", "prod", "prod", "sagebase.org");
		assertEquals(expectedCNAME, cname);
	}
	
	@Test
	public void testGetBackEndInstanceCNAME() throws IOException {
		activator = new Activator(factory, config, "123-0", "prod");
		String expectedCNAME = "repo-prod-123-0.prod.sagebase.org";
		String cname = activator.getBackEndInstanceCNAME("repo", "prod", "123-0", "prod", "sagebase.org");
		assertEquals(expectedCNAME, cname);
	}
	
	@Test
	public void testGetProdPortalGenericCNAME() throws IOException {
		activator = new Activator(factory, config, "123-0", "prod");
		String expectedCNAME = "synapse-prod.synapse.org";
		String cname = activator.getPortalGenericCNAME("prod");
		assertEquals(expectedCNAME, cname);
	}
	
	@Test
	public void testGetStagingPortalGenericCNAME() throws IOException {
		activator = new Activator(factory, config, "123-0", "staging");
		String expectedCNAME = "synapse-staging.synapse.org";
		String cname = activator.getPortalGenericCNAME("staging");
		assertEquals(expectedCNAME, cname);
	}
	
	@Test
	public void testGetProdPortalInstanceCNAME() throws IOException {
		activator = new Activator(factory, config, "123-0", "staging");
		String expectedCNAME = "portal-prod-123-0.prod.sagebase.org";
		String cname = activator.getPortalInstanceCNAME("123-0");
		assertEquals(expectedCNAME, cname);
	}

	@Test
	public void testGetHostedZoneByName() throws IOException {
		activator = new Activator(factory, config, "123-0", "staging");
		List<HostedZone> expectedListHostedZones = new ArrayList<HostedZone>();
		HostedZone hostedZone = new HostedZone().withName("hostedZone1");
		expectedListHostedZones.add(hostedZone);
		hostedZone = new HostedZone().withName("hostedZone2");
		expectedListHostedZones.add(hostedZone);
		ListHostedZonesResult expectedLhzRes = new ListHostedZonesResult();
		expectedLhzRes.setHostedZones(expectedListHostedZones);
		when(mockClient.listHostedZones()).thenReturn(expectedLhzRes);
		assertNull(activator.getHostedZoneByName("hostedZone0"));
		HostedZone actualHostedZone = activator.getHostedZoneByName("hostedZone1");
		assertNotNull(actualHostedZone);
		assertEquals("hostedZone1", actualHostedZone.getName());
	}
	
	@Test
	public void testMapbackendGenericCNAMEToInstanceCNAME() throws IOException {
		activator = new Activator(factory, config, "123-0", "staging");
		Map<String, String> map = activator.mapBackendGenericCNAMEToInstanceCNAME();
		assertNotNull(map);
		assertEquals(1, map.size());
		assertTrue(map.containsKey("repo-staging.prod.sagebase.org"));
		String s = map.get("repo-staging.prod.sagebase.org");
		assertEquals("repo-prod-123-0.prod.sagebase.org", s);
		activator = new Activator(factory, config, "456-1", "prod");
		map = activator.mapBackendGenericCNAMEToInstanceCNAME();
		assertNotNull(map);
		assertEquals(1, map.size());
		assertTrue(map.containsKey("repo-prod.prod.sagebase.org"));
		s = map.get("repo-prod.prod.sagebase.org");
		assertEquals("repo-prod-456-1.prod.sagebase.org", s);
	}
	
	@Test
	public void testMapPortalGenericCNAMEToInstanceCNAME() throws IOException {
		activator = new Activator(factory, config, "123-0", "staging");
		Map<String, String> map = activator.mapPortalGenericCNAMEToInstanceCNAME();
		assertNotNull(map);
		assertEquals(1, map.size());
		assertTrue(map.containsKey("synapse-staging.synapse.org"));
		String s = map.get("synapse-staging.synapse.org");
		assertEquals("portal-prod-123-0.prod.sagebase.org", s);
		activator = new Activator(factory, config, "456-1", "prod");
		map = activator.mapPortalGenericCNAMEToInstanceCNAME();
		assertNotNull(map);
		assertEquals(1, map.size());
		assertTrue(map.containsKey("synapse-prod.synapse.org"));
		s = map.get("synapse-prod.synapse.org");
		assertEquals("portal-prod-456-1.prod.sagebase.org", s);
	}
	
	@Test
	public void testGetResourceRecordSetByCNAME() throws IOException {
		// Setup expected ListResourceRecordSet requests and results
		ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest();
		req.setHostedZoneId("zoneId");
		req.setStartRecordType(RRType.CNAME);
		req.setStartRecordName("cname1");
		req.setMaxItems("1");
		//	Error case, the cname is not in the zone
		Collection<ResourceRecordSet> rrsList = new ArrayList<ResourceRecordSet>();
		ListResourceRecordSetsResult res = new ListResourceRecordSetsResult().withResourceRecordSets(rrsList);
		when(mockClient.listResourceRecordSets(req)).thenReturn(res);
		activator = new Activator(factory, config, "123-0", "staging");
		ResourceRecordSet rrs = null;
		try {
			rrs = activator.getResourceRecordSetByCNAME("zoneId", "cname1");
		} catch (IllegalArgumentException e) {
			assertNull(rrs);
		}
		//	OK case, cname found
		ResourceRecordSet rrs1 = new ResourceRecordSet().withName("cname1").withType(RRType.CNAME);
		rrsList.add(rrs1);
		res.setResourceRecordSets(rrsList);
		when(mockClient.listResourceRecordSets(req)).thenReturn(res);
		rrs = activator.getResourceRecordSetByCNAME("zoneId", "cname1");
		assertNotNull(rrs);
		assertEquals("cname1", rrs.getName());
		assertEquals(RRType.CNAME.toString(), rrs.getType());
	}
	
	@Test
	public void testCreateChangeList() throws IOException {
		ResourceRecordSet rrsOld = new ResourceRecordSet().withName("cname").withType(RRType.CNAME);
		String targetName = "adomain.org";
		List<Change> expectedChanges = new ArrayList<Change>();
		Change chg = new Change().withAction(ChangeAction.DELETE).withResourceRecordSet(rrsOld);
		expectedChanges.add(chg);
		ResourceRecordSet rrsNew = new ResourceRecordSet().withName("cname").withType(RRType.CNAME).withResourceRecords(new ResourceRecord().withValue(targetName)).withTTL(300L);
		chg = new Change().withResourceRecordSet(rrsNew).withAction(ChangeAction.CREATE);
		expectedChanges.add(chg);
		activator = new Activator(factory, config, "123-0", "staging");
		List<Change> changes = activator.createChangeList(rrsOld, targetName);
		assertNotNull(changes);
		assertEquals(expectedChanges, changes);
	}
	
	@Test
	public void testApplyChanges() throws IOException {
		String hostedZoneId = "zoneId";
		List<Change> changes = new ArrayList<Change>();
		Change chg = new Change().withAction(ChangeAction.DELETE).withResourceRecordSet(new ResourceRecordSet().withName("rrs1").withType(RRType.CNAME));
		changes.add(chg);
		ResourceRecord rr = new ResourceRecord().withValue("newName");
		chg = new Change().withAction(ChangeAction.DELETE).withResourceRecordSet(new ResourceRecordSet().withName("rrs1").withType(RRType.CNAME).withResourceRecords(rr));
		changes.add(chg);
		// Request
		String swapComment = "StackActivator - making stack: 123-0 to staging.";
		ChangeBatch batch = new ChangeBatch().withChanges(changes).withComment(swapComment);
		ChangeResourceRecordSetsRequest req = new ChangeResourceRecordSetsRequest().withChangeBatch(batch).withHostedZoneId(hostedZoneId);
		GetChangeRequest creq = new GetChangeRequest().withId("id");
		// Expected results
		ChangeInfo ciInProgress = new ChangeInfo().withStatus(ChangeStatus.InProgress).withId("id");
		ChangeInfo ciDeployed = new ChangeInfo().withStatus(ChangeStatus.Deployed).withId("id");
		ChangeResourceRecordSetsResult crrsrExpected = new ChangeResourceRecordSetsResult().withChangeInfo(ciInProgress);
		GetChangeResult gcrExpectedInProgress = new GetChangeResult().withChangeInfo(ciInProgress);
		GetChangeResult gcrExpectedDeployed = new GetChangeResult().withChangeInfo(ciDeployed);
		when(mockClient.changeResourceRecordSets(req)).thenReturn(crrsrExpected);
		when(mockClient.getChange(creq)).thenReturn(gcrExpectedInProgress, gcrExpectedInProgress, gcrExpectedDeployed);
		activator = new Activator(factory, config, "123-0", "staging");
		activator.applyChanges(hostedZoneId, changes);
		verify(mockClient, times(3)).getChange(creq);
	}
}
