package org.sagebionetworks.stack.ssl;

import java.io.IOException;

import com.amazonaws.services.certificatemanager.AWSCertificateManagerClient;
import com.amazonaws.services.certificatemanager.model.GetCertificateRequest;
import com.amazonaws.services.certificatemanager.model.GetCertificateResult;
import com.amazonaws.services.certificatemanager.model.ResourceNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.eq;

import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.StackEnvironmentType;
import org.sagebionetworks.stack.TestHelper;
import org.sagebionetworks.stack.config.InputConfiguration;

public class ACMSetupTest {
	
	InputConfiguration config;
	GeneratedResources resources;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	AWSCertificateManagerClient mockAcmClient;
	
	@Before
	public void setUp() throws IOException {
		config = TestHelper.createTestConfig("stack");
		resources = new GeneratedResources();
		mockAcmClient = factory.createCertificateManagerClient();
	}
	
	@After
	public void tearDown() {
	}
	
	@Test
	public void testHappyCase() throws Exception {
		GetCertificateRequest portalReq = new GetCertificateRequest().withCertificateArn(config.getACMCertificateArn(StackEnvironmentType.PORTAL));
		GetCertificateRequest repoReq = new GetCertificateRequest().withCertificateArn(config.getACMCertificateArn(StackEnvironmentType.REPO));
		GetCertificateRequest workersReq = new GetCertificateRequest().withCertificateArn(config.getACMCertificateArn(StackEnvironmentType.WORKERS));
		GetCertificateResult res = new GetCertificateResult();
		when(mockAcmClient.getCertificate(portalReq)).thenReturn(res);
		when(mockAcmClient.getCertificate(repoReq)).thenReturn(res);
		when(mockAcmClient.getCertificate(workersReq)).thenReturn(res);
		ACMSetup acm = new ACMSetup(factory, config, resources);
		assertNull(resources.getACMCertificateArn(StackEnvironmentType.PORTAL));
		assertNull(resources.getACMCertificateArn(StackEnvironmentType.REPO));
		assertNull(resources.getACMCertificateArn(StackEnvironmentType.WORKERS));
		acm.setupResources();
		assertEquals(config.getACMCertificateArn(StackEnvironmentType.PORTAL), resources.getACMCertificateArn(StackEnvironmentType.PORTAL));
		assertEquals(config.getACMCertificateArn(StackEnvironmentType.REPO), resources.getACMCertificateArn(StackEnvironmentType.REPO));
		assertEquals(config.getACMCertificateArn(StackEnvironmentType.WORKERS), resources.getACMCertificateArn(StackEnvironmentType.WORKERS));
	}
	
	@Test(expected=ResourceNotFoundException.class)
	public void testACMException() throws Exception {
		GetCertificateRequest portalReq = new GetCertificateRequest().withCertificateArn(config.getACMCertificateArn(StackEnvironmentType.PORTAL));
		GetCertificateRequest repoReq = new GetCertificateRequest().withCertificateArn(config.getACMCertificateArn(StackEnvironmentType.REPO));
		GetCertificateRequest workersReq = new GetCertificateRequest().withCertificateArn(config.getACMCertificateArn(StackEnvironmentType.WORKERS));
		GetCertificateResult res = new GetCertificateResult();
		when(mockAcmClient.getCertificate(portalReq)).thenReturn(res);
		when(mockAcmClient.getCertificate(repoReq)).thenThrow(new ResourceNotFoundException("Could not find REPO ACM certificate."));
		when(mockAcmClient.getCertificate(workersReq)).thenReturn(res);
		ACMSetup acm = new ACMSetup(factory, config, resources);
		assertNull(resources.getACMCertificateArn(StackEnvironmentType.PORTAL));
		assertNull(resources.getACMCertificateArn(StackEnvironmentType.REPO));
		assertNull(resources.getACMCertificateArn(StackEnvironmentType.WORKERS));
		acm.setupResources();
	}
}
