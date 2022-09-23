package org.sagebionetworks.template.repo.beanstalk;

public interface ElasticBeanstalkSolutionStackNameProvider {
	/**
	 * Returns information about the solution stack to use given the Java, Tomcat, and Amazon Linux versions.
	 *
	 * Set values for Java, Tomcat, and AmazonLinux using the Java properties:
	 * org.sagebionetworks.beanstalk.image.version.java
	 * org.sagebionetworks.beanstalk.image.version.tomcat
	 * org.sagebionetworks.beanstalk.image.version.amazonlinux
	 *
	 * @see <a href="https://docs.aws.amazon.com/elasticbeanstalk/latest/platforms/platforms-supported.html#platforms-supported.java">Details about the available versions</a>
	 * @return Beanstalk solution stack name
	 */
	String getSolutionStackName(String tomcatVersion, String javaVersion, String LinuxVersion);
}
