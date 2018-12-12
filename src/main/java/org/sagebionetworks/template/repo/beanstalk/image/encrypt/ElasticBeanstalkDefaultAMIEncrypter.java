package org.sagebionetworks.template.repo.beanstalk.image.encrypt;

public interface ElasticBeanstalkDefaultAMIEncrypter {
	ElasticBeanstalkEncryptedPlatformInfo getEncryptedElasticBeanstalkInfo(String javaVersion, String tomcatVersion, String amazonLinuxVersion);
}
