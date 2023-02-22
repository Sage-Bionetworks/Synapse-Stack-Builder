package org.sagebionetworks.template.utils;

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

	/**
	 * Download the mentioned file at the given URL from zip to the local temporary file.
	 * @param url
	 * @param filename
	 * @return
	 */
	File downloadFileFromZip(String url, String filename);

}
