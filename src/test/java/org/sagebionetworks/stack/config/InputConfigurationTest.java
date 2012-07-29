package org.sagebionetworks.stack.config;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.stack.Constants;
import org.sagebionetworks.stack.config.InputConfiguration;

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
	String encryptionKey = "encryptionKey";
	String stack = "stack";
	String instance ="instance";
	
	@Before
	public void before(){
		inputProperties = new Properties();
		inputProperties.put(Constants.AWS_ACCESS_KEY, id);
		inputProperties.put(Constants.AWS_SECRET_KEY, password);
		inputProperties.put(Constants.STACK_ENCRYPTION_KEY, encryptionKey);
		inputProperties.put(Constants.STACK, stack);
		inputProperties.put(Constants.INSTANCE, instance);
	}
	
	@Test
	public void testLoad() throws IOException{
		Properties props = InputConfiguration.loadRequired();
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
	}
	
	@Test
	public void testStackInstanceNames() throws IOException{
		// Load from the properties 
		InputConfiguration config = new InputConfiguration(inputProperties);
		assertEquals(stack+"-default", config.getDefaultS3BucketName());
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
		assertEquals(expectedStackDBIdentifier, config.getStackDatabaseIdentifier());
		assertEquals(stack+instance, config.getStackDatabaseSchema());
		assertEquals(stack+instance+"user", config.getStackDatabaseMasterUser());
		// The database security groups
		assertEquals(expectedIdGenIdentifier+"-security-group", config.getIdGeneratorDatabaseSecurityGroupName());
		assertEquals("The database security group used by the "+expectedIdGenIdentifier+".", config.getIdGeneratorDatabaseSecurityGroupDescription());
		assertEquals(expectedStackDBIdentifier+"-security-group", config.getStackDatabaseSecurityGroupName());
		assertEquals("The database security group used by the "+expectedStackDBIdentifier+".", config.getStackDatabaseSecurityGroupDescription());
	}
}
