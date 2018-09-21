package org.sagebionetworks.template.vpc;

import org.sagebionetworks.template.Constants;

/**
 * Basic subnet model.
 *
 */
public class Subnet {
	
	private String name;
	private String cidr;
	private SubnetType type;
	private String availabilityZone;
	
	/**
	 * 
	 * @param name Subnet's name.
	 * @param cidr Subnet's CIDR
	 * @param type Public/Private
	 * @param availabilityZone The AWS availability Zone where this subnet will reside.
	 */
	public Subnet(String name, String cidr, SubnetType type, String availabilityZone) {
		super();
		this.name = name;
		this.cidr = cidr;
		this.type = type;
		this.availabilityZone = availabilityZone;
	}

	/**
	 * Subnet's name.
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Subnet's CIDR
	 * @return
	 */
	public String getCidr() {
		return cidr;
	}

	/**
	 * Public/Private
	 * @return
	 */
	public String getType() {
		return type.name();
	}

	/**
	 * The AWS availability Zone where this subnet will reside.
	 * @return
	 */
	public String getAvailabilityZone() {
		return availabilityZone;
	}
	
	/**
	 * The name of an availability zone in camel-case.
	 * @return
	 */
	public String getAvailabilityZoneRef() {
		return Constants.createCamelCaseName(availabilityZone);
	}
	
	
}
