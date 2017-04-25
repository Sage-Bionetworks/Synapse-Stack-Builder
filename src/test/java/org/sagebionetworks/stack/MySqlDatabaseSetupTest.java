package org.sagebionetworks.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.stack.Constants.DATABASE_ENGINE_MYSQL;
import static org.sagebionetworks.stack.Constants.DATABASE_ENGINE_MYSQL_VERSION;
import static org.sagebionetworks.stack.Constants.DATABASE_INSTANCE_CLASS_M1_LARGE;
import static org.sagebionetworks.stack.Constants.DATABASE_INSTANCE_CLASS_R3_LARGE;
import static org.sagebionetworks.stack.Constants.DATABASE_INSTANCE_CLASS_M1_SMALL;
import static org.sagebionetworks.stack.Constants.LICENSE_MODEL_GENERAL_PUBLIC;
import static org.sagebionetworks.stack.Constants.PREFERRED_DATABASE_BACKUP_WINDOW_MIDNIGHT;
import static org.sagebionetworks.stack.Constants.PREFERRED_DATABASE_MAINTENANCE_WINDOW_SUNDAY_NIGHT_PDT;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import org.sagebionetworks.factory.MockAmazonClientFactory;

public class MySqlDatabaseSetupTest {
	
	InputConfiguration config;	
	AmazonRDSClient mockClient = null;
	GeneratedResources resources;
	MySqlDatabaseSetup databaseSetup;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	
	@Before
	public void before() throws IOException{
		mockClient = factory.createRDSClient();
		config = TestHelper.createTestConfig("dev");
		resources = new GeneratedResources();
		// Create the creator
		databaseSetup = new MySqlDatabaseSetup(factory, config, resources);
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
	private void setConfigForStack(String stack) throws IOException {
		config = TestHelper.createTestConfig(stack);
		// Create the creator
		databaseSetup = new MySqlDatabaseSetup(factory, config, resources);
	}
	
	@Test
	public void testGetDefaultCreateDBInstanceRequest(){
		// There are the current expected defaults.
		CreateDBInstanceRequest expected = new CreateDBInstanceRequest();
		expected.setAllocatedStorage(new Integer(10));
		expected.setDBInstanceClass(DATABASE_INSTANCE_CLASS_M1_SMALL);
		expected.setEngine(DATABASE_ENGINE_MYSQL);
//		expected.setAvailabilityZone(EC2_AVAILABILITY_ZONE_US_EAST_1D);
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
		expected.setDBInstanceClass(DATABASE_INSTANCE_CLASS_R3_LARGE);
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
		expected.setDBInstanceClass(DATABASE_INSTANCE_CLASS_M1_SMALL);
		expected.setDBName(config.getStackInstanceDatabaseSchema());
		expected.setDBInstanceIdentifier(config	.getStackInstanceDatabaseIdentifier());
		expected.setAllocatedStorage(new Integer(10));
		expected.setMasterUsername(config.getStackInstanceDatabaseMasterUser());
		expected.setMasterUserPassword(config.getStackInstanceDatabaseMasterPasswordPlaintext());
		expected.withDBSecurityGroups(config.getStackDatabaseSecurityGroupName());
		expected.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		// Test against expected
		CreateDBInstanceRequest request = databaseSetup.buildStackInstancesCreateDBInstanceRequest();
		assertEquals(expected, request);
	}
	
	@Test
	public void testBuildStackTableDBInstanceCreateDBInstanceRequestProduction() throws IOException {
		setupProductionConfig();
		CreateDBInstanceRequest expectedReq = MySqlDatabaseSetup.getDefaultCreateDBInstanceRequest();
		expectedReq.setDBName(config.getStackInstanceTablesDBSchema());
		expectedReq.setDBInstanceIdentifier(config.getStackTableDBInstanceDatabaseIdentifier(0));
		expectedReq.setMasterUsername(config.getStackInstanceDatabaseMasterUser());
		expectedReq.setMasterUserPassword(config.getStackInstanceDatabaseMasterPasswordPlaintext());
		expectedReq.withDBSecurityGroups(config.getStackDatabaseSecurityGroupName());
		expectedReq.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		expectedReq.setBackupRetentionPeriod(0);
		expectedReq.setMultiAZ(Boolean.FALSE);
		expectedReq.setAllocatedStorage(250);
		expectedReq.setDBInstanceClass(DATABASE_INSTANCE_CLASS_R3_LARGE);
		CreateDBInstanceRequest request = databaseSetup.buildStackTableDBInstanceCreateDBInstanceRequest(0);
		assertEquals(expectedReq, request);
	}
	
	@Test
	public void testBuildStackTableDBInstanceCreateDBInstanceRequestNonProduction() throws IOException {
		setupDevelopmentConfig();
		CreateDBInstanceRequest expectedReq = MySqlDatabaseSetup.getDefaultCreateDBInstanceRequest();
		expectedReq.setDBName(config.getStackInstanceTablesDBSchema());
		expectedReq.setDBInstanceIdentifier(config.getStackTableDBInstanceDatabaseIdentifier(0));
		expectedReq.setAllocatedStorage(new Integer(10));
		expectedReq.setMasterUsername(config.getStackInstanceDatabaseMasterUser());
		expectedReq.setMasterUserPassword(config.getStackInstanceDatabaseMasterPasswordPlaintext());
		expectedReq.withDBSecurityGroups(config.getStackDatabaseSecurityGroupName());
		expectedReq.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		expectedReq.setBackupRetentionPeriod(0);
		expectedReq.setMultiAZ(Boolean.FALSE);
		expectedReq.setDBInstanceClass(DATABASE_INSTANCE_CLASS_M1_SMALL);
		CreateDBInstanceRequest request = databaseSetup.buildStackTableDBInstanceCreateDBInstanceRequest(0);
		assertEquals(expectedReq, request);
	}
	
	@Test
	public void testBuildIdGeneratorDeleteDBInstanceRequestProduction() throws IOException {
		setupProductionConfig();
		DeleteDBInstanceRequest expectedReq = new DeleteDBInstanceRequest();
		expectedReq.setDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier());
		expectedReq.setSkipFinalSnapshot(Boolean.FALSE);
		expectedReq.setFinalDBSnapshotIdentifier(config.getStack() + config.getStackInstance());
		DeleteDBInstanceRequest req = databaseSetup.buildIdGeneratorDeleteDBInstanceRequest();
		assertEquals(expectedReq, req);
	}

