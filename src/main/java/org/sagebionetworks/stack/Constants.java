package org.sagebionetworks.stack;

/**
 * Basic constants for building the stack.
 * 
 * @author John
 *
 */
public class Constants {
	
	/**
	 * Property key for the AWS ACCESS_KEY
	 */
	public static String AWS_ACCESS_KEY = "org.sagebionetworks.aws.access.key";
	/**
	 * Property key for the AWS SECRET_KEY
	 */
	public static String AWS_SECRET_KEY = "org.sagebionetworks.aws.secret.key";
	
	/**
	 * Property key for the encryption key used by this stack
	 */
	public static final String STACK_ENCRYPTION_KEY	 = "org.sagebionetworks.encryption.key";
	
	/**
	 * Property key for the stack.
	 */
	public static final String STACK = "org.sagebionetworks.stack";

	/**
	 * Property key for the stack instance.
	 */
	public static final String INSTANCE = "org.sagebionetworks.stack.instance";
	
	/**
	 * Property key for the default id generator password.
	 */
	public static final String KEY_DEFAULT_ID_GEN_PASSWORD = "org.sagebionetworks.id.generator.db.default.password";
	
	/**
	 * Property for the classless inter-domain routing to be used for SSH access
	 */
	public static final String KEY_CIDR_FOR_SSH = "org.sagebionetworks.cidr.for.ssh";
	
	/**
	 * An AWS error code used to indicate a duplicate permission.
	 */
	public static final String ERROR_CODE_INVALID_PERMISSION_DUPLICATE = "InvalidPermission.Duplicate";
	
	/**
	 * An AWS error code used to indicate a duplicate security group.
	 */
	public static final String ERROR_CODE_INVALID_GROUP_DUPLICATE = "InvalidGroup.Duplicate";
	
	/**
	 * An AWS error code used to indicate a DB parameter group is not found
	 */
	public static final String ERROR_CODE_DB_PARAMETER_GROUP_NOT_FOUND = "DBParameterGroupNotFound";
	
	/**
	 * An AWS error code used to indicate that a DB security group already exits.
	 */
	public static final String ERROR_CODE_DB_SECURITY_GROUP_ALREADY_EXISTS = "DBSecurityGroupAlreadyExists";
	
	/**
	 * An AWS error code used to indicate that a DB security group authorization already exists.
	 */
	public static final String ERROR_CODE_AUTHORIZATION_ALREADY_EXITS = "AuthorizationAlreadyExists";
	

	/**
	 * Template for the security group name.
	 */
	public static final String SECURITY_GROUP_NAME_TEMPLATE = "elastic-beanstalk-%1$s-%2$s";

	/**
	 * Template for the security description.
	 */
	public static final String SECURITY_GROUP_DESCRIPTION_TEMPLATE = "All elastic beanstalk instances of stack:'%1$s' instance:'%2$s' belong to this EC2 security group";
	
	/**
	 * Used to create Ip permissions.
	 */
	public static final String IP_PROTOCOL_TCP = "tcp";
	public static final int PORT_HTTPS = 443;
	public static final int PORT_HTTP = 80;
	public static final int PORT_SSH = 22;
	
	/**
	 * The classless inter-domain routing to allow access to all IPs
	 */
	public static final String CIDR_ALL_IP = "0.0.0.0/0";
	
	/**
	 * Part of setting up a database parameter group.
	 */
	public static final String MYSQL_5_5_DB_PARAMETER_GROUP_FAMILY = "mysql5.5";
	
	/**
	 * Name of the DB parameter group
	 */
//	public static final String DB_PARAM_GROUP_NAME_TEMPLATE = "mysql5-5-%1$s-params";
	
	/**
	 * The description of the DB parameter group.
	 */
//	public static final String DB_PARAM_GROUP_DESC_TEMPALTE = "Custom MySQL 5.5 database parameters (including slow query log enabled) used by all database instances belonging to stack: '%1$s'";
	
	/**
	 * The DB parameter key for the slow query log.
	 * 
	 */
	public static final String DB_PARAM_KEY_SLOW_QUERY_LOG = "slow_query_log";
	
	/**
	 * The DB parameter key for the long query time.
	 */
	public static final String DB_PARAM_KEY_LONG_QUERY_TIME = "long_query_time";
		

	
}
