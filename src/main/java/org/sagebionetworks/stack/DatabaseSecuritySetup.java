package org.sagebionetworks.stack;

import static org.sagebionetworks.stack.Constants.ERROR_CODE_AUTHORIZATION_ALREADY_EXITS;
import static org.sagebionetworks.stack.Constants.ERROR_CODE_DB_SECURITY_GROUP_ALREADY_EXISTS;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AuthorizeDBSecurityGroupIngressRequest;
import com.amazonaws.services.rds.model.CreateDBSecurityGroupRequest;
import com.amazonaws.services.rds.model.DBSecurityGroup;

/**
 * Setup the Database security groups.
 * 
 * @author jmhill
 *
 */
public class DatabaseSecuritySetup {
	
	private static Logger log = Logger.getLogger(DatabaseSecuritySetup.class.getName());
	
	private AmazonRDSClient rdsClient;
	private InputConfiguration config;
	private SecurityGroup elasticSecurityGroup;

	/**
	 * The IoC constructor.
	 * @param rdsClient
	 * @param config
	 * @param elasticSecurityGroup
	 */
	public DatabaseSecuritySetup(AmazonRDSClient rdsClient,
			InputConfiguration config, SecurityGroup elasticSecurityGroup) {
		super();
		if(rdsClient == null) throw new IllegalArgumentException("AmazonEC2Client cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(elasticSecurityGroup == null) throw new IllegalArgumentException("SecurityGroup cannot be null");
		
		this.rdsClient = rdsClient;
		this.config = config;
		this.elasticSecurityGroup = elasticSecurityGroup;
	}

	/**
	 * Setup all of the database security groups needed for the stack.
	 * @param rdsClient
	 * @param config
	 * @param elasticSecurityGroup 
	 * @return
	 */
	public void setupDatabaseAllSecuityGroups(){
		// Create the ID generator security group
		CreateDBSecurityGroupRequest request = new CreateDBSecurityGroupRequest();
		request.setDBSecurityGroupDescription(config.getIdGeneratorDatabaseSecurityGroupDescription());
		request.setDBSecurityGroupName(config.getIdGeneratorDatabaseSecurityGroupName());
		createSecurityGroup(request);
		
		// Grant the EC2 security group access the ID generator database
		addEC2SecurityGroup(request.getDBSecurityGroupName(), elasticSecurityGroup);
		// Allow anyone in the CIDR used for the stack SSH access to access this database.
		addCIDRToGroup(request.getDBSecurityGroupName(), config.getCIDRForSSH());
		
		// Create Stack database security group
		request = new CreateDBSecurityGroupRequest();
		request.setDBSecurityGroupDescription(config.getStackDatabaseSecurityGroupDescription());
		request.setDBSecurityGroupName(config.getStackDatabaseSecurityGroupName());
		createSecurityGroup(request);
		
		// Grant the EC2 security group access the Stack MySQL database
		addEC2SecurityGroup(request.getDBSecurityGroupName(), elasticSecurityGroup);
		// Allow anyone in the CIDR used for the stack SSH access to access this database.
		addCIDRToGroup(request.getDBSecurityGroupName(), config.getCIDRForSSH());

	}

	/**
	 * Create a security group. If the group already exists
	 * @param ec2Client
	 * @param request
	 */
	void createSecurityGroup(CreateDBSecurityGroupRequest request) {
		try{
			// First create the EC2 group
			log.info("Creating Database Security Group: "+request.getDBSecurityGroupName()+"...");
			DBSecurityGroup result = rdsClient.createDBSecurityGroup(request);
		}catch (AmazonServiceException e){
			if(ERROR_CODE_DB_SECURITY_GROUP_ALREADY_EXISTS.equals(e.getErrorCode())){
				// This group already exists
				log.info("Database Security Group: "+request.getDBSecurityGroupName()+" already exits");
			}else{
				throw e;
			}
		}
	}
	
	/**
	 * Add a Classless Inter-Domain Routing (CIDR) to a database security group.  This will grant anyone within
	 * the CIDR to access this database.
	 * @param rdsClient
	 * @param groupName
	 * @param cIDR
	 */
	void addCIDRToGroup(String groupName, String cIDR){
		// Make sure we can access the machines from with the VPN
		try{
			// Configure this group
			AuthorizeDBSecurityGroupIngressRequest ingressRequest = new AuthorizeDBSecurityGroupIngressRequest(groupName);
			ingressRequest.setCIDRIP(cIDR);
			log.info(String.format("Adding CIDR '%1$s' to database security group: '%2$s'...", cIDR, groupName));
			rdsClient.authorizeDBSecurityGroupIngress(ingressRequest);
		}catch(AmazonServiceException e){
			// Ignore duplicates
			if(ERROR_CODE_AUTHORIZATION_ALREADY_EXITS.equals(e.getErrorCode())){
				// This already exists
				log.info(String.format("CIDR '%1$s' already has acces to DB security group '%2$s'", cIDR, groupName));
			}else{
				// Throw any other error
				throw e;
			}
		}
	}
	
	/**
	 * Add an EC2 Security group to a database security group.  This will allow any EC2 instance in that group to access this database.
	 * @param ec2Client
	 * @param groupName
	 * @param permission
	 */
	void addEC2SecurityGroup(String groupName,  SecurityGroup elasticSecurityGroup){
		// Make sure we can access the machines from with the VPN
		try{
			// Configure this group
			AuthorizeDBSecurityGroupIngressRequest ingressRequest = new AuthorizeDBSecurityGroupIngressRequest(groupName);
			ingressRequest.setEC2SecurityGroupOwnerId(elasticSecurityGroup.getOwnerId());
			ingressRequest.setEC2SecurityGroupName(elasticSecurityGroup.getGroupName());
			log.info(String.format("Adding EC2 security group '%1$s' to database security group: '%2$s'...", elasticSecurityGroup.getGroupName(), groupName));
			rdsClient.authorizeDBSecurityGroupIngress(ingressRequest);
		}catch(AmazonServiceException e){
			// Ignore duplicates
			if(ERROR_CODE_AUTHORIZATION_ALREADY_EXITS.equals(e.getErrorCode())){
				// This already exists
				log.info(String.format("EC2 secruity group '%1$s' already has acces to DB security group '%2$s'", elasticSecurityGroup.getGroupName(), groupName));
			}else{
				// Throw any other error
				throw e;
			}
		}
	}
	

}
