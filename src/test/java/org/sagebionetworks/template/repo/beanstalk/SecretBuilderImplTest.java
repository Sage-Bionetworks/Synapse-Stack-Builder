package org.sagebionetworks.template.repo.beanstalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.template.Constants.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.Configuration;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;

@RunWith(MockitoJUnitRunner.class)
public class SecretBuilderImplTest {
	
	@Mock
	Configuration mockConfig;
	@Mock
	AWSSecretsManager mockSecretManager;
	@Mock
	AWSKMS mockKeyManager;
	@Mock
	AmazonS3 mockS3Client;
	
	@Captor
	ArgumentCaptor<GetSecretValueRequest> secretRequestCaptor;
	@Captor
	ArgumentCaptor<EncryptRequest> encryptRequestCaptor;
	@Captor
	ArgumentCaptor<PutObjectRequest> putObjectRequsetCaptor;
	
	String stack;
	String instance;
	SecretBuilderImpl builder;
	
	String key;
	String secretString;
	String encryptedSecretValue;
	ByteBuffer secretBuffer;
	
	String s3Bucket;
	String expectedS3Key;
	
	@Before
	public void before() {
		stack = "dev";
		instance = "299";
		 key = "org.sagebionetworks.some.key";
		 
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_SECRET_KEYS_CSV)).thenReturn(new String[] {key});
		
		
		builder = new SecretBuilderImpl(mockConfig, mockSecretManager, mockKeyManager, mockS3Client);
		
		secretString = "super secret";
		when(mockSecretManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(new GetSecretValueResult().withSecretString(secretString));
		encryptedSecretValue = "pretend this is encrypted";
		secretBuffer = SecretBuilderImpl.stringToByteBuffer(encryptedSecretValue);
		when(mockKeyManager.encrypt(any(EncryptRequest.class))).thenReturn(new EncryptResult().withCiphertextBlob(secretBuffer));
		
		s3Bucket = "the-bucket";
		when(mockConfig.getConfigurationBucket()).thenReturn(s3Bucket);
		expectedS3Key = "Stack/dev-299-secrets.properties";
	}
	
	@Test
	public void testStringToByteBuffer() throws UnsupportedEncodingException {
		String value = "value";
		// call under test
		ByteBuffer buffer = SecretBuilderImpl.stringToByteBuffer(value);
		// call under test
		String base64 = SecretBuilderImpl.base64Encode(buffer);
		String result = base64Decode(base64);
		assertEquals(value, result);
	}
	
	@Test
	public void testCreateParameterName() {
		// call under test
		String paraName = SecretBuilderImpl.createParameterName(key);
		assertEquals("SomeKey", paraName);
	}
	
	@Test
	public void testGetMasterSecretKey() {
		// call under test
		String masterKey = builder.getMasterSecretKey(key);
		assertEquals("dev.org.sagebionetworks.some.key", masterKey);
	}
	
	@Test
	public void testGetCMKAlias() {
		// call under test
		String alias = builder.getCMKAlias();
		assertEquals("alias/synapse/dev/299/cmk", alias);
	}
	
	@Test
	public void testGetSecretValue() {
		// call under test
		String result = builder.getSecretValue(key);
		assertEquals(secretString, result);
		verify(mockSecretManager).getSecretValue(secretRequestCaptor.capture());
		assertEquals("dev.org.sagebionetworks.some.key", secretRequestCaptor.getValue().getSecretId());
	}
	
	@Test
	public void testCreateSecret() {
		// Call under test
		String cipher = builder.createSecret(key);
		assertNotNull(cipher);
		assertEquals(encryptedSecretValue, base64Decode(cipher));
		verify(mockKeyManager).encrypt(encryptRequestCaptor.capture());
		assertEquals("alias/synapse/dev/299/cmk", encryptRequestCaptor.getValue().getKeyId());
		assertEquals(secretString, byteBufferToString(encryptRequestCaptor.getValue().getPlaintext()));
	}
	
	@Test
	public void testCreateSecretS3Key() {
		// Call under test
		String key = builder.createSecretS3Key();
		assertEquals(expectedS3Key, key);
	}
	
	@Test
	public void testUploadSecretsToS3() {
		Properties toUpload = new Properties();
		toUpload.put("keyOne", "cipherOne");
		byte[] propertyBytes = SecretBuilderImpl.getPropertiesBytes(toUpload);
		// call under test
		SourceBundle bundle = builder.uploadSecretsToS3(toUpload);
		assertNotNull(bundle);
		assertEquals(s3Bucket, bundle.getBucket());
		assertEquals(expectedS3Key, bundle.getKey());
		verify(mockS3Client).putObject(putObjectRequsetCaptor.capture());
		PutObjectRequest request = putObjectRequsetCaptor.getValue();
		assertNotNull(request);
		assertEquals(s3Bucket, request.getBucketName());
		assertEquals(expectedS3Key, request.getBucketName());
		assertNotNull(request.getMetadata());
		assertEquals(propertyBytes.length, request.getMetadata().getContentLength());
		assertTrue();
	}
	
	@Test
	public void testCreateSecrets() {
		// Call under test
		SourceBundle bundle = builder.createSecrets();
		assertNotNull(bundle);
		assertEquals(s3Bucket, bundle.getBucket());
		assertEquals(expectedS3Key, bundle.getKey());
	}
	
	/**
	 * Convert a byte buffer to a string.
	 * @param byteBuffer
	 * @return
	 */
	public static String byteBufferToString(ByteBuffer byteBuffer) {
		byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Helper to base 64 decode a string.
	 * @param encoded
	 * @return
	 */
	public static String base64Decode(String encoded) {
		try {
			return new String(Base64.getDecoder().decode(encoded.getBytes("UTF-8")), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
