package org.sagebionetworks.stack;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

import static org.sagebionetworks.stack.Constants.*;

/**
 * Setup the MySQL database
 * 
 * @author John
 *
 */
public class MySqlDatabaseSetup {
	
	private static Logger log = Logger.getLogger(MySqlDatabaseSetup.class.getName());
	
	private AmazonRDSClient client;
	private InputConfiguration config;
	
	/**
	 * The IoC constructor.
	 * @param client
	 * @param config
	 */
	public MySqlDatabaseSetup(AmazonRDSClient client, InputConfiguration config) {
		super();
		this.client = client;
		this.config = config;
	}


	/**
	 * Create all database instances if they do not already exist.
	 * 
	 * @param client
	 * @param config
	 */
	public void setupAllDatabaseInstances(){
		// Create the ID generator database
		// Start with the default request
		CreateDBInstanceRequest request = getDefaultCreateDBInstanceRequest();
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
		
		// Get the instances
		DBInstance instance = createOrGetDatabaseInstance(request);
		log.debug("Database instance: ");
		log.debug(instance);

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
		request.setAllocatedStorage(new Integer(5));
		request.setDBInstanceClass(DATABASE_INSTANCE_CLASS_SMALL);
		request.setEngine(DATABASE_ENGINE_MYSQL);
		request.setAvailabilityZone(EC2_AVAILABILITY_ZONE_US_EAST_1D);
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
