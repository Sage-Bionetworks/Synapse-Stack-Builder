package org.sagebionetworks.template.nlb;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.template.ip.address.IpAddressPoolBuilderImpl;

/**
 * Context data for a single network load balancer.
 */
public class NetworkLoadBalancer {
	
	private final RecordName record;
	private final List<String> addressNames;
	
	
	public NetworkLoadBalancer(RecordName record, int numberAzPerNlb) {
		super();
		this.record = record;
		addressNames = new ArrayList<>(numberAzPerNlb);
		for(int az=0; az<numberAzPerNlb; az++) {
			addressNames.add(IpAddressPoolBuilderImpl.ipAddressName(record.getShortName(), az));
		}
	}


	/**
	 * @return the record
	 */
	public RecordName getRecord() {
		return record;
	}


	/**
	 * @return the addressNames
	 */
	public List<String> getAddressNames() {
		return addressNames;
	}


	@Override
	public int hashCode() {
		return Objects.hash(addressNames, record);
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
		return Objects.equals(addressNames, other.addressNames) && Objects.equals(record, other.record);
	}


	@Override
	public String toString() {
		return "NetworkLoadBalancer [domain=" + record + ", addressNames=" + addressNames + "]";
	}
	
}
