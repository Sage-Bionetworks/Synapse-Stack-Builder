package org.sagebionetworks.template.repo;

import java.io.File;

/**
 * Abstraction for downloading an Artifact.
 *
 */
public interface ArtifactDownload {
	
	/**
	 * Download the file at the given URL to the local temporary file.
	 * @param url
	 * @return
	 */
	public File downloadFile(String url);

}
