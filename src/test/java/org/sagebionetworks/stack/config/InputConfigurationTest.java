package org.sagebionetworks.stack.config;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import static org.sagebionetworks.stack.Constants.*;

import org.sagebionetworks.stack.Constants;
import org.sagebionetworks.stack.TestHelper;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.util.EncryptionUtils;

import com.amazonaws.auth.AWSCredentials;

/**
 * Test for the Configuration class.
 * @author John
 *
 */
public class InputConfigurationTest {
	
	Properties inputProperties;
	String id = "aws id";
	String password = "aws password";
	String encryptionKey = "encryption Key that is long enough to work";
	String stack = "stack";
	String instance ="instance";
	String portalBeanstalkNumber = "1001";
	String bridgeBeanstalkNumber = "2002";
	String numberTableInstances = "2";
	
	@Before
	public void before(){
		inputProperties = TestHelper.createInputProperties("stack");
		inputProperties.put(AWS_ACCESS_KEY, id);
		inputProperties.put(AWS_SECRET_KEY, password);
		inputProperties.put(STACK_ENCRYPTION_KEY, encryptionKey);
		inputProperties.put(STACK, stack);
		inputProperties.put(INSTANCE, instance);
		inputProperties.put(PORTAL_BEANSTALK_NUMBER, portalBeanstalkNumber);
		inputProperties.put(BRIDGE_BEANSTALK_NUMBER, bridgeBeanstalkNumber);
		inputProperties.put(NUMBER_TABLE_INSTANCES, numberTableInstances);
	}
	
	@Test
	public void testLoad() throws IOException{
		Properties props = InputConfiguration.loadRequired();
		assertNotNull(props);
		props.containsKey(AWS_ACCESS_KEY);
		props.containsKey(AWS_SECRET_KEY);
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
		InputConfiguration.validateProperties(required, loaded);
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
		InputConfiguration.validateProperties(required, loaded);
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
		InputConfiguration.validateProperties(required, loaded);
	}
	
	@Test
	public void testConfig() throws IOException{
		// Load from the properties 
		InputConfiguration config = new InputConfiguration(inputProperties);
		AWSCredentials creds = config.getAWSCredentials();
		assertNotNull(creds);
		assertEquals(id, creds.getAWSAccessKeyId());
		assertEquals(password, creds.getAWSSecretKey());
		assertEquals(encryptionKey, config.getEncryptionKey());
		assertEquals(stack, config.getStack());
		assertEquals(instance, config.getStackInstance());
		assertEquals(portalBeanstalkNumber, config.getPortalBeanstalkNumber());
		assertEquals(bridgeBeanstalkNumber, config.getBridgeBeanstalkNumber());
		assertEquals(numberTableInstances, config.getNumberTableInstances());
	}
	
	@Test
	public void testStackInstanceNames() throws IOException{
		// Load from the properties 
		InputConfiguration config = new InputConfiguration(inputProperties);
		assertEquals(stack+"-default.sagebase.org", config.getDefaultS3BucketName());
		assertEquals(stack+"-default.properties", config.getDefaultPropertiesFileName());
		assertEquals("elastic-beanstalk-"+stack+"-"+instance, config.getElasticSecurityGroupName());
		assertEquals("All elastic beanstalk instances of stack:'"+stack+"' instance:'"+instance+"' belong to this EC2 security group", config.getElasticSecurityGroupDescription());
		assertEquals("mysql5-5-"+stack+"-params", config.getDatabaseParameterGroupName());
		assertEquals("Custom MySQL 5.5 database parameters (including slow query log enabled) used by all database instances belonging to stack: "+stack, config.getDatabaseParameterGroupDescription());
		// Id gen database
		String expectedIdGenIdentifier = stack+"-id-generator-db";
		assertEquals(expectedIdGenIdentifier, config.getIdGeneratorDatabaseIdentifier());
		assertEquals(stack+"idgen", config.getIdGeneratorDatabaseSchemaName());
		assertEquals(stack+"idgenuser", config.getIdGeneratorDatabaseMasterUsername());
		// Stack database
		String expectedStackDBIdentifier = stack+"-"+instance+"-db";
		assertEquals(expectedStackDBIdentifier, config.getStackInstanceDatabaseIdentifier());
		assertEquals(stack+instance, config.getStackInstanceDatabaseSchema());
		assertEquals(stack+instance+"user", config.getStackInstanceDatabaseMasterUser());
		// Table instance databases
		int numTableInstances = Integer.parseInt(config.getNumberTableInstances());
		for (int instNum = 0; instNum < numTableInstances; instNum++) {
			String expectedStackTableInstanceIdentifier = stack + "-" + instance + "-table-" + instNum;
			assertEquals(expectedStackTableInstanceIdentifier, config.getStackTableInstanceDBIdentifier() + instNum);
			assertEquals(stack+instance, config.getStackTableInstanceDBSchema());
			assertEquals(stack+instance+"user", config.getStackTableInstanceDBMasterUser());
		}
		// The database security groups
		assertEquals(expectedIdGenIdentifier+"-security-group", config.getIdGeneratorDatabaseSecurityGroupName());
		assertEquals("The database security group used by the "+expectedIdGenIdentifier+".", config.getIdGeneratorDatabaseSecurityGroupDescription());
		assertEquals(expectedStackDBIdentifier+"-security-group", config.getStackDatabaseSecurityGroupName());
		assertEquals("The database security group used by the "+expectedStackDBIdentifier+".", config.getStackDatabaseSecurityGroupDescription());
		// the alert topic
		assertEquals(stack+"-"+instance+"-RDS-Alert", config.getRDSAlertTopicName());
		// Main file S3 bucket
		assertEquals(stack+"data.sagebase.org", config.getMainFileS3BucketName());
	}
	
	
	
