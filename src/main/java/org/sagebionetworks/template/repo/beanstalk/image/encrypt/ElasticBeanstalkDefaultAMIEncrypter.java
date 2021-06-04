package org.sagebionetworks.template.repo.beanstalk.image.encrypt;

public interface ElasticBeanstalkDefaultAMIEncrypter {
	/**
	 * Returns information about about the solution stack and encrypted ami to use given the Java, Tomcat, and Amazon Linux versions.
	 * This method will create an encrypted AMI copy of the default ElasticBeanstalk solution
	 * under your AWS account if it does not exist already.
	 * Otherwise, it will return the existing encrypted copy.
	 *
	 * Set values for Java, Tomcat, and AmazonLinux using the Java properties:
	 * org.sagebionetworks.beanstalk.image.version.java
	 * org.sagebionetworks.beanstalk.image.version.tomcat
	 * org.sagebionetworks.beanstalk.image.version.amazonlinux
	 *
	 * @see <a href="https://docs.aws.amazon.com/elasticbeanstalk/latest/platforms/platforms-supported.html#platforms-supported.java">Details about the available versions</a>
	 * @return AMI Id of the encrypted version of the default AWS AMI
	 */
	ElasticBeanstalkEncryptedPlatformInfo getEncryptedElasticBeanstalkAMI(String tomcatVersion, String javaVersion, String LinuxVersion);
}
