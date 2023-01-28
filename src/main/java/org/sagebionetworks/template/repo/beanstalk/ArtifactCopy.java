package org.sagebionetworks.template.repo.beanstalk;

/**
 * Abstraction for copying a war file from Artifactory to S3,
 *
 */
public interface ArtifactCopy {

	/**
	 * Copy an artifact from 
	 * @param environment
	 * @param version
	 * @return
	 */
	public SourceBundle copyArtifactIfNeeded(EnvironmentType environment, String version, int number);
}
