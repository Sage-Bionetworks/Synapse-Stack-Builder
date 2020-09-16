package org.sagebionetworks.template.repo.beanstalk.ssl;

import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import java.io.File;

/**
 * Abstraction for adding .ebextensions to a given war file.
 *
 */
public interface ElasticBeanstalkExtentionBuilder {

	/**
	 * Create a copy of the given war file that includes
	 * the .ebextensions directory.
	 * 
	 * @param warFile
	 */
	public File copyWarWithExtensions(File warFile, EnvironmentType envType);

}
