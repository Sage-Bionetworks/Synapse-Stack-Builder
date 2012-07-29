package org.sagebionetworks.stack;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceAlreadyExistsException;

/**
 * Set the ID generator as needed.
 * 
 * @author John
 *
 */
public class IdGeneratorSetup {
	

	
	/**
	 * Create everything need for the Id generator database.
	 * @param config
	 * @return
	 */
	public static DatabaseInfo createIdGeneratorDatabase(AmazonRDSClient client, InputConfiguration config){
		if(client == null) throw new IllegalArgumentException("AmazonRDSClient cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		DBInstance instance = null;
		try{
			// Create the database Instance
			CreateDBInstanceRequest request = new CreateDBInstanceRequest();
			
//			instance = client.createDBInstance(createDBInstanceRequest);
		}catch(DBInstanceAlreadyExistsException e){
			
		}

		return null;
		
		

	}
	

	/**
	 * Build up the configuration
	 * @param stack
	 * @param defaultPassword
	 * @return
	 */
	static CreateDBInstanceRequest buildCreateDBRequest(InputConfiguration config){
		// Create the database Instance
		CreateDBInstanceRequest request = new CreateDBInstanceRequest();
		// This will be the schema name.
		request.setDBName(config.getIdGeneratorDatabaseSchemaName());
		request.setDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier());
		// This database only needs a small amount of disk space so this is set the minimum of 5 GB
		request.setAllocatedStorage(new Integer(5));
		// We only need a small instance for now.
		request.setDBInstanceClass("db.m1.small");
		request.setEngine("MySQL");
		request.setMasterUsername(config.getIdGeneratorDatabaseMasterUsername());
		request.setMasterUserPassword(config.getIdGeneratorDatabaseMasterPasswordPlaintext());
//		request.
//		request.set
		return request;
	}

}
