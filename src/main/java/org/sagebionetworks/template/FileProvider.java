package org.sagebionetworks.template;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public interface FileProvider {
	
	/**
	 * Create a temp file.
	 * @param prefix
	 * @param suffix
	 * @return
	 * @throws IOException 
	 */
	File createTempFile(String prefix, String suffix) throws IOException;

	/**
	 * Create a new file with the given parent.
	 * @param parent
	 * @param fileName
	 * @return
	 */
	File createNewFile(File parent, String fileName);
	
	/**
	 * Create a 'UTF-8' file writer for the passed file.
	 * @param parent
	 * @param fileName
	 * @return
	 */
	Writer createFileWriter(File file);

}
