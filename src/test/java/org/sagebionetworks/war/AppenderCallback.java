package org.sagebionetworks.war;

import java.io.File;

public interface AppenderCallback {

	/**
	 * Append all files to the passed directory.
	 * @param directory
	 */
	public void addFilesToDirectory(File directory);
}
