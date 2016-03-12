package org.sagebionetworks.stack.ssl;

import org.sagebionetworks.stack.ssl.SSLSetup;
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
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateResult;
import com.amazonaws.services.s3.AmazonS3Client;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.TestHelper;
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
	
	@Test(expected = IllegalStateException.class)
	public void testDescribeSSLCertificateNoCertificate() {
		ListServerCertificatesResult expectedLstssr = new ListServerCertificatesResult();
		when(mockAmznIamClient.listServerCertificates()).thenReturn(expectedLstssr);
		SSLSetup sslSetup = new SSLSetup(factory, config, resources);
		sslSetup.describeSSLCertificate(StackEnvironmentType.REPO);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testDescribeSSLCertificateCertNotFound() {
		String certName = "someCertName";
		ServerCertificateMetadata srvCertMeta = new ServerCertificateMetadata().withServerCertificateName(certName);
		List<ServerCertificateMetadata> expectedLstSrvCertMetadata = new LinkedList<ServerCertificateMetadata>();
		expectedLstSrvCertMetadata.add(srvCertMeta);
		ListServerCertificatesResult expectedLstssr = new ListServerCertificatesResult().withServerCertificateMetadataList(expectedLstSrvCertMetadata);
		when(mockAmznIamClient.listServerCertificates()).thenReturn(expectedLstssr);
		SSLSetup sslSetup = new SSLSetup(factory, config, resources);
		sslSetup.describeSSLCertificate(StackEnvironmentType.REPO);
	}
	
	@Test
	public void testDescribeResourcesCertificateFound() {
		String expectedCertName = config.getSSLCertificateName(StackEnvironmentType.REPO);
		String expectedCertArn = "expectedCertArn";
		ServerCertificateMetadata srvCertMeta = new ServerCertificateMetadata().withServerCertificateName(expectedCertName).withArn(expectedCertArn);
		List<ServerCertificateMetadata> expectedLstSrvCertMetadata = new LinkedList<ServerCertificateMetadata>();
		expectedLstSrvCertMetadata.add(srvCertMeta);
		ListServerCertificatesResult expectedLstssr = new ListServerCertificatesResult().withServerCertificateMetadataList(expectedLstSrvCertMetadata);
		when(mockAmznIamClient.listServerCertificates()).thenReturn(expectedLstssr);
		SSLSetup sslSetup = new SSLSetup(factory, config, resources);
		sslSetup.describeSSLCertificate(StackEnvironmentType.REPO);
		//assertEquals(expectedCertArn, config.getSSLCertificateARN("generic"));
		assertEquals(srvCertMeta, resources.getSslCertificate(StackEnvironmentType.REPO));
		assertEquals(expectedCertArn, resources.getSslCertificate(StackEnvironmentType.REPO).getArn());
	}

	@Test
	public void testSetupSSLCertificateNoCertificate() {
		String expectedCertName = config.getSSLCertificateName(StackEnvironmentType.REPO);
		String expectedCertArn = "expectedCertArn";
		// Returned in 1st call to FindCertificate() --> empty
//		ServerCertificateMetadata scmdList1 = new ServerCertificateMetadata().withServerCertificateName(expectedCertName);
		List<ServerCertificateMetadata> expectedLstSrvCertMetadata1 = new LinkedList<ServerCertificateMetadata>();
//		expectedLstSrvCertMetadata1.add(scmdList1);
		// Returned in 2nd call to FindCertificate()
		ServerCertificateMetadata scmdList2 = new ServerCertificateMetadata().withServerCertificateName(expectedCertName).withArn(expectedCertArn);
		List<ServerCertificateMetadata> expectedLstSrvCertMetadata2 = new LinkedList<ServerCertificateMetadata>();
		expectedLstSrvCertMetadata1.add(scmdList2);
		// listServerCertificates() should return empty list, then uploaded cert
		ListServerCertificatesResult expectedLstssr1 = new ListServerCertificatesResult().withServerCertificateMetadataList(expectedLstSrvCertMetadata1);
		ListServerCertificatesResult expectedLstssr2 = new ListServerCertificatesResult().withServerCertificateMetadataList(expectedLstSrvCertMetadata2);
		when(mockAmznIamClient.listServerCertificates()).thenReturn(expectedLstssr1, expectedLstssr2);
		// Call to uploadServerCertificate()
		ServerCertificateMetadata srvCertMeta = new ServerCertificateMetadata().withServerCertificateName(expectedCertName).withArn(expectedCertArn);
		UploadServerCertificateRequest uscr = new UploadServerCertificateRequest().withServerCertificateName(expectedCertName);
		UploadServerCertificateResult expectedUscr = new UploadServerCertificateResult().withServerCertificateMetadata(srvCertMeta);
		when(mockAmznIamClient.uploadServerCertificate(uscr)).thenReturn(expectedUscr);
		SSLSetup sslSetup = new SSLSetup(factory, config, resources);
		sslSetup.setupSSLCertificate(StackEnvironmentType.REPO);
		// Meta for upload server cert should be in resources
		assertEquals(expectedCertName, resources.getSslCertificate(StackEnvironmentType.REPO).getServerCertificateName());
		assertEquals(expectedCertArn, resources.getSslCertificate(StackEnvironmentType.REPO).getArn());
	}
	
	// TODO: Add test to add cert to existing list
	// TODO: Add test to add existing certificate (update)
}
