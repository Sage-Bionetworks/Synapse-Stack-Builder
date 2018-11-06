package org.sagebionetworks.war;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;

public class WarAppenderImpl implements WarAppender {

	@Override
	public File appendFilesToWar(File war, AppenderCallback callback) {
		File tempDir = null;
		try {
			tempDir = Files.createTempDirectory("warCopy").toFile();
			// Unzip the war to the temp dir
			WarUtilities.unzipWarToDirectory(war, tempDir);
			// let the caller add files to the war
			callback.addFilesToDirectory(tempDir);
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
