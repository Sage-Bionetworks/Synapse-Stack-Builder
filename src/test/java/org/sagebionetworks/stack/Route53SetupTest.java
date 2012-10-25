package org.sagebionetworks.stack;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.when;

import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;


import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import java.io.IOException;
import java.util.ArrayList;
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
		config = TestHelper.createRoute53TestConfig("dev");
		resources = new GeneratedResources();
		mockClient = factory.createRoute53Client();
	}
	
//	@After
//	public void tearDown() {
//	}
//
	@Test
	public void testGetHostedZoneExistentZone() {
		String hostedZoneDomainName = "r53.sagebase.org";
		ListHostedZonesResult res = new ListHostedZonesResult();
		List<HostedZone> expectedHostedZones = new ArrayList<HostedZone>();
		HostedZone hz = new HostedZone().withName(hostedZoneDomainName);
		expectedHostedZones.add(hz);
		hz = new HostedZone().withName("anotherzone.sagebase.org");
		expectedHostedZones.add(hz);
		res.setHostedZones(expectedHostedZones);
		when(mockClient.listHostedZones()).thenReturn(res);
		
		Route53Setup r53Setup = new Route53Setup(factory, config, resources);
		HostedZone z = r53Setup.getHostedZone(hostedZoneDomainName);
		assertEquals(hostedZoneDomainName, z.getName());
	
	}
	@Test(expected = IllegalArgumentException.class)
	public void testGetHostedZoneNonExistentZone() {
		String hostedZoneDomainName = "r53.sagebase.org";
		ListHostedZonesResult res = new ListHostedZonesResult();
		List<HostedZone> expectedHostedZones = new ArrayList<HostedZone>();
		HostedZone hz = new HostedZone().withName("sone1.org");
		expectedHostedZones.add(hz);
		hz = new HostedZone().withName("zone2.org");
		expectedHostedZones.add(hz);
		res.setHostedZones(expectedHostedZones);
		when(mockClient.listHostedZones()).thenReturn(res);
		Route53Setup r53Setup = new Route53Setup(factory, config, resources);
		HostedZone z = r53Setup.getHostedZone(hostedZoneDomainName);
	
	}
	
	@Ignore
	@Test
	public void testSetupResourcesAllFound() throws Exception {
		String hostedZoneDomainName = "r53.sagebase.org";
		ListHostedZonesResult res = new ListHostedZonesResult();
		List<HostedZone> expectedHostedZones = new ArrayList<HostedZone>();
		HostedZone hz = new HostedZone().withName(hostedZoneDomainName);
		expectedHostedZones.add(hz);
		hz = new HostedZone().withName("anotherzone.sagebase.org");
		expectedHostedZones.add(hz);
		res.setHostedZones(expectedHostedZones);
		Map<ListResourceRecordSetsRequest, ListResourceRecordSetsResult> expectedResourceRecordSetsResults = TestHelper.createListExpectedListResourceRecordSetsRequestAllFound();
		when(mockClient.listHostedZones()).thenReturn(res);
		// Args for getResourceRecordSetForRecordName().listResourceRecordSets()
		for (ListResourceRecordSetsRequest req: expectedResourceRecordSetsResults.keySet()) {
			when(mockClient.listResourceRecordSets(req)).thenReturn(expectedResourceRecordSetsResults.get(req));
		}
		
	}
}
