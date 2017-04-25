package org.sagebionetworks.stack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * The input keys for the war versions.
	 */
	public static final String SWC_VERSION = "org.sagebionetworks.swc.version";
	public static final String PLFM_VERSION = "org.sagebionetworks.plfm.version";
	
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
	 * Other keys
	 */
	public static final String KEY_ORG_SAGEBIONETWORKS_BCC_GOOGLEAPPS_OAUTH_ACCESS_TOKEN_SECRET_PLAINTEXT = "org.sagebionetworks.bcc.googleapps.oauth.access.token.secret.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_BCC_GOOGLEAPPS_OAUTH_ACCESS_TOKEN_PLAINTEXT = "org.sagebionetworks.bcc.googleapps.oauth.access.token.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_BCC_GOOGLEAPPS_OAUTH_CONSUMER_SECRET_PLAINTEX = "org.sagebionetworks.bcc.googleapps.oauth.consumer.secret.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_MAIL_PW_PLAINTEXT = "org.sagebionetworks.mailPW.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_PORTAL_API_LINKEDIN_KEY = "org.sagebionetworks.portal.api.linkedin.key";
	public static final String KEY_ORG_SAGEBIONETWORKS_PORTAL_API_LINKEDIN_SECRET_PLAINTEXT = "org.sagebionetworks.portal.api.linkedin.secret.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_PORTAL_API_GETSATISFACTION_KEY = "org.sagebionetworks.portal.api.getsatisfaction.key";
	public static final String KEY_ORG_SAGEBIONETWORKS_PORTAL_API_GETSATISFACTION_SECRET_PLAINTEXT = "org.sagebionetworks.portal.api.getsatisfaction.secret.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_EZID_USERNAME = "org.sagebionetworks.ezid.username";
	public static final String KEY_ORG_SAGEBIONETWORKS_EZID_PASSWORD_PLAINTEXT = "org.sagebionetworks.ezid.password.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_EZID_DOI_PREFIX = "org.sagebionetworks.ezid.doi.prefix";
	public static final String KEY_ORG_SAGEBIONETWORKS_REPO_MANAGER_JIRA_USER_PASSWORD_PLAINTEXT = "org.sagebionetworks.repo.manager.jira.user.password.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_MIGRATION_API_KEY = "org.sagebionetworks.migration.admin.apikey";
	public static final String KEY_ORG_SAGEBIONETWORKS_SEARCH_ENABLED = "org.sagebionetworks.search.enabled";
	public static final String KEY_ORG_SAGEBIONETWORKS_DYNAMO_ENABLED = "org.sagebionetworks.dynamo.enabled";
	public static final String KEY_ORG_SAGEBIONETWORKS_TABLE_ENABLED = "org.sagebionetworks.table.enabled";
	public static final String KEY_ORG_SAGEBIONETWORKS_PREVIEW_OPENOFFICE_ENABLED = "org.sagebionetworks.preview.open.office.enabled";
	public static final String KEY_ORG_SAGEBIONETWORKS_NOTIFICATION_EMAIL_ADDRESS = "org.sagebionetworks.notification.email.address";
	public static final String KEY_ORG_SAGEBIONETWORKS_OAUTH2_GOOGLE_CLIENT_ID = "org.sagebionetworks.oauth2.google.client.id";
	public static final String KEY_ORG_SAGEBIONETWORKS_OAUTH2_GOOGLE_CLIENT_SECRET = "org.sagebionetworks.oauth2.google.client.secret.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_CLOUDMAILIN_USR_PLAINTEXT = "org.sagebionetworks.email.cloudmailin.user.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_CLOUDMAILIN_PW_PLAINTEXT = "org.sagebionetworks.email.cloudmailin.password.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_OAUTH2_ORCID_CLIENT_ID="org.sagebionetworks.oauth2.orcid.client.id";
	public static final String KEY_ORG_SAGEBIONETWORKS_OAUTH2_ORCID_CLIENT_SECRET="org.sagebionetworks.oauth2.orcid.client.secret.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_DOCKER_AUTHORIZATION_PRIVATE_KEY_PLAINTEXT="org.sagebionetworks.docker.authorization.private.key.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_DOCKER_AUTHORIZATION_CERTIFICATE="org.sagebionetworks.docker.authorization.certificate";
	public static final String KEY_ORG_SAGEBIONETWORKS_DOCKER_REGISTRY_USER_PLAINTEXT="org.sagebionetworks.docker.registry.user.plaintext";
	public static final String KEY_ORG_SAGEBIONETWORKS_DOCKER_REGISTRY_PASSWORD_PLAINTEXT="org.sagebionetworks.docker.registry.password.plaintext";

	public static final String KEY_ORG_SAGEBIONETWORKS_PORTAL_ACM_CERT_ARN="org.sagebionetworks.PORTAL.acm.certificate.arn";
	public static final String KEY_ORG_SAGEBIONETWORKS_REPO_ACM_CERT_ARN="org.sagebionetworks.REPO.acm.certificate.arn";
	public static final String KEY_ORG_SAGEBIONETWORKS_WORKERS_ACM_CERT_ARN="org.sagebionetworks.WORKERS.acm.certificate.arn";
	
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
	
	public static final String 	ERROR_CODE_KEY_PAIR_NOT_FOUND = "InvalidKeyPair.NotFound";
	

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
	public static final String MYSQL_5_6_DB_PARAMETER_GROUP_FAMILY = "mysql5.6";
		
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
	 * The DB parameter key for the max allowed packet.
	 */
	public static final String DB_PARAM_KEY_MAX_ALLOWED_PACKET = "max_allowed_packet";

	/**
	 * The DB paramater key for log_bin_trust_function_creators (see PLFM-4276)
	 */
	public static final String DB_PARAM_KEY_LOG_BIN_TRUST_FUNCTION_CREATORS = "log_bin_trust_function_creators";

	/**
	 * This is currently set to 16 MB per PLFM-1526.
	 */
	public static final long DB_PARAM_VALUE_MAX_ALLOWED_PACKET = 1024*1024*16;
	
	/**
	 * Small m1 database instance class
	 */
	public static final String DATABASE_INSTANCE_CLASS_M1_SMALL = "db.m1.small";
	
	
	/**
	 * Large m1 database instance class
	 */
	public static final String DATABASE_INSTANCE_CLASS_M1_LARGE = "db.m1.large";

	/**
	 * Large r3 database instance class
	 */
	public static final String DATABASE_INSTANCE_CLASS_R3_LARGE = "db.r3.large";
	
	/**
	 * 1 GB = 2^30 Bytes
	 */
	public static final Integer BYTES_PER_GIGABYTE = (int) Math.pow(2, 30);
	
	/**
	 * Maps Database instances to their memory.
	 * 
	 */
	private static final Map<String, Double> INSTANCE_MEMORY_MAP = new HashMap<String, Double>();
	static{
		// m1.small 1.7 GB as of July 2012
		INSTANCE_MEMORY_MAP.put(DATABASE_INSTANCE_CLASS_M1_SMALL, 1.7*BYTES_PER_GIGABYTE);
		// m1.arge 7.5 GB as of July 2012
		INSTANCE_MEMORY_MAP.put(DATABASE_INSTANCE_CLASS_M1_LARGE, 7.5*BYTES_PER_GIGABYTE);
		// r3.large 15GB as of April 2017
		INSTANCE_MEMORY_MAP.put(DATABASE_INSTANCE_CLASS_R3_LARGE, 15.0*BYTES_PER_GIGABYTE);
	}
	
	/**
	 * Get the total memory of a AWS Database instances class
	 * @param intancesClass
	 * @return
	 */
	public static Double getDatabaseClassMemrorySizeBytes(String intancesClass){
		Double value = INSTANCE_MEMORY_MAP.get(intancesClass);
		if(value == null) throw new IllegalArgumentException("Unknown AWS Database intances class: "+intancesClass);
		return value;
	}
	
	/**
	 * MySQL database engine.
	 */
	public static final String DATABASE_ENGINE_MYSQL = "MySQL";
	
	/**
	 * MySQL version.
	 */
	public static final String DATABASE_ENGINE_MYSQL_VERSION = "5.6.23";
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
	
	/**
	 * Email protocol for subscribing to a topic.
	 */
	public static final String TOPIC_SUBSCRIBE_PROTOCOL_EMAIL = "email";
	
	/**
	 * The property key used for the RDS alert topic subscription endpoint
	 */
	public static final String KEY_RDS_ALAERT_SUBSCRIPTION_ENDPONT = "org.sagebionetworks.stack.rds.alert.topic.subscription.endpoint";
	public static final String KEY_ORG_SAGEBIONETWORKS_ENVIRONMENT_NOTIFICATION_ENDPOINT = "org.sagebionetworks.environment.instance.notification.endpoint";
	/** 
	 * Alarm constants.
	 */
	public static final String METRIC_FREEABLE_MEMORY = "FreeableMemory";
	public static final String METRIC_WRITE_LATENCY = "WriteLatency";
	public static final String METRIC_HIGH_CPU_UTILIZATION = "CPUUtilization";
	public static final String METRIC_FREE_STOREAGE_SPACE = "FreeStorageSpace";
	public static final String METRIC_SWAP_USAGE = "SwapUsage";
	public static final String DB_INSTANCE_IDENTIFIER = "DBInstanceIdentifier";
	public static final String NAME_SPACES_AWS_RDS = "AWS/RDS";
	public static final String LOW_FREEABLE_MEMORY_NAME = "-Low-Freeable-Memory";
	public static final String HIGH_WRITE_LATENCY = "High-Write-Latency";
	public static final String HIGH_CPU_UTILIZATION = "High-CPU-Utilization";
	public static final String LOW_FREE_STOREAGE_SPACE = "Low-Free-Storage-Space";
	public static final String SWAP_USAGE = "Swap Usage";
	public static final int FIVE_MINUTES_IN_SECONDS = 5*60;
	public static final String STATISTIC_AVERAGE = "Average";
	
	public static final String NAMESPACE_ELB = "AWS/ELB";
	public static final String STATISTIC_MAX = "Maximum";
	public static final String DIMENSION_NAME_LOAD_BALANCER = "LoadBalancerName";
	public static final String METRIC_UNHEALTHY_COUNT = "UnHealthyHostCount";
	
	
	/**
	 * The stack config template file.
	 */
	public static String FILE_STACK_CONFIG_TEMPLATE = "stack-config-template.properties";
	
	/**
	 * The key used to store the id generator database end point.
	 */
	public static String KEY_ID_GENERATOR_DB_ADDRESS = "id.gen.database.address";
	
	/**
	 * The key used to store the stack instance database end point.
	 */
	public static String KEY_STACK_INSTANCE_DB_ADDRESS = "stack.instance.database.address";
	
	/**
	 * The endpoint for issuing searches.
	 */
	public static String KEY_STACK_INSTANCE_SEARCH_INDEX_SEARCH_ENDPOINT = "stack.instance.search.index.search.endpoint";
	
	/**
	 * The endpoint for updating search documents.
	 */
	public static String KEY_STACK_INSTANCE_SEARCH_INDEX_DOCUMENT_ENDPOINT = "stack.instance.search.index.document.endpoint";
	
	/**
	 * Stack solution name for "32bit Amazon Linux running Tomcat 7"
	 */
	public static final String SOLUTION_STACK_NAME_32BIT_TOMCAT_7 = "32bit Amazon Linux running Tomcat 7";

	/**
	 * Stack solution name for "64bit Amazon Linux running Tomcat 7"
	 */
	public static final String SOLUTION_STACK_NAME_64BIT_TOMCAT_7 = "64bit Amazon Linux running Tomcat 7";
	
	/**
	 * Stack solution name for "64bit Amazon Linux 2014.03 v1.0.3 running Tomcat 7 Java 7"
	 */
	public static final String SOLUTION_STACK_NAME_64BIT_TOMCAT7_JAVA7_2016_03_AMI = "64bit Amazon Linux 2016.03 v2.1.3 running Tomcat 7 Java 7";
	
	/**
	 * Properties file of all of the beanstalk config values.
	 */
	public static final String ELASTIC_BEANSTALK_CONFIG_PROP_FILE_NAME = "elastic-beanstalk-config.properties";
	
	/**
	 * The property key for the SSL certificate ARN.
	 */
	public static final String KEY_SSL_CERTIFICATE_ARN = "ssl.certificate.arn";
	
	/** 
	 * Prefixes used for property lookup.
	 */
	public static final String PREFIX_REPO = "repo";
	public static final String PREFIX_PORTAL = "portal";
	public static final String PREFIX_WORKERS = "workers";
	
	// List of service prefixes for Route53 setup
	public static final List<String> SVC_PREFIXES = Arrays.asList(Constants.PREFIX_PORTAL, Constants.PREFIX_REPO, Constants.PREFIX_WORKERS);
	public static final List<String> ROUTE53_PREFIXES = Arrays.asList(Constants.PREFIX_PORTAL, Constants.PREFIX_REPO, Constants.PREFIX_WORKERS);
	
	public static final String PORTAL_BEANSTALK_NUMBER = "org.sagebionetworks.stack.portal.beanstalk.number";
	public static final String PLFM_BEANSTALK_NUMBER = "org.sagebionetworks.stack.plfm.beanstalk.number";
	
	public static final String NUMBER_TABLE_INSTANCES = "org.sagebionetworks.number.table.instances";
	
	public static final String KEY_TABLE_CLUSTER_DATABASE_COUNT = "org.sagebionetworks.table.cluster.database.count";
	public static final String KEY_TABLE_CLUSTER_DATABASE_ENDPOINT_PREFIX = "org.sagebionetworks.table.cluster.endpoint.";
	public static final String KEY_TABLE_CLUSTER_DATABASE_SCHEMA_PREFIX = "org.sagebionetworks.table.cluster.schema.";
	
	public static String encryptKeyName(String key) {
		return key+".encrypted";
	}
	
	public static String plaintextKeyName(String key) {
		return key+".plaintext";
	}
}
