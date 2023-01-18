package org.sagebionetworks.template.nlb;

import java.util.List;
import java.util.Objects;

/**
 * Context data for a single network load balancer.
 */
public class NetworkLoadBalancer {
	
	private final String name;
	private final List<String> addressNames;
	
	
	public NetworkLoadBalancer(String name, List<String> addressNames) {
		super();
		this.name = name;
		this.addressNames = addressNames;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return the addressNames
	 */
	public List<String> getAddressNames() {
		return addressNames;
	}

	@Override
	public int hashCode() {
		return Objects.hash(addressNames, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NetworkLoadBalancer)) {
			return false;
		}
		NetworkLoadBalancer other = (NetworkLoadBalancer) obj;
		return Objects.equals(addressNames, other.addressNames) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "NetworkLoadBalancer [name=" + name + ", addressNames=" + addressNames + "]";
	}
	
}
