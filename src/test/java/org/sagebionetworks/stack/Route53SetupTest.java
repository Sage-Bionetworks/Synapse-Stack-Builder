package org.sagebionetworks.stack;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;


import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.ChangeInfo;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ChangeStatus;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Ignore;

/**
 *
 * @author xavier
 */
public class Route53SetupTest {
	
	InputConfiguration config;
	GeneratedResources resources;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	AmazonRoute53Client mockClient;
	
	public Route53SetupTest() {
	}
	
//	@BeforeClass
//	public static void setUpClass() {
//	}
//	
//	@AfterClass
//	public static void tearDownClass() {
//	}
//	
	@Before
	public void setUp() throws IOException {
		config = TestHelper.createRoute53TestConfig("stack");
		resources = new GeneratedResources();
		mockClient = factory.createRoute53Client();
	}
	
//	@After
//	public void tearDown() {
//	}
//
	
	@Test
	public void testGetHostedZoneExistentZone() {
		String stack = "stack";
		String hostedZoneDomainName = stack + ".sagebase.org.";
		ListHostedZonesResult res = new ListHostedZonesResult();
		List<HostedZone> expectedHostedZones = new ArrayList<HostedZone>();
		HostedZone hz = new HostedZone().withName(hostedZoneDomainName);
		expectedHostedZones.add(hz);
		hz = new HostedZone().withName("anotherzone.sagebase.org.");
		expectedHostedZones.add(hz);
		res.setHostedZones(expectedHostedZones);
		when(mockClient.listHostedZones()).thenReturn(res);
		
		Route53Setup r53Setup = new Route53Setup(factory, config, resources);
		HostedZone z = r53Setup.getHostedZone(stack + ".sagebase.org");
		assertEquals(hostedZoneDomainName, z.getName());
	
	}
	
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetHostedZoneNonExistentZone() {
		String hostedZoneDomainName = "r53.sagebase.org.";
		ListHostedZonesResult res = new ListHostedZonesResult();
		List<HostedZone> expectedHostedZones = new ArrayList<HostedZone>();
		HostedZone hz = new HostedZone().withName("sone1.org.");
		expectedHostedZones.add(hz);
		hz = new HostedZone().withName("zone2.org.");
		expectedHostedZones.add(hz);
		res.setHostedZones(expectedHostedZones);
		when(mockClient.listHostedZones()).thenReturn(res);
		Route53Setup r53Setup = new Route53Setup(factory, config, resources);
		HostedZone z = r53Setup.getHostedZone("r53.sagebase.org");
	
	}
	
	
	@Test
	public void testGetResourceRecordSetForRecordNameAllFound() {
		String stack = "stack";
		String hostedZoneDomainName = stack + ".sagebase.org.";
		ListHostedZonesResult res = new ListHostedZonesResult();
		List<HostedZone> expectedHostedZones = new ArrayList<HostedZone>();
		HostedZone hz = new HostedZone().withName(hostedZoneDomainName);
		expectedHostedZones.add(hz);
		hz = new HostedZone().withName("anotherzone.sagebase.org");
		expectedHostedZones.add(hz);
		res.setHostedZones(expectedHostedZones);
		Map<ListResourceRecordSetsRequest, ListResourceRecordSetsResult> expectedResourceRecordSetsResults = TestHelper.createListExpectedListResourceRecordSetsRequestAllFound(stack);
		when(mockClient.listHostedZones()).thenReturn(res);
		// Args for getResourceRecordSetForRecordName().listResourceRecordSets()
		for (ListResourceRecordSetsRequest req: expectedResourceRecordSetsResults.keySet()) {
			when(mockClient.listResourceRecordSets(req)).thenReturn(expectedResourceRecordSetsResults.get(req));
		}

		Route53Setup r53Setup = new Route53Setup(factory, config, resources);
		for (String svcPrefix: Arrays.asList(Constants.PREFIX_AUTH, Constants.PREFIX_PORTAL, Constants.PREFIX_REPO, Constants.PREFIX_SEARCH)) {
			ResourceRecordSet rrs = r53Setup.getResourceRecordSetForRecordName(svcPrefix + ".stack.inst.r53.sagebase.org");
			assertFalse(rrs == null);
			assertEquals(rrs.getName(), svcPrefix + ".stack.inst.r53.sagebase.org");
			assertEquals(rrs.getResourceRecords().get(0).getValue(), svcPrefix + "-stack-inst-sagebase-org.elasticbeanstalk.com");
		}
	}
	
