package org.sagebionetworks.stack;

import static org.junit.Assert.*;
import static org.sagebionetworks.stack.Constants.DATABASE_ENGINE_MYSQL;
import static org.sagebionetworks.stack.Constants.DATABASE_ENGINE_MYSQL_VERSION;
import static org.sagebionetworks.stack.Constants.DATABASE_INSTANCE_CLASS_LARGE;
import static org.sagebionetworks.stack.Constants.DATABASE_INSTANCE_CLASS_SMALL;
import static org.sagebionetworks.stack.Constants.EC2_AVAILABILITY_ZONE_US_EAST_1D;
import static org.sagebionetworks.stack.Constants.LICENSE_MODEL_GENERAL_PUBLIC;
import static org.sagebionetworks.stack.Constants.PREFERRED_DATABASE_BACKUP_WINDOW_MIDNIGHT;
import static org.sagebionetworks.stack.Constants.PREFERRED_DATABASE_MAINTENANCE_WINDOW_SUNDAY_NIGHT_PDT;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

public class MySqlDatabaseSetupTest {
	
	Properties inputProperties;
	String id = "aws id";
	String password = "aws password";
	String encryptionKey = "encryptionKey that is long enough";
	String stack = "dev";
	String instance ="A";
	InputConfiguration config;	
	AmazonRDSClient mockClient = null;
	
	MySqlDatabaseSetup databaseSetup;
	
	@Before
	public void before() throws IOException{
		mockClient = Mockito.mock(AmazonRDSClient.class);
		inputProperties = new Properties();
		inputProperties.put(Constants.AWS_ACCESS_KEY, id);
		inputProperties.put(Constants.AWS_SECRET_KEY, password);
		inputProperties.put(Constants.STACK_ENCRYPTION_KEY, encryptionKey);
		inputProperties.put(Constants.STACK, stack);
		inputProperties.put(Constants.INSTANCE, instance);
		config = new InputConfiguration(inputProperties);
		Properties defaults = new Properties();
		defaults.put(Constants.KEY_DEFAULT_ID_GEN_PASSWORD_PLAIN_TEXT, password);
		defaults.put(Constants.KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PLAIN_TEXT, password);
		config.addPropertiesWithPlaintext(defaults);
		// Create the creator
		databaseSetup = new MySqlDatabaseSetup(mockClient, config);
	}
	
	/**
	 * Setup for a production configuration.
	 * @throws IOException
	 */
	private void setupProductionConfig() throws IOException {
		String prodStack = "prod";
		setConfigForStack(prodStack);
	}
	/**
	 * Setup a development configuration.
	 * @throws IOException
	 */
	private void setupDevelopmentConfig() throws IOException {
		String devStack = "dev";
		setConfigForStack(devStack);
	}

	/**
	 * Helper to setup a either a prod or dev configuration.
	 * @param prodStack
	 * @throws IOException
	 */
	private void setConfigForStack(String prodStack) throws IOException {
		inputProperties.put(Constants.STACK, prodStack);
		config = new InputConfiguration(inputProperties);
		Properties defaults = new Properties();
		defaults.put(Constants.KEY_DEFAULT_ID_GEN_PASSWORD_PLAIN_TEXT, password);
		config.addPropertiesWithPlaintext(defaults);
		// Create the creator
		databaseSetup = new MySqlDatabaseSetup(mockClient, config);
	}
	
	@Test
	public void testGetDefaultCreateDBInstanceRequest(){
		// There are the current expected defaults.
		CreateDBInstanceRequest expected = new CreateDBInstanceRequest();
		expected.setAllocatedStorage(new Integer(5));
		expected.setDBInstanceClass(DATABASE_INSTANCE_CLASS_SMALL);
		expected.setEngine(DATABASE_ENGINE_MYSQL);
		expected.setAvailabilityZone(EC2_AVAILABILITY_ZONE_US_EAST_1D);
		expected.setPreferredMaintenanceWindow(PREFERRED_DATABASE_MAINTENANCE_WINDOW_SUNDAY_NIGHT_PDT);
		expected.setBackupRetentionPeriod(new Integer(7));
		expected.setPreferredBackupWindow(PREFERRED_DATABASE_BACKUP_WINDOW_MIDNIGHT);
		expected.setMultiAZ(false);
		expected.setEngineVersion(DATABASE_ENGINE_MYSQL_VERSION);
		expected.setAutoMinorVersionUpgrade(true);
		expected.setLicenseModel(LICENSE_MODEL_GENERAL_PUBLIC);
		CreateDBInstanceRequest request = MySqlDatabaseSetup.getDefaultCreateDBInstanceRequest();
		assertEquals(expected, request);
	}
	
