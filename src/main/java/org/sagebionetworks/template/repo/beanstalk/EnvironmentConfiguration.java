package org.sagebionetworks.template.repo.beanstalk;

/**
 * Abstraction for building the runtime configuration property files.
 *
 */
public interface EnvironmentConfiguration {

	/**
	 * Create the runtime environment configuration property file and upload
	 * the file to S3
	 * @param descriptor
	 * @return The URL of the resulting S3 file.
	 */
	String createEnvironmentConfiguration();
}
