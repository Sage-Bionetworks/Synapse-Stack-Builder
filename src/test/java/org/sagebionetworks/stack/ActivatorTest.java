/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sagebionetworks.stack;

import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
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
		
	}
}