	@Test
	public void testBuildIdGeneratorDeleteDBInstanceRequestNonProduction() throws IOException {
		setupDevelopmentConfig();
		DeleteDBInstanceRequest expectedReq = new DeleteDBInstanceRequest();
		expectedReq.setDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier());
		expectedReq.setSkipFinalSnapshot(Boolean.TRUE);
		DeleteDBInstanceRequest req = databaseSetup.buildIdGeneratorDeleteDBInstanceRequest();
		assertEquals(expectedReq, req);
	}
	
	@Test
	public void testBuildStackInstanceDeleteDBInstanceRequestProduction() throws IOException {
		setupProductionConfig();
		DeleteDBInstanceRequest expectedReq = new DeleteDBInstanceRequest();
		expectedReq.setDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier());
		expectedReq.setSkipFinalSnapshot(Boolean.FALSE);
		expectedReq.setFinalDBSnapshotIdentifier(config.getStack() + config.getStackInstance());
		DeleteDBInstanceRequest req = databaseSetup.buildStackInstanceDeleteDBInstanceRequest();
		assertEquals(expectedReq, req);
	}

	@Test
	public void testBuildStackInstanceDeleteDBInstanceRequestNonProduction() throws IOException {
		setupDevelopmentConfig();
		DeleteDBInstanceRequest expectedReq = new DeleteDBInstanceRequest();
		expectedReq.setDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier());
		expectedReq.setSkipFinalSnapshot(Boolean.TRUE);
		DeleteDBInstanceRequest req = databaseSetup.buildStackInstanceDeleteDBInstanceRequest();
		assertEquals(expectedReq, req);
	}
	
	@Test
	public void testbuildIdGeneratorDescribeDBInstanceRequest() {
		DescribeDBInstancesRequest expectedReq = new DescribeDBInstancesRequest().withDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier());
		DescribeDBInstancesRequest req = databaseSetup.buildIdGeneratorDescribeDBInstanceRequest();
		assertEquals(expectedReq, req);
	}
	
	@Test
	public void testbuildStackInstanceDescribeDBInstanceRequest() {
		DescribeDBInstancesRequest expectedReq = new DescribeDBInstancesRequest().withDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier());
		DescribeDBInstancesRequest req = databaseSetup.buildStackInstanceDescribeDBInstanceRequest();
		assertEquals(expectedReq, req);
	}
	
	@Test
	public void testbuildStackTableDBInstanceDescribeDBInstanceRequest() {
		int numTableInstances = Integer.parseInt(config.getNumberTableInstances());
		for (int i = 0; i < numTableInstances; i++) {
			DescribeDBInstancesRequest expectedReq = new DescribeDBInstancesRequest().withDBInstanceIdentifier(config.getStackTableDBInstanceDatabaseIdentifier(i));
			DescribeDBInstancesRequest req = databaseSetup.buildStackTableDBInstanceDescribeDBInstanceRequest(i);
			assertEquals(expectedReq, req);
		}
	}
	
	@Test
	public void testDeleteDatabaseInstanceDoesNotExist() {
		// Should just be a no-op
		DeleteDBInstanceRequest req = new DeleteDBInstanceRequest();
		when(mockClient.deleteDBInstance(req)).thenThrow(new DBInstanceNotFoundException("Database instance does not exist"));
		DBInstance inst = databaseSetup.deleteDatabaseInstance(req);
		assertNull(inst);
	}
	
	@Test
	public void testDeleteDatabaseInstanceDoesExist() {
		DeleteDBInstanceRequest req = new DeleteDBInstanceRequest();
		req.setDBInstanceIdentifier("someDB");
		when(mockClient.deleteDBInstance(req)).thenReturn(new DBInstance().withDBInstanceIdentifier("someDB"));
		DBInstance inst = databaseSetup.deleteDatabaseInstance(req);
		assertEquals(inst.getDBInstanceIdentifier(), "someDB");
	}
}
