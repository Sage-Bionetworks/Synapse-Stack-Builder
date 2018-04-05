package org.sagebionetworks.template;

public class Constants {

	/**
	 * The unique name assigned to the Synapse VPC stack.
	 */
	public static final String VPC_STACK_NAME = "synapse-stack-vpc";
	
	// CloudFormation Parameter names.
	public static final String PARAMETER_VPN_CIDR = "VpnCidr";
	public static final String PARAMETER_PUBLIC_SUBNET_ZONES = "PublicSubnetZones";
	public static final String PARAMETER_PRIVATE_SUBNET_ZONES = "PrivateSubnetZones";
	public static final String PARAMETER_VPC_SUBNET_PREFIX = "VpcSubnetPrefix";
	public static final String PARAMETER_VPC_NAME = "VpcName";
	// input property keys
	public static final String PROPERTY_KEY_VPC_VPN_CIDR = "org.sagebionetworks.vpc.vpn.cidr";
	public static final String PROPERTY_KEY_VPC_PUBLIC_SUBNET_ZONES = "org.sagebionetworks.vpc.public.subnet.zones";
	public static final String PROPERTY_KEY_VPC_PRIVATE_SUBNET_ZONES = "org.sagebionetworks.vpc.private.subnet.zones";
	public static final String PROPERTY_KEY_VPC_SUBNET_PREFIX = "org.sagebionetworks.vpc.subnet.prefix";
	public static final String PROPERTY_KEY_COLORS = "org.sagebionetworks.vpc.colors.csv";
	
	public static final int JSON_INDENT = 3;
	public static final String TEMPLATES_VPC_MAIN_VPC_JSON_VTP = "templates/vpc/main-vpc.json.vtp";
	public static final String COLORS = "colors";
	
}
