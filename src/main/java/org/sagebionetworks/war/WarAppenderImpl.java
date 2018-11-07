package org.sagebionetworks.war;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

import com.google.inject.Inject;

public class WarAppenderImpl implements WarAppender {
	
	@Inject
	public WarAppenderImpl() {
	}

	@Override
	public File appendFilesCopyOfWar(File originalWar, Consumer<File> callback) {
		File tempDir = null;
		try {
			tempDir = Files.createTempDirectory("warCopy").toFile();
			// Unzip the war to the temp dir
			WarUtilities.unzipWarToDirectory(originalWar, tempDir);
			// create the ebextensions directory
			callback.accept(tempDir);
			// Zip the war into a new file
			File warCopy = File.createTempFile("WarCopy", ".war");
			WarUtilities.zipDirectoryToWar(tempDir, warCopy);
			return warCopy;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (tempDir != null) {
				try {
					FileUtils.deleteDirectory(tempDir);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

}