	@Test
	public void testAddPropertiesWithPlaintext() throws IOException{
		InputConfiguration config = new InputConfiguration(inputProperties);
		// These are properties that we want encrypted
		Properties props = new Properties();
		String plainTextKey = "key.one."+PLAIN_TEXT_SUFFIX;
		String encryptedKey = "key.one."+ENCRYPTED_SUFFIX;
		String plainText = "Please encrypte me!";
		props.put(plainTextKey, plainText);
		props.put("key.two", "Do not encrypt me!");
		// Add the properties
		config.addPropertiesWithPlaintext(props);
		
		// Make sure the original properties are there
		assertEquals(plainText, config.validateAndGetProperty(plainTextKey));
		assertEquals("Do not encrypt me!", config.validateAndGetProperty("key.two"));
		
		// Now check the expected encrypted key
		String expectedCipherText = EncryptionUtils.encryptString(config.getEncryptionKey(), plainText);
		assertEquals(expectedCipherText, config.validateAndGetProperty(encryptedKey));
	}
	
	@Test
	public void testStackPasswords() throws IOException{
		// Load from the properties 
		InputConfiguration config = new InputConfiguration(inputProperties);
		Properties passwords = new Properties();
		String plainTextPassword = "password";
		String plainTextPassword2 = "password2";
		String expectedCipherText = EncryptionUtils.encryptString(config.getEncryptionKey(), plainTextPassword);
		String expectedCipherText2 = EncryptionUtils.encryptString(config.getEncryptionKey(), plainTextPassword2);
		passwords.put(KEY_DEFAULT_ID_GEN_PASSWORD_PLAIN_TEXT, plainTextPassword);
		passwords.put(Constants.KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PLAIN_TEXT, plainTextPassword2);
		config.addPropertiesWithPlaintext(passwords);
		
		// Make sure we can get both the plain text version and the encrypted versions
		assertEquals(plainTextPassword, config.getIdGeneratorDatabaseMasterPasswordPlaintext());
		assertEquals(expectedCipherText, config.validateAndGetProperty(KEY_DEFAULT_ID_GEN_PASSWORD_ENCRYPTED));
		assertEquals(plainTextPassword2, config.getStackInstanceDatabaseMasterPasswordPlaintext());
		assertEquals(expectedCipherText2, config.validateAndGetProperty(KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_ENCRYPTED));
	}
	
	@Test
	public void testIsProdcution() throws IOException{
		// This is a production stack
		inputProperties.put(STACK, "prod");
		InputConfiguration config = new InputConfiguration(inputProperties);
		assertTrue(config.isProductionStack());
		// Also a prod stack
		inputProperties.put(STACK, "PROD");
		config = new InputConfiguration(inputProperties);
		assertTrue(config.isProductionStack());
		
		// A dev stack in NOT a production stack.
		inputProperties.put(STACK, "dev");
		config = new InputConfiguration(inputProperties);
		assertFalse(config.isProductionStack());
		
	}
}
