package org.sagebionetworks.template;

import java.io.File;
import java.io.IOException;

/**
 * Basic implementation of FileProvider.
 *
 */
public class FileProviderImpl implements FileProvider {

	@Override
	public File createTempFile(String prefix, String suffix) throws IOException {
		return File.createTempFile(prefix, suffix);
	}

}
