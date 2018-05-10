package org.sagebionetworks.template.vpc;

import java.util.Arrays;

/**
 * Model object representing all of the sub-nets to be created.
 * 
 *
 */
public class Subnets {
	
	Subnet[] publicSubnets;
	SubnetGroup[] privateSubnetGroups;
	
	/**
	 * 
	 * @param publicSubnets The public sub-nets
	 * @param privateSubnetGroups The grouping of each colors private sub-nets
	 */
	public Subnets(Subnet[] publicSubnets, SubnetGroup[] privateSubnetGroups) {
		super();
		this.publicSubnets = publicSubnets;
		this.privateSubnetGroups = privateSubnetGroups;
	}


	/**
	 * The public sub-nets
	 * @return
	 */
	public Subnet[] getPublicSubnets() {
		return publicSubnets;
	}


	/**
	 * The grouping of each colors private sub-nets
	 * @return
	 */
	public SubnetGroup[] getPrivateSubnetGroups() {
		return privateSubnetGroups;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(privateSubnetGroups);
		result = prime * result + Arrays.hashCode(publicSubnets);
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subnets other = (Subnets) obj;
		if (!Arrays.equals(privateSubnetGroups, other.privateSubnetGroups))
			return false;
		if (!Arrays.equals(publicSubnets, other.publicSubnets))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Subnets [publicSubnets=" + Arrays.toString(publicSubnets) + ", privateSubnetGroups="
				+ Arrays.toString(privateSubnetGroups) + "]";
	}
	
	
}
