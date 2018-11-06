package org.sagebionetworks.template;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * Basic implementation of FileProvider.
 *
 */
public class FileProviderImpl implements FileProvider {

	@Override
	public File createTempFile(String prefix, String suffix) throws IOException {
		return File.createTempFile(prefix, suffix);
	}

	@Override
	public File createNewFile(File parent, String childName) {
		return new File(parent, childName);
	}

	@Override
	public Writer createFileWriter(File file) {
		try {
			return new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
