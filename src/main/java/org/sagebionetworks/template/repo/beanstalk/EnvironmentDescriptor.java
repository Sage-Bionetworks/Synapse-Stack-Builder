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
	String versionLabel;
	String sslCertificateARN;
	String hostedZone;
	String cnamePrefix;
	SourceBundle secretsSource;

	public String getVersionLabel() {
		return versionLabel;
	}

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
	
	public String getInstanceProfileSuffix() {
		return type.getInstanceProfileSuffix();
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

	public String getSslCertificateARN() {
		return sslCertificateARN;
	}

	public String getHostedZone() {
		return hostedZone;
	}

	public String getCnamePrefix() {
		return cnamePrefix;
	}
	
	public EnvironmentDescriptor withHostedZone(String hostedZone) {
		this.hostedZone = hostedZone;
		return this;
	}
	
	public EnvironmentDescriptor withSslCertificateARN(String sslCertificateARN) {
		this.sslCertificateARN = sslCertificateARN;
		return this;
	}

	public EnvironmentDescriptor withCnamePrefix(String cnamePrefix) {
		this.cnamePrefix = cnamePrefix;
		return this;
	}

	public EnvironmentDescriptor withVersionLabel(String versionLabel) {
		this.versionLabel = versionLabel;
		return this;
	}

	public EnvironmentDescriptor withMinInstances(int minInstances) {
		this.minInstances = minInstances;
		return this;
	}

	public EnvironmentDescriptor withMaxInstances(int maxInstances) {
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
	
	public SourceBundle getSecretsSource() {
		return secretsSource;
	}

	public EnvironmentDescriptor withSecretsSource(SourceBundle secrets) {
		this.secretsSource = secrets;
		return this;
	}

	/**
	 * Is the type repository or workers?
	 * @return
	 */
	public boolean isTypeRepositoryOrWorkers() {
		return EnvironmentType.REPOSITORY_SERVICES.equals(type) || EnvironmentType.REPOSITORY_WORKERS.equals(type);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cnamePrefix == null) ? 0 : cnamePrefix.hashCode());
		result = prime * result + ((healthCheckUrl == null) ? 0 : healthCheckUrl.hashCode());
		result = prime * result + ((hostedZone == null) ? 0 : hostedZone.hashCode());
		result = prime * result + maxInstances;
		result = prime * result + minInstances;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + number;
		result = prime * result + ((refName == null) ? 0 : refName.hashCode());
		result = prime * result + ((secretsSource == null) ? 0 : secretsSource.hashCode());
		result = prime * result + ((sourceBundle == null) ? 0 : sourceBundle.hashCode());
		result = prime * result + ((sslCertificateARN == null) ? 0 : sslCertificateARN.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((versionLabel == null) ? 0 : versionLabel.hashCode());
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
		if (cnamePrefix == null) {
			if (other.cnamePrefix != null)
				return false;
		} else if (!cnamePrefix.equals(other.cnamePrefix))
			return false;
		if (healthCheckUrl == null) {
			if (other.healthCheckUrl != null)
				return false;
		} else if (!healthCheckUrl.equals(other.healthCheckUrl))
			return false;
		if (hostedZone == null) {
			if (other.hostedZone != null)
				return false;
		} else if (!hostedZone.equals(other.hostedZone))
			return false;
		if (maxInstances != other.maxInstances)
			return false;
		if (minInstances != other.minInstances)
			return false;
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
		if (secretsSource == null) {
			if (other.secretsSource != null)
				return false;
		} else if (!secretsSource.equals(other.secretsSource))
			return false;
		if (sourceBundle == null) {
			if (other.sourceBundle != null)
				return false;
		} else if (!sourceBundle.equals(other.sourceBundle))
			return false;
		if (sslCertificateARN == null) {
			if (other.sslCertificateARN != null)
				return false;
		} else if (!sslCertificateARN.equals(other.sslCertificateARN))
			return false;
		if (type != other.type)
			return false;
		if (versionLabel == null) {
			if (other.versionLabel != null)
				return false;
		} else if (!versionLabel.equals(other.versionLabel))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EnvironmentDescriptor [name=" + name + ", refName=" + refName + ", number=" + number + ", type=" + type
				+ ", sourceBundle=" + sourceBundle + ", healthCheckUrl=" + healthCheckUrl + ", minInstances="
				+ minInstances + ", maxInstances=" + maxInstances + ", versionLabel=" + versionLabel
				+ ", sslCertificateARN=" + sslCertificateARN + ", hostedZone=" + hostedZone + ", cnamePrefix="
				+ cnamePrefix + ", secretsSource=" + secretsSource + "]";
	}
		
}
