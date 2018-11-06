package org.sagebionetworks.war;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class WarUtilitiesTest {

	String[] files;

	@Before
	public void before() {

		files = new String[] { "WEB-INF/web.xml", "WEB-INF/lib/fake.jar",
				"WEB-INF/classes/org/sagebioneworks/sample.java", "META-INF/MANIFEST.MF", "index.html" };
	}

	@Test
	public void testZipAndUnzipWar() throws IOException {
		File tempDir = null;
		File tempWar = null;
		try {
			tempDir = Files.createTempDirectory("start-war").toFile();
			// Add some files to the dir
			writeTestFilesToDirectory(tempDir);
			// Create a war from the dir
			tempWar = File.createTempFile("TestWar", ".war");
			// call under test
			WarUtilities.zipDirectoryToWar(tempDir, tempWar);
			// delete the temp dir
			FileUtils.deleteDirectory(tempDir);
			// create a new temp
			tempDir = Files.createTempDirectory("end-war").toFile();
			// call under test
			WarUtilities.unzipWarToDirectory(tempWar, tempDir);
			// validate the results
			validateFiles(tempDir);
		}finally {
			if(tempDir != null) {
				FileUtils.deleteDirectory(tempDir);
			}
			if(tempWar != null) {
				tempWar.delete();
			}
		}
	}

	public void validateFiles(File directory) throws IOException {
		for(String fileName: files) {
			File file = new File(directory, fileName);
			assertTrue(file.exists());
			byte[] encoded = Files.readAllBytes(file.toPath());
			String contents =  new String(encoded, "UTF-8");
			assertEquals(fileName, contents);
		}
	}

	public void writeTestFilesToDirectory(File directory) throws IOException {
		for (String fileName : files) {
			File file = new File(directory, fileName);
			FileUtils.write(file, fileName, "UTF-8");
		}
	}
}
