package org.sagebionetworks.template.repo.beanstalk.image.encrypt;

import java.util.Objects;

public class ElasticBeanstalkEncryptedPlatformInfo {
	private final String encryptedAmiId;
	private final String solutionStackName;

	public ElasticBeanstalkEncryptedPlatformInfo(String encryptedAmiId, String solutionStackName) {
		this.encryptedAmiId = encryptedAmiId;
		this.solutionStackName = solutionStackName;
	}

	public String getEncryptedAmiId() {
		return encryptedAmiId;
	}

	public String getSolutionStackName() {
		return solutionStackName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ElasticBeanstalkEncryptedPlatformInfo that = (ElasticBeanstalkEncryptedPlatformInfo) o;
		return Objects.equals(encryptedAmiId, that.encryptedAmiId) &&
				Objects.equals(solutionStackName, that.solutionStackName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptedAmiId, solutionStackName);
	}

	@Override
	public String toString() {
		return "ElasticBeanstalkEncryptedPlatformInfo{" +
				"encryptedAmiId='" + encryptedAmiId + '\'' +
				", solutionStackName='" + solutionStackName + '\'' +
				'}';
	}
}
