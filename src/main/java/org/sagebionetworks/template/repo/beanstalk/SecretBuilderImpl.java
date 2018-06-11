package org.sagebionetworks.template.repo.beanstalk;

import static org.sagebionetworks.template.Constants.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.StringJoiner;

import org.sagebionetworks.template.Configuration;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.google.inject.Inject;

public class SecretBuilderImpl implements SecretBuilder {
	
	private static final String DOT = "\\.";
	private static final String SAGEBIONETWORKS = "sagebionetworks";
	private static final String ORG = "org";
	public static final String UTF_8 = "UTF-8";
	
	Configuration config;
	AWSSecretsManager secretManager;
	AWSKMS keyManager;
	
	
	@Inject
	public SecretBuilderImpl(Configuration config, AWSSecretsManager secretManager, AWSKMS keyManager) {
		super();
		this.config = config;
		this.config.initializeWithDefaults(DEFAULT_REPO_PROPERTIES);
		this.secretManager = secretManager;
		this.keyManager = keyManager;
	}

	@Override
	public Secret[] createSecrets() {
		// Load the secret names
		String[] secretNames = config.getComaSeparatedProperty(PROPERTY_KEY_SECRET_KEYS_CSV);
		Secret[] secrets = new Secret[secretNames.length];
		for (int i = 0; i < secretNames.length; i++) {
			secrets[i] = createSecret(secretNames[i]);
		}
		return secrets;
	}

	/**
	 * A secret is created by getting the plaintext value from the SecretManager and
	 * then encrypting the value using the stack's CMK.
	 * 
	 * @param string
	 * @return
	 */
	Secret createSecret(String key) {
		String plaintextValue = getSecretValue(key);
		// Encrypt the value using the stack's key
		EncryptResult encryptResult = keyManager.encrypt(new EncryptRequest()
				.withPlaintext(stringToByteBuffer(plaintextValue)).withKeyId(getCMKAlias()));
		String encryptedValue = base64Encode(encryptResult.getCiphertextBlob());
		String parameterName = SecretBuilderImpl.createParameterName(key);
		return new Secret().withEncryptedValue(encryptedValue).withPropertyKey(key).withParameterName(parameterName);
	}

	/**
	 * Get plaintext value for the given secret.
	 * 
	 * @param key
	 * @return
	 */
	String getSecretValue(String key) {
		String masterKey = getMasterSecretKey(key);
		// Fetch the master plaintext value for this keys
		GetSecretValueResult secretResult = secretManager.getSecretValue(new GetSecretValueRequest().withSecretId(masterKey));
		String plaintextValue = secretResult.getSecretString();
		if(plaintextValue == null) {
			throw new IllegalArgumentException("Secret string is null for: "+masterKey);
		}
		return plaintextValue;
	}

	/**
	 * The master secret key is '<stack>.key'
	 * 
	 * @param key
	 * @return
	 */
	String getMasterSecretKey(String key) {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		StringJoiner joiner = new StringJoiner(".");
		joiner.add(stack);
		joiner.add(key);
		return joiner.toString();
	}
	
	/**
	 * The Customer Master Key alias is 'synapse/<stack>/<instance>/cmk'
	 * 
	 * @return
	 */
	@Override
	public String getCMKAlias() {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		StringJoiner joiner = new StringJoiner("/");
		joiner.add("alias/synapse");
		joiner.add(stack);
		joiner.add(instance);
		joiner.add("cmk");
		return joiner.toString();
	}
	
	/**
	 * Convert a string to ByteBuffer.
	 * 
	 * @param source
	 * @return
	 */
	public static ByteBuffer stringToByteBuffer(String source) {
		try {
			return ByteBuffer.wrap(source.getBytes(UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Base 64 encode the bytes from the given buffer.
	 * 
	 * 
	 * @param buffer
	 * @return
	 */
	public static String base64Encode(ByteBuffer buffer) {
		// read the remaining bytes from the buffer
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		try {
			return new String(Base64.getEncoder().encode(bytes), UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Create a parameter name from a property key
	 * @param key
	 * @return
	 */
	public static String createParameterName(String key) {
		String[] split = key.split(DOT);
		StringBuilder builder = new StringBuilder();
		for(String part: split) {
			if(!ORG.equals(part) && !SAGEBIONETWORKS.equals(part)) {
				builder.append(part.substring(0, 1).toUpperCase());
				builder.append(part.substring(1));
			}
		}
		return builder.toString();
	}

	@Override
	public String getRepositoryDatabasePassword() {
		return getSecretValue(PROPERTY_KEY_REPOSITORY_DATABASE_PASSWORD);
	}

	@Override
	public String getIdGeneratorPassword() {
		return getSecretValue(PROPERTY_KEY_ID_GENERATOR_DATABASE_PASSWORD);
	}

}
