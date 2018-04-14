package org.sagebionetworks.template.vpc;

import java.util.Arrays;

/**
 * 
 * Represents a group of public and private sub-nets. Each group is identified
 * by a color.
 * 
 *
 */
public class SubnetGroup {

	private Color color;
	private String cidr;
	private Subnet[] subnets;

	/**
	 * 
	 * @param color The color identifier of this sub-net group.
	 * @param The CIDR for this group of subnets.
	 * @param publicCidrs The CIDRs for the public sub-nets for this group.
	 * @param privateCidrs The CIDRs for the private sub-nets for this group.
	 */
	public SubnetGroup(Color color, String cidr, Subnet[] subnets) {
		super();
		this.color = color;
		this.cidr = cidr;
		this.subnets = subnets;
	}

	/**
	 * The color identifier of this sub-net group.
	 * @return
	 */
	public String getColor() {
		return color.name();
	}

	public Subnet[] getSubnets() {
		return subnets;
	}

	/**
	 * The CIDR for this group of subnets.
	 * @return
	 */
	public String getCidr() {
		return cidr;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cidr == null) ? 0 : cidr.hashCode());
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + Arrays.hashCode(subnets);
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
		SubnetGroup other = (SubnetGroup) obj;
		if (cidr == null) {
			if (other.cidr != null)
				return false;
		} else if (!cidr.equals(other.cidr))
			return false;
		if (color != other.color)
			return false;
		if (!Arrays.equals(subnets, other.subnets))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubnetGroup [color=" + color + ", cidr=" + cidr + ", subnets=" + Arrays.toString(subnets) + "]";
	}


}
