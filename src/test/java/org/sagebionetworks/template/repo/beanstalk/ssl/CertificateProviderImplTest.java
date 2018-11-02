package org.sagebionetworks.template.repo.beanstalk.ssl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.Constants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import com.amazonaws.services.s3.AmazonS3;

@RunWith(MockitoJUnitRunner.class)
public class CertificateProviderImplTest {

	@Mock
	AmazonS3 mockS3Client;
	@Mock
	Configuration mockConfiguration;
	@Mock
	CertificateBuilder mockBuilder;
	
	@InjectMocks
	CertificateProviderImpl provider;
	
	String bucketName;
	String stack;
	String instance;
	CertificatePair pair;
	String certificateS3Key;
	String privateKeyS3key;
	
	URL certificateUrl;
	URL privateKeyUrl;
	
	@Before
	public void before() throws MalformedURLException {
		stack = "dev";
		instance = "test1";
		when(mockConfiguration.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn(stack);
		when(mockConfiguration.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		bucketName = "someBucket";
		when(mockConfiguration.getConfigurationBucket()).thenReturn(bucketName);
		
		pair = new CertificatePair("certificatePEM","privatePEM");
		when(mockBuilder.buildNewX509CertificatePair()).thenReturn(pair);
		
		certificateS3Key = provider.buildCertificateS3Key();
		privateKeyS3key = provider.buildPrivateKeyS3Key();
		
		certificateUrl = new URL("https", "aws", certificateS3Key);
		privateKeyUrl = new URL("https", "aws", privateKeyS3key);
		when(mockS3Client.getUrl(bucketName, certificateS3Key)).thenReturn(certificateUrl);
		when(mockS3Client.getUrl(bucketName, privateKeyS3key)).thenReturn(privateKeyUrl);
	}
	
	@Test
	public void testBuildSSLFolder() {
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		String expected = "ssl/dev/test1/"+year+"/"+month;
		// call under test
		String folder = provider.buildSSLFolder();
		assertEquals(expected, folder);
	}
	
	@Test
	public void testBuildCertificateS3Key() {
		String folder = provider.buildSSLFolder();
		String expected = folder+"/x509-certificate.pem";
		// call under test
		String key = provider.buildCertificateS3Key();
		assertEquals(expected, key);
	}
	
	@Test
	public void testBuildPrivateKeyS3Key() {
		String folder = provider.buildSSLFolder();
		String expected = folder+"/rsa-private-key.pem";
		// call under test
		String key = provider.buildPrivateKeyS3Key();
		assertEquals(expected, key);
	}
	
	@Test
	public void testBuildAndUploadNewCertificatePair() {
		String bucketName = "someBucket";
		String certificateS3Key = "certKey";
		String rsaPrivateKeyS3Key = "privatekey";
		// call under test
		provider.buildAndUploadNewCertificatePair(bucketName, certificateS3Key, rsaPrivateKeyS3Key);
		verify(mockBuilder).buildNewX509CertificatePair();
		verify(mockS3Client).putObject(bucketName, certificateS3Key, pair.getX509CertificatePEM());
		verify(mockS3Client).putObject(bucketName, rsaPrivateKeyS3Key, pair.getPrivateKeyPEM());
	}
	
	@Test
	public void testProvideCertificateUrlsS3FilesExist() {
		when(mockS3Client.doesObjectExist(any(String.class), any(String.class))).thenReturn(true);
		// Call under test
		CertificateUrls urls = provider.provideCertificateUrls();
		assertNotNull(urls);
		assertEquals(certificateUrl.toString(), urls.getX509CertificateUrl());
		assertEquals(privateKeyUrl.toString(), urls.getPrivateKeyUrl());
		verify(mockS3Client, never()).putObject(anyString(), anyString(), anyString());
	}
}
