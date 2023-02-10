package org.sagebionetworks.template.nlb;

import java.util.Objects;

/**
 * A load balancer listener that links a network load balancer to an application
 * load balancer target group.
 *
 */
public class Listener {

	private final int port;
	private final RecordToStackMapping mapping;
	private final String healthCheckPath;
	private final int healthCheckPort;
	private final String healthCheckProtocol;

	public Listener(int port, RecordToStackMapping mapping) {
		super();
		this.port = port;
		this.mapping = mapping;
		this.healthCheckPath = mapping.getTarget() != null && mapping.getTarget().contains("repo") ? "/repo/v1/version"
				: "/";
		this.healthCheckPort = port;
		this.healthCheckProtocol = port == 443 ? "HTTPS" : "HTTP";
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

	
	/**
	 * @return the healthCheckPath
	 */
	public String getHealthCheckPath() {
		return healthCheckPath;
	}

	/**
	 * @return the healthCheckPort
	 */
	public int getHealthCheckPort() {
		return healthCheckPort;
	}

	/**
	 * @return the healthCheckProtocol
	 */
	public String getHealthCheckProtocol() {
		return healthCheckProtocol;
	}

	@Override
	public int hashCode() {
		return Objects.hash(healthCheckProtocol, healthCheckPath, healthCheckPort, mapping, port);
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
		return Objects.equals(healthCheckProtocol, other.healthCheckProtocol)
				&& Objects.equals(healthCheckPath, other.healthCheckPath) && healthCheckPort == other.healthCheckPort
				&& Objects.equals(mapping, other.mapping) && port == other.port;
	}

	@Override
	public String toString() {
		return "Listener [port=" + port + ", mapping=" + mapping + ", healthCheckPath=" + healthCheckPath
				+ ", healthCheckPort=" + healthCheckPort + ", healtchCheckProtocol=" + healthCheckProtocol + "]";
	}

}
