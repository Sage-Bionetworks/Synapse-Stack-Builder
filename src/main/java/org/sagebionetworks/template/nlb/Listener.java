package org.sagebionetworks.template.nlb;

import java.util.Objects;

/**
 * A load balancer listener that links a network load balancer to an application load balancer target group.
 *
 */
public class Listener {
	
	private final int port;
	private final RecordToStackMapping mapping;
	
	public Listener(int port, RecordToStackMapping mapping) {
		super();
		this.port = port;
		this.mapping = mapping;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return the mapping
	 */
	public RecordToStackMapping getMapping() {
		return mapping;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mapping, port);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Listener)) {
			return false;
		}
		Listener other = (Listener) obj;
		return Objects.equals(mapping, other.mapping) && port == other.port;
	}

	@Override
	public String toString() {
		return "Listener [port=" + port + ", mapping=" + mapping + "]";
	}

}
