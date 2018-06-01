package org.sagebionetworks.template.repo.beanstalk;

/**
 * Secret has a property key and an encrypted value.
 *
 */
public class Secret {

	String parameterName;
	String propertyKey;
	String encryptedValue;

	/**
	 * The name of the template parameter containing the value.
	 * 
	 * @return
	 */
	public String getParameterName() {
		return parameterName;
	}

	/**
	 * The name of the template parameter that will contain this value.
	 * 
	 * @param parameterName
	 * @return
	 */
	public Secret withParameterName(String parameterName) {
		this.parameterName = parameterName;
		return this;
	}

	/**
	 * The property key for this secret.
	 * 
	 * @return
	 */
	public String getPropertyKey() {
		return propertyKey;
	}

	/**
	 * The property key for this secret.
	 * 
	 * @param key
	 * @return
	 */
	public Secret withPropertyKey(String key) {
		this.propertyKey = key;
		return this;
	}

	/**
	 * The encrypted value of this secret.
	 * 
	 * @return
	 */
	public String getEncryptedValue() {
		return encryptedValue;
	}

	/**
	 * The encrypted value of this secret. 
	 * 
	 * @param encryptedValue
	 * @return
	 */
	public Secret withEncryptedValue(String encryptedValue) {
		this.encryptedValue = encryptedValue;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((encryptedValue == null) ? 0 : encryptedValue.hashCode());
		result = prime * result + ((propertyKey == null) ? 0 : propertyKey.hashCode());
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
		Secret other = (Secret) obj;
		if (encryptedValue == null) {
			if (other.encryptedValue != null)
				return false;
		} else if (!encryptedValue.equals(other.encryptedValue))
			return false;
		if (propertyKey == null) {
			if (other.propertyKey != null)
				return false;
		} else if (!propertyKey.equals(other.propertyKey))
			return false;
		return true;
	}

}
