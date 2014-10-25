/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sagebionetworks.stack;

import com.amazonaws.services.route53.AmazonRoute53Client;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
		activator = new Activator(factory, config, "prod", "123");
		String expectedCNAME = "repo-prod.prod.sagebase.org";
		String cname = activator.getBackEndGenericCNAME("repo", "prod", "sagebase.org");
		assertEquals(expectedCNAME, cname);
	}

}
