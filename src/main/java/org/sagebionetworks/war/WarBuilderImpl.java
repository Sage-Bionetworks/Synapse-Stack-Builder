package org.sagebionetworks.war;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;

public class WarBuilderImpl implements WarBuilder {

	File warToModify;
	File directoryToAdd;

	File tempDirectory;
	File resultWar;

	public WarBuilderImpl(File warToModify, File directoryToAdd) {
		super();
		this.warToModify = warToModify;
		this.directoryToAdd = directoryToAdd;
	}

	@Override
	public void close() throws IOException {
		if (tempDirectory != null) {
			FileUtils.deleteDirectory(tempDirectory);
		}
		if(resultWar != null) {
			resultWar.delete();
		}
	}

	@Override
	public File builder() throws IOException {
		// create the temporary directory for the war
		tempDirectory = Files.createTempDirectory(warToModify.getName()).toFile();
		// unzip the WAR to the temp directory
		unzipWarToDirectory(warToModify, tempDirectory);
		// copy the files
		FileUtils.copyDirectory(directoryToAdd, tempDirectory);
		
		resultWar = Files.createTempFile("WarCopy", ".WAR").toFile();
		return null;
	}

	/**
	 * Unzip the given WAR file to the provided destination.
	 * This is from 
	 * @param war
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ArchiveException
	 */
	public static void unzipWarToDirectory(File war, File targetDir)
			throws IOException {
		try (InputStream fis = Files.newInputStream(war.toPath());
				ArchiveInputStream archiveInput = new ArchiveStreamFactory().createArchiveInputStream(fis)) {
			ArchiveEntry entry = null;
			while ((entry = archiveInput.getNextEntry()) != null) {
				if (!archiveInput.canReadEntryData(entry)) {
					throw new IOException("failed to read entry: " + entry.getName());
				}
				File newFile = new File(targetDir, entry.getName());
				if (entry.isDirectory()) {
					if (!newFile.isDirectory() && !newFile.mkdirs()) {
						throw new IOException("failed to create directory " + newFile);
					}
				} else {
					File parent = newFile.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("failed to create directory " + parent);
					}
					try (OutputStream output = Files.newOutputStream(newFile.toPath())) {
						IOUtils.copy(archiveInput, output);
					}
				}
			}
		} catch (ArchiveException e) {
			throw new IOException(e);
		}
	}

	public static void zipDirectoryToWar(File sourceDirectory, File destinationWar) {
		try (OutputStream out = Files.newOutputStream(destinationWar.toPath());
				CompressorOutputStream o = new CompressorStreamFactory()
			    .createCompressorOutputStream(CompressorStreamFactory.GZIP, out)) {
		    for (File f : filesToArchive) {
		        // maybe skip directories for formats like AR that don't store directories
		        ArchiveEntry entry = o.createArchiveEntry(f, entryName(f));
		        // potentially add more flags to entry
		        o.putArchiveEntry(entry);
		        if (f.isFile()) {
		            try (InputStream i = Files.newInputStream(f.toPath())) {
		                IOUtils.copy(i, o);
		            }
		        }
		        o.closeArchiveEntry();
		    }
		    out.finish();
		}
	}
}
