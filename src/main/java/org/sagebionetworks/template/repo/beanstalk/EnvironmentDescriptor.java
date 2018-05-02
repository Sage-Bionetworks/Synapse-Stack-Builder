package org.sagebionetworks.template.repo.beanstalk;

/**
 * Description of an Elastic Bean Stalk Environment.
 *
 */
public class EnvironmentDescriptor {

	String name;
	String refName;
	int number;
	EnvironmentType type;
	SourceBundle sourceBundle;
	String healthCheckUrl;
	int minInstances;
	int maxInstances;

	public String getHealthCheckUrl() {
		return healthCheckUrl;
	}

	public String getRefName() {
		return refName;
	}

	public String getName() {
		return name;
	}

	public int getNumber() {
		return number;
	}

	public String getType() {
		return type.getShortName();
	}

	public int getMinInstances() {
		return minInstances;
	}

	public int getMaxInstances() {
		return maxInstances;
	}

	public SourceBundle getSourceBundle() {
		return sourceBundle;
	}

	public EnvironmentDescriptor withMinInstances(int minInstances) {
		this.minInstances = minInstances;
		return this;
	}

	public EnvironmentDescriptor setMaxInstances(int maxInstances) {
		this.maxInstances = maxInstances;
		return this;
	}

	public EnvironmentDescriptor withName(String name) {
		this.name = name;
		return this;
	}

	public EnvironmentDescriptor withRefName(String refName) {
		this.refName = refName;
		return this;
	}

	public EnvironmentDescriptor withNumber(int number) {
		this.number = number;
		return this;
	}

	public EnvironmentDescriptor withType(EnvironmentType type) {
		this.type = type;
		return this;
	}

	public EnvironmentDescriptor withSourceBundle(SourceBundle sourceBundle) {
		this.sourceBundle = sourceBundle;
		return this;
	}

	public EnvironmentDescriptor withHealthCheckUrl(String healthCheckUrl) {
		this.healthCheckUrl = healthCheckUrl;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + number;
		result = prime * result + ((refName == null) ? 0 : refName.hashCode());
		result = prime * result + ((sourceBundle == null) ? 0 : sourceBundle.hashCode());
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
		EnvironmentDescriptor other = (EnvironmentDescriptor) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (number != other.number)
			return false;
		if (refName == null) {
			if (other.refName != null)
				return false;
		} else if (!refName.equals(other.refName))
			return false;
		if (sourceBundle == null) {
			if (other.sourceBundle != null)
				return false;
		} else if (!sourceBundle.equals(other.sourceBundle))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EnvironmentDescriptor [name=" + name + ", refName=" + refName + ", number=" + number + ", type=" + type
				+ ", sourceBundle=" + sourceBundle + "]";
	}
	
}
