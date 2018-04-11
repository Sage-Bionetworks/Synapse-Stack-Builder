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
	private String[] publicCidrs;
	private String[] privateCidrs;

	/**
	 * 
	 * @param color The color identifier of this sub-net group.
	 * @param publicCidrs The CIDRs for the public sub-nets for this group.
	 * @param privateCidrs The CIDRs for the private sub-nets for this group.
	 */
	public SubnetGroup(Color color, String[] publicCidrs, String[] privateCidrs) {
		super();
		this.color = color;
		this.publicCidrs = publicCidrs;
		this.privateCidrs = privateCidrs;
	}

	/**
	 * The color identifier of this sub-net group.
	 * @return
	 */
	public String getColor() {
		return color.name();
	}

	/**
	 * The CIDRs for the public sub-nets for this group.
	 * @return
	 */
	public String[] getPublicCidrs() {
		return publicCidrs;
	}

	/**
	 * The CIDRs for the private sub-nets for this group.
	 * 
	 * @return
	 */
	public String[] getPrivateCidrs() {
		return privateCidrs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + Arrays.hashCode(privateCidrs);
		result = prime * result + Arrays.hashCode(publicCidrs);
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
		if (color != other.color)
			return false;
		if (!Arrays.equals(privateCidrs, other.privateCidrs))
			return false;
		if (!Arrays.equals(publicCidrs, other.publicCidrs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubnetGroup [color=" + color + ", publicCidrs=" + Arrays.toString(publicCidrs) + ", privateCidrs="
				+ Arrays.toString(privateCidrs) + "]";
	}

}
