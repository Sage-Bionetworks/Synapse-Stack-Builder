package org.sagebionetworks.template.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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

    /**
     * Download the content at the given URL as an InputStream.
     * The caller is responsible for closing the stream.
     *
     * @param url The URL to download from
     * @return An InputStream containing the content
     */
    InputStream downloadAsStream(String url);

    byte[] downloadAsBytes(String url);
}
