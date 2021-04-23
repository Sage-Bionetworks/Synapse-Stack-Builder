package org.sagebionetworks.template;

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class Constants {

	/**
	 * Suffix for the configuration S3 bucket.
	 */
	public static final String CONFIGURATION_BUCKET_TEMPLATE = "%1$s-configuration.sagebase.org";

	public static final String DEFAULT_REPO_PROPERTIES = "templates/repo/repo-defaults.properties";
	public static final String SNS_AND_SQS_CONFIG_FILE = "templates/repo/sns-and-sqs-config.json";
	public static final String KINESIS_CONFIG_FILE = "templates/repo/kinesis-log-streams.json";
	public static final String CLOUDWATCH_LOGS_CONFIG_FILE = "templates/repo/cloudwatch-logs-config.json";
	public static final String S3_CONFIG_FILE = "templates/s3/s3-buckets-config.json";
	public static final String LOAD_BALANCER_ALARM_CONFIG_FILE = "templates/repo/elasticbeanstalk-alarms-config.json";
	public static final String ATHENA_QUERIES_CONFIG_FILE = "templates/repo/athena-queries-config.json";

	/**
	 * A VPC peering role ARN must start with this prefix.
	 */
	public static final String PEERING_ROLE_ARN_PREFIX = "arn:aws:iam::745159704268:role";

	/**
	 * The unique name assigned to the Synapse VPC stack.
	 */
	public static final String VPC_STACK_NAME_FORMAT = "synapse-%1$s-vpc-2";
	public static final String VPC_PUBLIC_SUBNETS_STACKNAME_FORMAT = "synapse-%1$s-vpc-2-public-subnets";
	public static final String VPC_PRIVATE_SUBNET_STACKNAME_FORMAT = "synapse-%1$s-vpc-2-private-subnets-%2$s";

	/**
	 * Name assigned to Synapse Global Resources stack
	 */
	public static final String GLOBAL_RESOURCES_STACK_NAME_FORMAT = "synapse-%1$s-global-resources";

	// CloudFormation Parameter names.
	// VPC
	public static final String PARAMETER_VPN_CIDR = "VpnCidr";
	public static final String PARAMETER_VPC_SUBNET_PREFIX = "VpcSubnetPrefix";
	public static final String PARAMETER_OLD_VPC_ID = "OldVpcId";
	public static final String PARAMETER_OLD_VPC_CIDR = "OldVpcCidr";
	// repo
	public static final String PARAMETER_MYSQL_PASSWORD = "MySQLDatabaseMasterPassword";
	public static final String PARAMETER_ENCRYPTION_KEY = "EncryptionKey";
	public static final String PARAMETER_AWS_SECRET = "AwsSecret";
	public static final String PARAMETER_AWS_KEY = "AwsKey";
	// input property keys
	// vpc
	public static final String PROPERTY_KEY_VPC_VPN_CIDR = "org.sagebionetworks.vpc.vpn.cidr";
	public static final String PROPERTY_KEY_VPC_AVAILABILITY_ZONES = "org.sagebionetworks.vpc.availability.zones";
	public static final String PROPERTY_KEY_VPC_SUBNET_PREFIX = "org.sagebionetworks.vpc.subnet.prefix";
	public static final String PROPERTY_KEY_COLORS = "org.sagebionetworks.vpc.colors.csv";
	public static final String PROPERTY_KEY_VPC_PEERING_ACCEPT_ROLE_ARN = "org.sagebionetworks.vpc.peering.accept.role.arn";
	public static final String PROPERTY_KEY_OLD_VPC_ID = "org.sagebionetworks.vpc.old.vpc.id";
	public static final String PROPERTY_KEY_OLD_VPC_CIDR = "org.sagebionetworks.vpc.old.vpc.cidr";
	// repo
	public static final String PROPERTY_KEY_STACK = "org.sagebionetworks.stack";
	public static final String PROPERTY_KEY_INSTANCE = "org.sagebionetworks.instance";
	public static final String PROPERTY_KEY_VPC_SUBNET_COLOR = "org.sagebionetworks.vpc.subnet.color";
	public static final String PROPERTY_KEY_REPO_RDS_MULTI_AZ = "org.sagebionetworks.repo.rds.multi.az";
	public static final String PROPERTY_KEY_REPO_RDS_INSTANCE_CLASS = "org.sagebionetworks.repo.rds.instance.class";
	public static final String PROPERTY_KEY_REPO_RDS_STORAGE_TYPE = "org.sagebionetworks.repo.rds.storage.type";
	public static final String PROPERTY_KEY_REPO_RDS_IOPS = "org.sagebionetworks.repo.rds.iops";
	public static final String PROPERTY_KEY_TABLES_RDS_STORAGE_TYPE = "org.sagebionetworks.tables.rds.storage.type";
	public static final String PROPERTY_KEY_TABLES_RDS_IOPS = "org.sagebionetworks.tables.rds.iops";
	public static final String PROPERTY_KEY_REPO_RDS_ALLOCATED_STORAGE = "org.sagebionetworks.repo.rds.allocated.storage";
	public static final String PROPERTY_KEY_REPO_RDS_MAX_ALLOCATED_STORAGE = "org.sagebionetworks.repo.rds.max.allocated.storage";
	public static final String PROPERTY_KEY_TABLES_INSTANCE_COUNT = "org.sagebionetworks.tables.rds.instance.count";
	public static final String PROPERTY_KEY_TABLES_RDS_INSTANCE_CLASS = "org.sagebionetworks.tables.rds.instance.class";
	public static final String PROPERTY_KEY_TABLES_RDS_ALLOCATED_STORAGE = "org.sagebionetworks.tables.rds.allocated.storage";
	public static final String PROPERTY_KEY_TABLES_RDS_MAX_ALLOCATED_STORAGE = "org.sagebionetworks.tables.rds.max.allocated.storage";
	public static final String PROPERTY_KEY_BEANSTALK_ENCRYPTION_KEY = "org.sagebionetworks.beanstalk.encryption.key";
	public static final String PROPERTY_KEY_AWS_SECRET_KEY = "aws.secretKey";
	public static final String PROPERTY_KEY_AWS_ACCESS_KEY_ID = "aws.accessKeyId";
	public static final String PROPERTY_KEY_BEANSTALK_MAX_INSTANCES = "org.sagebionetworks.beanstalk.max.instances.";
	public static final String PROPERTY_KEY_BEANSTALK_MIN_INSTANCES = "org.sagebionetworks.beanstalk.min.instances.";
	public static final String PROPERTY_KEY_BEANSTALK_HEALTH_CHECK_URL = "org.sagebionetworks.beanstalk.health.check.url.";
	public static final String PROPERTY_KEY_BEANSTALK_VERSION = "org.sagebionetworks.beanstalk.version.";
	public static final String PROPERTY_KEY_BEANSTALK_NUMBER = "org.sagebionetworks.beanstalk.number.";
	public static final String PROPERTY_KEY_BEANSTALK_SSL_ARN = "org.sagebionetworks.beanstalk.ssl.arn.";
	public static final String PROPERTY_KEY_ROUTE_53_HOSTED_ZONE = "org.sagebionetworks.route.53.hosted.zone.";
	public static final String PROPERTY_KEY_SECRET_KEYS_CSV = "org.sagebionetworks.secret.keys.csv";
	public static final String PROPERTY_KEY_REPOSITORY_DATABASE_PASSWORD = "org.sagebionetworks.repository.database.password";
	public static final String PROPERTY_KEY_ID_GENERATOR_DATABASE_PASSWORD = "org.sagebionetworks.id.generator.database.password";
	public static final String PROPERTY_KEY_ID_GENERATOR_HOSTED_ZONE_ID = "org.sagebionetworks.id.generator.hosted.zone.id";

	public static final String PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_PREFIX = "org.sagebionetworks.beanstalk.image.version.";
	public static final String PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_JAVA = PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_PREFIX + "java";
	public static final String PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_TOMCAT = PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_PREFIX + "tomcat";
	public static final String PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_AMAZONLINUX = PROPERTY_KEY_ELASTICBEANSTALK_IMAGE_VERSION_PREFIX + "amazonlinux";

	public static final String PROPERTY_KEY_OAUTH_ENDPOINT = "org.sagebionetworks.oauth.authorization.endpoint";

	// tag values
	public static final String TAG_VALUE_DEPARTMENT = "Platform";
	public static final String TAG_VALUE_PROJECT = "Synapse";
	public static final String TAG_VALUE_OWNER_EMAIL = "platform@sagebase.org";
	// tag keys
	public static final String TAG_KEY_DEPARTMENT = "Department";
	public static final String TAG_KEY_PROJECT = "Project";
	public static final String TAG_KEY_OWNER_EMAIL = "OwnerEmail";

	// templates
	public static final String TEMPLATES_VPC_MAIN_VPC_JSON_VTP = "templates/vpc/main-vpc.json.vtp";
	public static final String TEMPLATES_VPC_PUBLIC_SUBNETS_JSON_VTP = "templates/vpc/public-subnets-resources.json.vtp";
	public static final String TEMPLATES_VPC_PRIVATE_SUBNET_JSON_VTP = "templates/vpc/private-subnet-resources.json.vtp";
	public static final String TEMPALTE_SHARED_RESOUCES_MAIN_JSON_VTP = "templates/repo/main-repo-shared-resources-template.json.vpt";
	public static final String TEMPALTE_BEAN_STALK_ENVIRONMENT = "templates/repo/elasticbeanstalk-template.json.vpt";
	public static final String TEMPLATE_ID_GENERATOR = "templates/repo/id-generator-template.json.vpt";
	public static final String TEMPLATE_INVENTORY_BUCKET_POLICY_TEMPLATE = "templates/s3/s3-inventory-bucket-policy.json.vpt";
	public static final String TEMPLATE_GLOBAL_RESOURCES = "templates/global/global-template.json.vpt";
	

	public static final int JSON_INDENT = 5;

	/*
	 * The subnet mask used to create subnet. A subnet mask of 21 will allocate a
	 * subnet with 2,048 address. Note: The subnet mask will be the suffix of each
	 * subnet CIDR.
	 */
	public static final int VPC_SUBNET_NETWORK_MASK = 24;

	/*
	 * The network mask used to create color group. A mask of 21 will allocate a
	 * group with 2048 address. Note: The mask will be the suffix for the color
	 * group CIDR.
	 */
	public static final int VPC_COLOR_GROUP_NETWORK_MASK = 21;

	public static final String VPC_CIDR_SUFFIX = ".0.0/16";

	// context keys
	public static final String SUBNETS = "subnets";
	public static final String VPC_CIDR = "vpcCidr";
	public static final String VPC_SUBNET_COLOR = "subnetGroupColor";
	public static final String STACK = "stack";
	public static final String INSTANCE = "instance";
	public static final String SHARED_RESOUCES_STACK_NAME = "sharedRresourcesStackName";
	public static final String VPC_EXPORT_PREFIX = "vpcExportPrefix";
	public static final String SHARED_EXPORT_PREFIX = "sharedExportPrefix";
	public static final String GLOBAL_RESOURCES_EXPORT_PREFIX = "globalResourcesExportPrefix";
	public static final String PROPS = "props";
	public static final String PEER_ROLE_ARN = "peerRoleArn";
	public static final String AVAILABILITY_ZONES = "availabilityZones";
	public static final String DATABASE_DESCRIPTORS = "databaseDescriptors";
	public static final String ENVIRONMENT = "environment";
	public static final String REPO_NUMBER = "repoNumber";
	public static final String DB_ENDPOINT_SUFFIX = "dbEndpointSuffix";
	public static final String REPO_BEANSTALK_NUMBER = "repoBeanstalkNumber";
	public static final String STACK_CMK_ALIAS = "stackCMKAlias";
	public static final String DATABASE_IDENTIFIER = "databaseIdentifier";
	public static final String EXCEPTION_THROWER = "exceptionThrower";
	public static final String VPC_STACKNAME = "vpcStackName";
	public static final String PRIVATE_SUBNET_IDX = "privateSubnetIdx";
	public static final String HOSTED_ZONE = "hostedZone";
	public static final String TEMP_VPC_CIDR = "tempVpcCidr";

	public static final String CAPABILITY_NAMED_IAM = "CAPABILITY_NAMED_IAM";
	public static final String OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT = "RepositoryDBEndpoint";

	public static final String SNS_TOPIC_DESCRIPTORS = "snsTopicDescriptors";
	public static final String SQS_QUEUE_DESCRIPTORS = "sqsQueueDescriptors";

	public static final String SOLUTION_STACK_NAME = "solutionStackName";
	public static final String ENCRYPTED_AMI_IMAGE_ID = "encryptedImageId";

	public static final String GLUE_DATABASE_NAME = "glueDatabaseName";
	public static final String KINESIS_FIREHOSE_STREAM_DESCRIPTORS = "kinesisFirehoseStreams";
	public static final String KINESIS_FIREHOSE_BUCKETS = "kinesisBuckets";

	public static final String CLOUDWATCH_LOGS_DESCRIPTORS = "logDescriptors";
	
	public static final String ATHENA_QUERY_DESCRIPTORS = "athenaQueryDescriptors";
	
	public static final String LOAD_BALANCER_ALARMS = "loadBalancerAlarms";

	public static final String OAUTH_ENDPOINT = "oauthEndpoint";

	public static final String SES_SYNAPSE_DOMAIN = "synapse.org";
	public static final String GLOBAL_CFSTACK_OUTPUT_KEY_SES_COMPLAINT_TOPIC = "SesSynapseOrgComplaintTopic";
	public static final String GLOBAL_CFSTACK_OUTPUT_KEY_SES_BOUNCE_TOPIC = "SesSynapseOrgBounceTopic";

	/**
	 * Create a camel case name from dash-separated-name. Given 'foo-bar' will
	 * return 'FooBar'
	 * 
	 * @param name
	 * @param separator
	 * @return
	 */
	public static final String createCamelCaseName(String name, String separator) {
		String[] split = name.split(separator);
		StringBuilder builder = new StringBuilder();
		for (String part : split) {
			builder.append(part.substring(0, 1).toUpperCase());
			builder.append(part.substring(1).toLowerCase());
		}
		return builder.toString();
	}

	public static List<String> createCamelCaseName(Collection<String> collection, String separator){
		return collection.stream().map(str -> createCamelCaseName(str, separator)).collect(Collectors.toList());
	}

	/**
	 * Create the prefix used for all of the VPC stack exports;
	 * 
	 * @return
	 */
	public static String createVpcExportPrefix(String stack) {
		StringJoiner joiner = new StringJoiner("-");
		joiner.add("us-east-1-synapse");
		joiner.add(stack);
		joiner.add("vpc");
		joiner.add("2");
		return joiner.toString();
	}

	public static String createGlobalResourcesExportPrefix(String stack) {
		StringJoiner joiner = new StringJoiner("-");
		joiner.add("us-east-1");
		joiner.add(String.format(GLOBAL_RESOURCES_STACK_NAME_FORMAT, stack));
		return joiner.toString();
	}

	public static final String PROD_STACK_NAME = "prod";

}