	@Test
	public void testGetResourceRecordSetForRecordNameNoneFound() {
		String stack = "stack";
		String hostedZoneDomainName = stack + ".sagebase.org.";
		ListHostedZonesResult res = new ListHostedZonesResult();
		List<HostedZone> expectedHostedZones = new ArrayList<HostedZone>();
		HostedZone hz = new HostedZone().withName(hostedZoneDomainName);
		expectedHostedZones.add(hz);
		hz = new HostedZone().withName("anotherzone.sagebase.org");
		expectedHostedZones.add(hz);
		res.setHostedZones(expectedHostedZones);
		Map<ListResourceRecordSetsRequest, ListResourceRecordSetsResult> expectedResourceRecordSetsResults = TestHelper.createListExpectedListResourceRecordSetsRequestNoneFound(stack);
		when(mockClient.listHostedZones()).thenReturn(res);
		// Args for getResourceRecordSetForRecordName().listResourceRecordSets()
		for (ListResourceRecordSetsRequest req: expectedResourceRecordSetsResults.keySet()) {
			when(mockClient.listResourceRecordSets(req)).thenReturn(expectedResourceRecordSetsResults.get(req));
		}

		Route53Setup r53Setup = new Route53Setup(factory, config, resources);
		for (String svcPrefix: Arrays.asList(Constants.PREFIX_AUTH, Constants.PREFIX_PORTAL, Constants.PREFIX_REPO, Constants.PREFIX_SEARCH)) {
			ResourceRecordSet rrs = r53Setup.getResourceRecordSetForRecordName(svcPrefix + ".stack.inst.r53.sagebase.org");
			assertTrue(rrs == null);
		}
	}
	
	@Ignore
	@Test
	public void testSetupResourcesAllFound() throws Exception {
		String stack = "stack";
		String hostedZoneDomainName = stack + ".sagebase.org.";
		ListHostedZonesResult res = new ListHostedZonesResult();
		List<HostedZone> expectedHostedZones = new ArrayList<HostedZone>();
		HostedZone hz = new HostedZone().withName(hostedZoneDomainName);
		expectedHostedZones.add(hz);
		hz = new HostedZone().withName("anotherzone.sagebase.org.");
		expectedHostedZones.add(hz);
		res.setHostedZones(expectedHostedZones);
		Map<ListResourceRecordSetsRequest, ListResourceRecordSetsResult> expectedResourceRecordSetsResults = TestHelper.createListExpectedListResourceRecordSetsRequestAllFound(stack);
		when(mockClient.listHostedZones()).thenReturn(res);
		// Args for getResourceRecordSetForRecordName().listResourceRecordSets()
		for (ListResourceRecordSetsRequest req: expectedResourceRecordSetsResults.keySet()) {
			when(mockClient.listResourceRecordSets(req)).thenReturn(expectedResourceRecordSetsResults.get(req));
		}
		
		ChangeInfo expectedChangeInfo = new ChangeInfo().withId("changeInfoId").withStatus(ChangeStatus.Deployed);
		ChangeResourceRecordSetsResult expectedChangeResourceRecordSetsResult = new ChangeResourceRecordSetsResult().withChangeInfo(expectedChangeInfo);
		when(mockClient.changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class))).thenReturn(expectedChangeResourceRecordSetsResult);
		
		Route53Setup r53Setup = new Route53Setup(factory, config, resources);
		
	}
}
