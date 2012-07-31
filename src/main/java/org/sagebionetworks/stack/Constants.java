package org.sagebionetworks.stack;

/**
 * Basic constants for building the stack.
 * 
 * @author John
 *
 */
public class Constants {
	
	/**
	 * This is the stack named used to indicate a production stack.
	 * 
	 */
	public static final String PRODUCTION_STACK = "prod";
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
	 * Properties that requiring encryption should have this as a suffix.
	 */
	public static final String PLAIN_TEXT_SUFFIX = "plaintext";
	
	/**
	 * The suffix added to the encrypted form of plain text properties.
	 */
	public static final String ENCRYPTED_SUFFIX = "encrypted";
	
	/**
	 * Property key for the default id generator password.
	 */
	private static final String KEY_DEFAULT_ID_GEN_PASSWORD_PREFIX = "org.sagebionetworks.id.generator.db.default.password";
	public static final String KEY_DEFAULT_ID_GEN_PASSWORD_PLAIN_TEXT = KEY_DEFAULT_ID_GEN_PASSWORD_PREFIX+"."+PLAIN_TEXT_SUFFIX;
	public static final String KEY_DEFAULT_ID_GEN_PASSWORD_ENCRYPTED = KEY_DEFAULT_ID_GEN_PASSWORD_PREFIX+"."+ENCRYPTED_SUFFIX;
	
	
	/**
	 * Property key for stack-instance database
	 */
	private static final String KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PREFIX = "org.sagebionetworks.stack.instance.db.default.password";
	public static final String KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PLAIN_TEXT = KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PREFIX+"."+PLAIN_TEXT_SUFFIX;
	public static final String KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_ENCRYPTED = KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PREFIX+"."+ENCRYPTED_SUFFIX;
	
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
	 * The DB parameter key for the slow query log.
	 * 
	 */
	public static final String DB_PARAM_KEY_SLOW_QUERY_LOG = "slow_query_log";
	
	/**
	 * The DB parameter key for the long query time.
	 */
	public static final String DB_PARAM_KEY_LONG_QUERY_TIME = "long_query_time";
	
	/**
	 * Small database instance class
	 */
	public static final String DATABASE_INSTANCE_CLASS_SMALL = "db.m1.small";
	
	/**
	 * Small database instance class
	 */
	public static final String DATABASE_INSTANCE_CLASS_LARGE = "db.m1.large";
	
	/**
	 * MySQL database engine.
	 */
	public static final String DATABASE_ENGINE_MYSQL = "MySQL";
	
	/**
	 * MySQL version.
	 */
	public static final String DATABASE_ENGINE_MYSQL_VERSION = "5.5.12";
	/**
	 * us-east-1d
	 */
	public static final String  EC2_AVAILABILITY_ZONE_US_EAST_1D = "us-east-1d";
	
	/**
	 * This window is in UTC.  Monday morning UTC should be Sunday night PDT.
	 */
	public static final String PREFERRED_DATABASE_MAINTENANCE_WINDOW_SUNDAY_NIGHT_PDT = "Mon:07:15-Mon:07:45";
		
	/**
	 * This window is in UTC.  Should be 10 pm - 1 am PDT
	 */
	public static final String PREFERRED_DATABASE_BACKUP_WINDOW_MIDNIGHT = "3:00-6:00";
	
	/**
	 * general-public-license
	 */
	public static final String LICENSE_MODEL_GENERAL_PUBLIC = "general-public-license";
	

	
}
