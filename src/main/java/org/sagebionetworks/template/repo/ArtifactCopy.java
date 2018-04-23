package org.sagebionetworks.template.repo;

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
	public SourceBundle copyArtifactIfNeeded(Environment environment, String version);
}
