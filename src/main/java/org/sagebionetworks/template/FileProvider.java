package org.sagebionetworks.template;

import java.io.File;
import java.io.IOException;

public interface FileProvider {
	
	/**
	 * Create a temp file.
	 * @param prefix
	 * @param suffix
	 * @return
	 * @throws IOException 
	 */
	File createTempFile(String prefix, String suffix) throws IOException;

}
