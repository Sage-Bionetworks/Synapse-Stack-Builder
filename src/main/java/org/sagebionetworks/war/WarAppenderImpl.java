package org.sagebionetworks.war;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;

public class WarAppenderImpl implements WarAppender {

	@Override
	public File appendFilesCopyOfWar(File originalWar, AppenderCallback callback) {
		File tempDir = null;
		try {
			tempDir = Files.createTempDirectory("warCopy").toFile();
			// Unzip the war to the temp dir
			WarUtilities.unzipWarToDirectory(originalWar, tempDir);
			// create the ebextensions directory
			callback.appendFilesToDirectory(tempDir);
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
