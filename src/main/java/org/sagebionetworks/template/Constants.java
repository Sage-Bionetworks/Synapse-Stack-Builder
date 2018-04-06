package org.sagebionetworks.template;

public class Constants {

	/**
	 * The unique name assigned to the Synapse VPC stack.
	 */
	public static final String VPC_STACK_NAME_FORMAT = "synapse-%1$s-vpc";
	
	// CloudFormation Parameter names.
	public static final String PARAMETER_VPN_CIDR = "VpnCidr";
	public static final String PARAMETER_PUBLIC_SUBNET_ZONES = "PublicSubnetZones";
	public static final String PARAMETER_PRIVATE_SUBNET_ZONES = "PrivateSubnetZones";
	public static final String PARAMETER_VPC_SUBNET_PREFIX = "VpcSubnetPrefix";
	public static final String PARAMETER_VPC_NAME = "VpcName";
	// input property keys
	// vpc
	public static final String PROPERTY_KEY_VPC_VPN_CIDR = "org.sagebionetworks.vpc.vpn.cidr";
	public static final String PROPERTY_KEY_VPC_PUBLIC_SUBNET_ZONES = "org.sagebionetworks.vpc.public.subnet.zones";
	public static final String PROPERTY_KEY_VPC_PRIVATE_SUBNET_ZONES = "org.sagebionetworks.vpc.private.subnet.zones";
	public static final String PROPERTY_KEY_VPC_SUBNET_PREFIX = "org.sagebionetworks.vpc.subnet.prefix";
	public static final String PROPERTY_KEY_COLORS = "org.sagebionetworks.vpc.colors.csv";
	// repo
	public static final String PROPERTY_KEY_STACK = "org.sagebionetworks.stack";
	public static final String PROPERTY_KEY_INSTANCE = "org.sagebionetworks.instance";
	public static final String PROPERTY_KEY_VPC_SUBNET_COLOR = "org.sagebionetworks.vpc.subnet.color";
	public static final String PROPERTY_KEY_REPO_BEANSTALK_NUMBER = "org.sagebionetworks.repo.beanstalk.number";
	
	//templates
	public static final String TEMPLATES_VPC_MAIN_VPC_JSON_VTP = "templates/vpc/main-vpc.json.vtp";
	public static final String TEMPALTE_SHARED_RESOUCES_MAIN_JSON_VTP = "templates/repo/main-repo-shared-resources-template.json.vpt";
	
	public static final int JSON_INDENT = 3;

	public static final String COLORS = "colors";
	public static final String VPC_SUBNET_COLOR = "vpc-subnet-color";
	public static final String STACK = "stack";
	public static final String INSTANCE = "instance";
	public static final String SHARED_RESOUCES_STACK_NAME = "shared-resources-stack-name";
	
}
