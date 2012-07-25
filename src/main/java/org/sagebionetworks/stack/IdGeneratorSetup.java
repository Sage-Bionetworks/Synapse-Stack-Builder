package org.sagebionetworks.stack;
import static org.sagebionetworks.stack.Constants.*;
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
	
	public static String DATABASE_IDENTIFIER_SUFFIX = "-id-generator";
	public static String DATABASE_SCHEMA_SUFFIX = "idgen";
	public static String MASTER_USER_SUFFIX = "idgenUser";
	
	/**
	 * Create everything need for the Id generator database.
	 * @param config
	 * @return
	 */
	public static DatabaseInfo createIdGeneratorDatabase(AmazonRDSClient client, String defaultPassword, String securityGroupName){
		if(client == null) throw new IllegalArgumentException("AmazonRDSClient cannot be null");
		if(defaultPassword == null) throw new IllegalArgumentException("Default password cannot be null");
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
	static CreateDBInstanceRequest buildCreateDBRequest(String stack, String defaultPassword){
		// Create the database Instance
		CreateDBInstanceRequest request = new CreateDBInstanceRequest();
		// This will be the schema name.
		request.setDBName(stack+DATABASE_SCHEMA_SUFFIX);
		request.setDBInstanceIdentifier(stack+DATABASE_IDENTIFIER_SUFFIX);
		// This database only needs a small amount of disk space so this is set the minimum of 5 GB
		request.setAllocatedStorage(new Integer(5));
		// We only need a small instance for now.
		request.setDBInstanceClass("db.m1.small");
		request.setEngine("MySQL");
		request.setMasterUsername(stack+MASTER_USER_SUFFIX);
		request.setMasterUserPassword(defaultPassword);
//		request.
//		request.set
		return request;
	}

}
