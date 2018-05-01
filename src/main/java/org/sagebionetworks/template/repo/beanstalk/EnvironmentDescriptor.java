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
	
	public EnvironmentDescriptor(String name, String refName, int number, EnvironmentType type, SourceBundle sourceBundle) {
		super();
		this.name = name;
		this.refName = refName;
		this.number = number;
		this.type = type;
		this.sourceBundle = sourceBundle;
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

	public SourceBundle getSourceBundle() {
		return sourceBundle;
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
