package org.sagebionetworks.war;

import java.io.File;
import java.io.IOException;

public interface AppenderCallback {

	/**
	 * All files to be appended to the WAR file should be
	 * added to the provided directory.
	 * 
	 * @param dir
	 * @throws IOException 
	 */
	public void appendFilesToDirectory(File dir) throws IOException;
}
