package org.sagebionetworks.war;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public interface WarBuilder extends Closeable {

	/**
	 * Add the given directory including all files to a copy of the provided WAR
	 * file.
	 * 
	 * @param warToModify
	 * @param directoryToAdd
	 * @return The copy WAR file that will contain the provided directory.
	 * @throws IOException 
	 * @throws IllegalArgumentException If any file in the provided directory
	 *                                  already exists in the provided WAR file.
	 */
	File builder() throws IOException;
}
