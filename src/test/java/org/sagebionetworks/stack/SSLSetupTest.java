package org.sagebionetworks.stack;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesResult;
import com.amazonaws.services.identitymanagement.model.ServerCertificateMetadata;
import com.amazonaws.services.s3.AmazonS3Client;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;

/**
 *
 * @author xavier
 */
public class SSLSetupTest {

	InputConfiguration config;
	GeneratedResources resources;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	AmazonIdentityManagementClient mockAmznIamClient;
	AmazonS3Client mockAmznS3Client;

	public SSLSetupTest() {
	}
	
	@Before
	public void setUp() throws IOException {
		config = TestHelper.createTestConfig("stack");
		resources = new GeneratedResources();
		mockAmznIamClient = factory.createIdentityManagementClient();
		mockAmznS3Client = factory.createS3Client();
	}
	
	@After
	public void tearDown() {
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testDescribeResourcesInvalidArg() {
		SSLSetup sslSetup = new SSLSetup(factory, config, resources);
		sslSetup.describeResources("generics");
	}
	
	@Test(expected = IllegalStateException.class)
	public void testDescribeResourcesNoCertificate() {
		ListServerCertificatesResult expectedLstssr = new ListServerCertificatesResult();
		when(mockAmznIamClient.listServerCertificates()).thenReturn(expectedLstssr);
		SSLSetup sslSetup = new SSLSetup(factory, config, resources);
		sslSetup.describeResources("generic");
	}
	
	@Test(expected = IllegalStateException.class)
	public void testDescribeResourcesCertificateNotFound() {
		String certName = "someCertName";
		ServerCertificateMetadata srvCertMeta = new ServerCertificateMetadata().withServerCertificateName(certName);
		List<ServerCertificateMetadata> expectedLstSrvCertMetadata = new LinkedList<ServerCertificateMetadata>();
		expectedLstSrvCertMetadata.add(srvCertMeta);
		ListServerCertificatesResult expectedLstssr = new ListServerCertificatesResult().withServerCertificateMetadataList(expectedLstSrvCertMetadata);
		when(mockAmznIamClient.listServerCertificates()).thenReturn(expectedLstssr);
		SSLSetup sslSetup = new SSLSetup(factory, config, resources);
		sslSetup.describeResources("generic");
	}
	
	@Test
	public void testDescribeResourcesCertificateFound() {
		String expectedCertName = "wildcard.sagebase.org-2012-2";
		String expectedCertArn = "arn";
		ServerCertificateMetadata srvCertMeta = new ServerCertificateMetadata().withServerCertificateName(expectedCertName).withArn(expectedCertArn);
		List<ServerCertificateMetadata> expectedLstSrvCertMetadata = new LinkedList<ServerCertificateMetadata>();
		expectedLstSrvCertMetadata.add(srvCertMeta);
		ListServerCertificatesResult expectedLstssr = new ListServerCertificatesResult().withServerCertificateMetadataList(expectedLstSrvCertMetadata);
		when(mockAmznIamClient.listServerCertificates()).thenReturn(expectedLstssr);
		SSLSetup sslSetup = new SSLSetup(factory, config, resources);
		sslSetup.describeResources("generic");
		//assertEquals(expectedCertArn, config.getSSLCertificateARN("generic"));
		assertEquals(srvCertMeta, resources.getSslCertificate("generic"));
	}

//	@Test(expected = IllegalArgumentException.class)
//	public void testSetupResourcesInvalidArg() {
//		SSLSetup sslSetup = new SSLSetup(factory, config, resources);
//		sslSetup.setupResources();
//	}
	
}
