package org.sagebionetworks.template.repo.beanstalk.image.encrypt;

public class ElasticBeanstalkEncryptedPlatformInfo {
	final String encryptedAmiId;
	final String solutionStackName;

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
}
