package org.sagebionetworks;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;
import org.sagebionetworks.stack.Configuration;
import org.sagebionetworks.stack.Constants;

import com.amazonaws.auth.AWSCredentials;

/**
 * Test for the Configuration class.
 * @author John
 *
 */
public class ConfigurationTest {
	
	@Test
	public void testLoad() throws IOException{
		Properties props = Configuration.loadRequired();
		assertNotNull(props);
		props.containsKey(Constants.AWS_ACCESS_KEY);
		props.containsKey(Constants.AWS_SECRET_KEY);
	}
	
	/**
	 * Required missing
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePropertiesMissing(){
		Properties required = new Properties();
		Properties loaded = new Properties();
		required.put("key.one", "");
		loaded.put("key.two", "not null");
		Configuration.validateProperties(required, loaded);
	}
	
	/**
	 * Required empty
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePropertiesEmpty(){
		Properties required = new Properties();
		Properties loaded = new Properties();
		required.put("key.one", "");
		loaded.put("key.one", "");
		Configuration.validateProperties(required, loaded);
	}

	/**
	 * Required as expected
	 */
	@Test
	public void testValidateProperties(){
		Properties required = new Properties();
		Properties loaded = new Properties();
		required.put("key.one", "");
		loaded.put("key.one", "not null");
		loaded.put("not.required", "I am not required");
		Configuration.validateProperties(required, loaded);
	}

	@Test
	public void testConfig() throws IOException{
		Properties loaded = new Properties();
		String id = "aws id";
		String password = "aws password";
		String encryptionKey = "encryptionKey";
		String stack = "stack";
		String instance ="instance";
		loaded.put(Constants.AWS_ACCESS_KEY, id);
		loaded.put(Constants.AWS_SECRET_KEY, password);
		loaded.put(Constants.STACK_ENCRYPTION_KEY, encryptionKey);
		loaded.put(Constants.STACK, stack);
		loaded.put(Constants.INSTANCE, instance);
		// Load from the properties 
		Configuration config = new Configuration(loaded);
		AWSCredentials creds = config.getAWSCredentials();
		assertNotNull(creds);
		assertEquals(id, creds.getAWSAccessKeyId());
		assertEquals(password, creds.getAWSSecretKey());
		assertEquals(encryptionKey, config.getEncryptionKey());
		assertEquals(stack, config.getStack());
		assertEquals(instance, config.getStackInstance());
	}
}
