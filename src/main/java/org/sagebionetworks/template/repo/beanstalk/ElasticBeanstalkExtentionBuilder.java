package org.sagebionetworks.template.repo.beanstalk;

import java.io.File;

/**
 * Abstraction for adding .ebextensions to a given war file.
 *
 */
public interface ElasticBeanstalkExtentionBuilder {

	/**
	 * Build the .ebextensions for the given WAR file.
	 * 
	 * @param warFile
	 */
	public void buildWarExtentions(File warFile);

}
