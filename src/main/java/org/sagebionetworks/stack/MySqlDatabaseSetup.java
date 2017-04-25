package org.sagebionetworks.stack;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import java.util.ArrayList;
import java.util.List;

import static org.sagebionetworks.stack.Constants.*;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 * Setup the MySQL database
 * 
 * @author John
 *
 */
public class MySqlDatabaseSetup implements ResourceProcessor {
	
	private static Logger log = Logger.getLogger(MySqlDatabaseSetup.class.getName());
	
	private AmazonRDSClient client;
	private InputConfiguration config;
	private GeneratedResources resources;
	/**
	 * The IoC constructor.
	 * @param client
	 * @param config
	 */
	public MySqlDatabaseSetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		this.initialize(factory, config, resources);
	}

	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		if(factory == null) throw new IllegalArgumentException("AmazonClientFactory cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		this.client = factory.createRDSClient();
		this.config = config;
		this.resources = resources;
	}
	
	public void setupResources() {
		this.setupAllDatabaseInstances();
	}
	
	public void teardownResources() {
		deleteStackInstanceDatabaseInstance();
	}
	
	public void describeResources() {
		DescribeDBInstancesRequest req;
		DescribeDBInstancesResult res;
		
		req = buildIdGeneratorDescribeDBInstanceRequest();
		res = client.describeDBInstances(req);
		if ((res.getDBInstances() != null) && (res.getDBInstances().size() == 1)) {
			resources.setIdGeneratorDatabase(res.getDBInstances().get(0));
		}
		req = buildStackInstanceDescribeDBInstanceRequest();
		res = client.describeDBInstances(req);
		if ((res.getDBInstances() != null) && (res.getDBInstances().size() == 1)) {
			resources.setStackInstancesDatabase(res.getDBInstances().get(0));
		}
		//	TODO: Describe table DB instances
		List<DBInstance> descDbInstResults = new ArrayList<DBInstance>();
		int numTableInstances = Integer.parseInt(this.config.getNumberTableInstances());
		for (int i = 0; i < numTableInstances; i++) {
			req = buildStackTableDBInstanceDescribeDBInstanceRequest(i);
			res = client.describeDBInstances(req);
			if ((res.getDBInstances() != null) && (res.getDBInstances().size() == 1)) {
				descDbInstResults.add(res.getDBInstances().get(0));
			}
		}
		resources.setStackInstanceTablesDatabases(descDbInstResults);
	}

	/**
	 * Create all database instances if they do not already exist.
	 * 
	 * @param client
	 * @param config
	 */
	public void setupAllDatabaseInstances(){
		// Build the request to create the ID generator database.
		CreateDBInstanceRequest request = buildIdGeneratorCreateDBInstanceRequest();
		
		// Get the instances
		DBInstance idGenInstance = createOrGetDatabaseInstance(request);
		log.debug("Database instance: ");
		log.debug(idGenInstance);

		// Now create the stack instance database
		request = buildStackInstancesCreateDBInstanceRequest();
		
		// Get the instances
		DBInstance stackInstance = createOrGetDatabaseInstance(request);
		log.debug("Database instance: ");
		log.debug(stackInstance);
		
		
		// Create the table instances databases
		int numTableInstances = Integer.parseInt(config.getNumberTableInstances());
		List<DBInstance> stackTableInstances = new ArrayList<DBInstance>();
		for (int inst = 0; inst < numTableInstances; inst++) {
			request = buildStackTableDBInstanceCreateDBInstanceRequest(inst);
			DBInstance dbInst = createOrGetDatabaseInstance(request);
			log.debug("Database instance: " + dbInst);
			stackTableInstances.add(dbInst);
		}
		// Wait for both to be created
		idGenInstance = waitForDatabase(idGenInstance);
		stackInstance = waitForDatabase(stackInstance);
		
		List<DBInstance> updStackTableInstances = new ArrayList<DBInstance>();
		for (DBInstance ti: stackTableInstances) {
			ti = waitForDatabase(ti);
			updStackTableInstances.add(ti);
		}
		
		resources.setIdGeneratorDatabase(idGenInstance);
		resources.setStackInstancesDatabase(stackInstance);
		resources.setStackInstanceTablesDatabases(updStackTableInstances);
	}
	
	/**
	 * Wait for a database to be available
	 * @param stackInstance
	 */
	public DBInstance waitForDatabase(DBInstance stackInstance) {
		String status = null;
		DBInstance instance = null;
		do{
			DescribeDBInstancesResult result = client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(stackInstance.getDBInstanceIdentifier()));
			instance = result.getDBInstances().get(0);
			status = instance.getDBInstanceStatus();
			log.info(String.format("Waiting for database: instance: %1$s status: %2$s ", stackInstance.getDBInstanceIdentifier(), status));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}while(!"available".equals(status));
		return instance;
	}
	

	/*
	/*
	 * Delete  stack instance database
	 */
	public void deleteStackInstanceDatabaseInstance() {
		// Build the request to delete the stack instance database
		DeleteDBInstanceRequest req = buildStackInstanceDeleteDBInstanceRequest();
		DBInstance inst = deleteDatabaseInstance(req);
	}
	

	public DBInstance deleteDatabaseInstance(DeleteDBInstanceRequest req) {
		DBInstance inst = null;
		try {
			inst = client.deleteDBInstance(req);
		} catch (DBInstanceNotFoundException e) {
			log.debug("Stack instance database not found!!!");
		} finally {
			if (inst != null) {
				log.debug("Stack instance database status:" + inst.getDBInstanceStatus());
			}
			return inst;
		}
	}
	/**
	 * Build up the CreateDBInstanceRequest used to create the ID Generator database.
	 * 
	 * @return
	 */
	CreateDBInstanceRequest buildIdGeneratorCreateDBInstanceRequest() {
		CreateDBInstanceRequest request = getDefaultCreateDBInstanceRequest();
		// Is this a production stack?
		if(config.isProductionStack()){
			// Production database need to have a backup replicate ready to go in another
			// zone at all times!
			request.setMultiAZ(true);
		}else{
			// This is not a production stack so we do not want to incur the extra cost of
			// a backup replicate database in another zone.
			request.setMultiAZ(false);
		}
		// This will be the schema name.
		request.setDBName(config.getIdGeneratorDatabaseSchemaName());
		request.setDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier());
		// This database only needs a small amount of disk space so this is set the minimum of 5 GB
		request.setAllocatedStorage(new Integer(5));
		request.setMasterUsername(config.getIdGeneratorDatabaseMasterUsername());
		request.setMasterUserPassword(config.getIdGeneratorDatabaseMasterPasswordPlaintext());
		// The security group
		request.withDBSecurityGroups(config.getIdGeneratorDatabaseSecurityGroupName());
		// The parameters.
		request.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		return request;
	}
	
	

	/**
	 * Build up the CreateDBInstanceRequest used for the stack-instance database.
	 * 
	 * @return
	 */
	CreateDBInstanceRequest buildStackInstancesCreateDBInstanceRequest() {
		CreateDBInstanceRequest request = getDefaultCreateDBInstanceRequest();
		// Production stacks have have different properties
		if(config.isProductionStack()){
			// Production database need to have a backup replicate ready to go in another
			// zone at all times!
			request.setMultiAZ(true);
			// The production database should be a large
			request.setDBInstanceClass(DATABASE_INSTANCE_CLASS_R3_LARGE);
			// The size of the database should be 50GB
			request.setAllocatedStorage(new Integer(50));
		}else{
			// This is not a production stack so we do not want to incur the extra cost of
			// a backup replicate database in another zone.
			request.setMultiAZ(false);
			// All non-production databases should be small
			request.setDBInstanceClass(DATABASE_INSTANCE_CLASS_M1_SMALL);
			// The size of the database should be 10GB
			request.setAllocatedStorage(new Integer(10));
		}
		// This will be the schema name.
		request.setDBName(config.getStackInstanceDatabaseSchema());
		request.setDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier());
		request.setMasterUsername(config.getStackInstanceDatabaseMasterUser());
		request.setMasterUserPassword(config.getStackInstanceDatabaseMasterPasswordPlaintext());
		// The security group
		request.withDBSecurityGroups(config.getStackDatabaseSecurityGroupName());
		// The parameters.
		request.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		// if this is a production stack
		return request;
	}
	
	CreateDBInstanceRequest buildStackTableDBInstanceCreateDBInstanceRequest(int instNum) {
		CreateDBInstanceRequest request = getDefaultCreateDBInstanceRequest();
		if (config.isProductionStack()) {
			request.setDBInstanceClass(DATABASE_INSTANCE_CLASS_R3_LARGE);
			request.setAllocatedStorage(new Integer(250));
		} else {
			request.setDBInstanceClass(DATABASE_INSTANCE_CLASS_M1_SMALL);
			request.setAllocatedStorage(new Integer(10));
		}
		// This will be the schema name.
		request.setDBName(config.getStackInstanceTablesDBSchema());
		request.setDBInstanceIdentifier(config.getStackTableDBInstanceDatabaseIdentifier(instNum));
		request.setMasterUsername(config.getStackInstanceDatabaseMasterUser());
		request.setMasterUserPassword(config.getStackInstanceDatabaseMasterPasswordPlaintext());
		request.setBackupRetentionPeriod(0);
		// The security group
		request.withDBSecurityGroups(config.getStackDatabaseSecurityGroupName());
		// The parameters.
		request.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		// if this is a production stack
		return request;
	}

	/*
	 * NOTE: Do not call unless deleting shared resources!!!
	 */
	DeleteDBInstanceRequest buildIdGeneratorDeleteDBInstanceRequest() {
		DeleteDBInstanceRequest req = new DeleteDBInstanceRequest();
		req.setDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier());
		if (config.isProductionStack()) {
			req.setSkipFinalSnapshot(Boolean.FALSE);
			// TODO: Come up with better name for final snapshot
			req.setFinalDBSnapshotIdentifier(config.getStack() + config.getStackInstance());
		} else {
			req.setSkipFinalSnapshot(Boolean.TRUE);
		}
		return req;
		
	}

	DeleteDBInstanceRequest buildStackInstanceDeleteDBInstanceRequest() {
		DeleteDBInstanceRequest req = new DeleteDBInstanceRequest();
		req.setDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier());
		if (config.isProductionStack()) {
			req.setSkipFinalSnapshot(Boolean.FALSE);
			// TODO: Come up with better name for final snapshot
			req.setFinalDBSnapshotIdentifier(config.getStack() + config.getStackInstance());
		} else {
			req.setSkipFinalSnapshot(Boolean.TRUE);
		}
		return req;
		
	}

	DescribeDBInstancesRequest buildIdGeneratorDescribeDBInstanceRequest() {
		DescribeDBInstancesRequest req = new DescribeDBInstancesRequest();
		req.setDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier());
		return req;
	}

	DescribeDBInstancesRequest buildStackInstanceDescribeDBInstanceRequest() {
		DescribeDBInstancesRequest req = new DescribeDBInstancesRequest();
		req.setDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier());
		return req;
	}
	
	DescribeDBInstancesRequest buildStackTableDBInstanceDescribeDBInstanceRequest(int inst) {
		DescribeDBInstancesRequest req = new DescribeDBInstancesRequest();
		req.setDBInstanceIdentifier(config.getStackTableDBInstanceDatabaseIdentifier(inst));
		return req;
	}
	/**
	 * If this database instances does not already exist it will be created, otherwise the instance information will be returned.
	 * 
	 * @param request
	 * @return
	 */
	DBInstance createOrGetDatabaseInstance(CreateDBInstanceRequest request){
		// First query for the instance
		try{
			DescribeDBInstancesResult result = client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(request.getDBInstanceIdentifier()));
			if(result.getDBInstances() == null || result.getDBInstances().size() != 1) throw new IllegalStateException("Did not find exactly one database instances with the identifier: "+request.getDBInstanceIdentifier());
			log.debug("Database: "+request.getDBInstanceIdentifier()+" already exists");
			return result.getDBInstances().get(0);
		}catch(DBInstanceNotFoundException e){
			// This database does not exist to create it
			// Create the database.
			log.debug("Creating database...");
			log.debug(request);
			return client.createDBInstance(request);
		}
	}
	
	/**
	 * Fill out a CreateDBInstanceRequest will all of the default values.
	 * 
	 * @return
	 */
	public static CreateDBInstanceRequest getDefaultCreateDBInstanceRequest(){
		CreateDBInstanceRequest request = new CreateDBInstanceRequest();
		request.setAllocatedStorage(new Integer(10));
		request.setDBInstanceClass(DATABASE_INSTANCE_CLASS_M1_SMALL);
		request.setEngine(DATABASE_ENGINE_MYSQL);
//		request.setAvailabilityZone(EC2_AVAILABILITY_ZONE_US_EAST_1D);
		request.setPreferredMaintenanceWindow(PREFERRED_DATABASE_MAINTENANCE_WINDOW_SUNDAY_NIGHT_PDT);
		request.setBackupRetentionPeriod(new Integer(7));
		request.setPreferredBackupWindow(PREFERRED_DATABASE_BACKUP_WINDOW_MIDNIGHT);
		request.setMultiAZ(false);
		request.setEngineVersion(DATABASE_ENGINE_MYSQL_VERSION);
		request.setAutoMinorVersionUpgrade(true);
		request.setLicenseModel(LICENSE_MODEL_GENERAL_PUBLIC);
		return request;
	}

}