	@Test
	public void testCreateOrGetDatabaseInstanceDoesNotExist(){
		// Amazon notifies us that a database instances does not exist by throwing DBInstanceNotFoundException
		// with a describe methods. This should trigger a create.
		String dbIdentifier = "id-123";
		CreateDBInstanceRequest request = MySqlDatabaseSetup.getDefaultCreateDBInstanceRequest();
		request.setDBInstanceIdentifier(dbIdentifier);
		DescribeDBInstancesRequest describeRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbIdentifier);
		when(mockClient.describeDBInstances(describeRequest)).thenThrow(new DBInstanceNotFoundException("Does not exist"));
		databaseSetup.createOrGetDatabaseInstance(request);
		verify(mockClient, times(1)).createDBInstance(request);
	}
	
	@Test
	public void testCreateOrGetDatabaseInstanceDoesExist(){
		// For this case the database already exists so it should not be created again.
		String dbIdentifier = "id-123";
		CreateDBInstanceRequest request = MySqlDatabaseSetup.getDefaultCreateDBInstanceRequest();
		request.setDBInstanceIdentifier(dbIdentifier);
		DescribeDBInstancesRequest describeRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbIdentifier);
		
		DBInstance instance = new DBInstance();
		instance.setDBInstanceIdentifier(dbIdentifier);
		DescribeDBInstancesResult describeResult = new DescribeDBInstancesResult();
		describeResult.withDBInstances(instance);
		when(mockClient.describeDBInstances(describeRequest)).thenReturn(describeResult);
		DBInstance instanceResult = databaseSetup.createOrGetDatabaseInstance(request);
		assertEquals(instance, instanceResult);
		// Describe once
		verify(mockClient, times(1)).describeDBInstances(describeRequest);
		// Create should not be called this time.
		verify(mockClient, times(0)).createDBInstance(request);
	}
	
	/**
	 * Test that we setup a production database as expected.
	 * @throws IOException
	 */
	@Test
	public void testBuildIdGeneratorCreateDBInstanceRequestProduction() throws IOException{
		setupProductionConfig();
		// Start with the defaults
		CreateDBInstanceRequest expected = MySqlDatabaseSetup.getDefaultCreateDBInstanceRequest();
		expected.setDBName(config.getIdGeneratorDatabaseSchemaName());
		expected.setDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier());
		expected.setAllocatedStorage(new Integer(5));
		expected.setMasterUsername(config.getIdGeneratorDatabaseMasterUsername());
		expected.setMasterUserPassword(config.getIdGeneratorDatabaseMasterPasswordPlaintext());
		expected.withDBSecurityGroups(config.getIdGeneratorDatabaseSecurityGroupName());
		expected.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		// In production the Multi-AZ must be turned on
		expected.setMultiAZ(true);
		// This needs to be a production configuration
		CreateDBInstanceRequest request = databaseSetup.buildIdGeneratorCreateDBInstanceRequest();
		assertEquals(expected, request);
	}



	/**
	 * Test that we setup a non-production database correctly.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testBuildIdGeneratorCreateDBInstanceRequestNonProduction() throws IOException{
		setupDevelopmentConfig();
		// Start with the defaults
		CreateDBInstanceRequest expected = MySqlDatabaseSetup.getDefaultCreateDBInstanceRequest();
		expected.setDBName(config.getIdGeneratorDatabaseSchemaName());
		expected.setDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier());
		expected.setAllocatedStorage(new Integer(5));
		expected.setMasterUsername(config.getIdGeneratorDatabaseMasterUsername());
		expected.setMasterUserPassword(config.getIdGeneratorDatabaseMasterPasswordPlaintext());
		expected.withDBSecurityGroups(config.getIdGeneratorDatabaseSecurityGroupName());
		expected.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		// in non-production Multi-AZ must be turned off.
		expected.setMultiAZ(false);
		// Test against expected
		CreateDBInstanceRequest request = databaseSetup.buildIdGeneratorCreateDBInstanceRequest();
		assertEquals(expected, request);
	}
	
	/**
	 * Test that we setup a production database as expected.
	 * @throws IOException 
	 */
	@Test
	public void testBuildStackInstancesCreateDBInstanceRequestProduction()throws IOException {
		// setup for production.
		setupProductionConfig();
		// Start with the defaults.
		CreateDBInstanceRequest expected = MySqlDatabaseSetup.getDefaultCreateDBInstanceRequest();
		// Production stacks have have different properties
		// Production database need to have a backup replicate ready to go in
		// another zone at all times!
		expected.setMultiAZ(true);
		// The production database should be a large
		expected.setDBInstanceClass(DATABASE_INSTANCE_CLASS_LARGE);
		expected.setDBName(config.getStackInstanceDatabaseSchema());
		expected.setDBInstanceIdentifier(config	.getStackInstanceDatabaseIdentifier());
		expected.setAllocatedStorage(new Integer(50));
		expected.setMasterUsername(config.getStackInstanceDatabaseMasterUser());
		expected.setMasterUserPassword(config.getStackInstanceDatabaseMasterPasswordPlaintext());
		expected.withDBSecurityGroups(config.getStackDatabaseSecurityGroupName());
		expected.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		// Test against expected
		CreateDBInstanceRequest request = databaseSetup.buildStackInstancesCreateDBInstanceRequest();
		assertEquals(expected, request);
	}
	
	/**
	 * Test that we setup a non-production database correctly.
	 * @throws IOException 
	 */
	@Test
	public void testBuildStackInstancesCreateDBInstanceRequestNonProduction() throws IOException{
		// Setup for development
		setupDevelopmentConfig();
		// Start with the defaults.
		CreateDBInstanceRequest expected = MySqlDatabaseSetup.getDefaultCreateDBInstanceRequest();
		// A development database must have Multi-AZ backup replication truned off!!!!
		expected.setMultiAZ(false);
		// The development database must be on a small.
		expected.setDBInstanceClass(DATABASE_INSTANCE_CLASS_SMALL);
		expected.setDBName(config.getStackInstanceDatabaseSchema());
		expected.setDBInstanceIdentifier(config	.getStackInstanceDatabaseIdentifier());
		expected.setAllocatedStorage(new Integer(50));
		expected.setMasterUsername(config.getStackInstanceDatabaseMasterUser());
		expected.setMasterUserPassword(config.getStackInstanceDatabaseMasterPasswordPlaintext());
		expected.withDBSecurityGroups(config.getStackDatabaseSecurityGroupName());
		expected.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		// Test against expected
		CreateDBInstanceRequest request = databaseSetup.buildStackInstancesCreateDBInstanceRequest();
		assertEquals(expected, request);
	}
	

}
