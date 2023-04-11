package org.sagebionetworks.template.utils;

import java.io.File;

/**
 * Abstraction for downloading an Artifact.
 */
public interface ArtifactDownload {

    /**
     * Download the file at the given URL to the local temporary file.
     *
     * @param url
     * @return
     */
	File downloadFile(String url);

}
