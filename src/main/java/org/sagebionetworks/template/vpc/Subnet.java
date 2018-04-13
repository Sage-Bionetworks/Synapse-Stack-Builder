package org.sagebionetworks.template.vpc;

/**
 * Basic subnet model.
 *
 */
public class Subnet {
	
	private String name;
	private String cidr;
	private SubnetType type;
	
	/**
	 * 
	 * @param name Subnet's name.
	 * @param cidr Subnet's CIDR
	 * @param type Public/Private
	 */
	public Subnet(String name, String cidr, SubnetType type) {
		super();
		this.name = name;
		this.cidr = cidr;
		this.type = type;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cidr == null) ? 0 : cidr.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Subnet other = (Subnet) obj;
		if (cidr == null) {
			if (other.cidr != null)
				return false;
		} else if (!cidr.equals(other.cidr))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

}
