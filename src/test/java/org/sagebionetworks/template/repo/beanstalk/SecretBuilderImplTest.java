package org.sagebionetworks.template.repo.beanstalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.template.Constants.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;

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
	
	@Captor
	ArgumentCaptor<GetSecretValueRequest> secretRequestCaptor;
	@Captor
	ArgumentCaptor<EncryptRequest> encryptRequestCaptor;
	
	String stack;
	String instance;
	SecretBuilderImpl builder;
	
	String key;
	String secretString;
	String encryptedSecretValue;
	ByteBuffer secretBuffer;
	
	@Before
	public void before() {
		stack = "dev";
		instance = "299";
		 key = "org.sagebionetworks.some.key";
		 
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_SECRET_KEYS_CSV)).thenReturn(new String[] {key});
		
		
		builder = new SecretBuilderImpl(mockConfig, mockSecretManager, mockKeyManager);
		
		secretString = "super secret";
		when(mockSecretManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(new GetSecretValueResult().withSecretString(secretString));
		encryptedSecretValue = "pretend this is encrypted";
		secretBuffer = SecretBuilderImpl.stringToByteBuffer(encryptedSecretValue);
		when(mockKeyManager.encrypt(any(EncryptRequest.class))).thenReturn(new EncryptResult().withCiphertextBlob(secretBuffer));
	
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
		Secret secret = builder.createSecret(key);
		assertNotNull(secret);
		assertEquals(encryptedSecretValue, base64Decode(secret.getEncryptedValue()));
		assertEquals("SomeKey", secret.getParameterName());
		assertEquals("org.sagebionetworks.some.key", secret.getPropertyKey());
		verify(mockKeyManager).encrypt(encryptRequestCaptor.capture());
		assertEquals("alias/synapse/dev/299/cmk", encryptRequestCaptor.getValue().getKeyId());
		assertEquals(secretString, byteBufferToString(encryptRequestCaptor.getValue().getPlaintext()));
	}
	
	@Test
	public void testCreateSecrets() {
		// Call under test
		Secret[] secrets = builder.createSecrets();
		assertNotNull(secrets);
		assertEquals(1, secrets.length);
		assertEquals("SomeKey", secrets[0].getParameterName());
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
