package org.sagebionetworks.template.repo.beanstalk;

import com.amazonaws.services.cloudformation.model.Stack;

/**
 * Abstraction for building the runtime configuration property files.
 *
 */
public interface EnvironmentConfiguration {

	/**
	 * Create the runtime environment configuration property file and upload
	 * the file to S3
	 * @param sharedStackResults 
	 * @param descriptor
	 * @return The URL of the resulting S3 file.
	 */
	String createEnvironmentConfiguration(Stack sharedStackResults);
}
