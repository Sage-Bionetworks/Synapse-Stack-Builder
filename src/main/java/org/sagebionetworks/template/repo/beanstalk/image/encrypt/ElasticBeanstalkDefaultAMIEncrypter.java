package org.sagebionetworks.template.repo.beanstalk.image.encrypt;

public interface ElasticBeanstalkDefaultAMIEncrypter {
	/**
	 * Returns information about about the solution stack and encrypted ami to use given the Java, Tomcat, and Amazon Linuz versions.
	 * This method will create an encrypted AMI copy of the default Elasticbeanstak solution
	 * under your AWS account if it does not exist already.
	 * Otherwise, it will return the existing encrypted copy.
	 *
	 * @see <a href="https://docs.aws.amazon.com/elasticbeanstalk/latest/platforms/platforms-supported.html#platforms-supported.java">Details about the available versions</a>
	 * @param javaVersion version of Java
	 * @param tomcatVersion version of Tomcat
	 * @param amazonLinuxVersion version of Amazon Linux. If null, will use the latest version of Amazon Linux
	 * @return AMI Id of the encrypted version of the default AWS AMI
	 */
	ElasticBeanstalkEncryptedPlatformInfo getEncryptedElasticBeanstalkAMI(String javaVersion, String tomcatVersion, String amazonLinuxVersion);
}
