package org.sagebionetworks.war;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

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
		if (resultWar != null) {
			resultWar.delete();
		}
	}

	@Override
	public File builder() throws IOException {
		// create the temporary directory for the war
		tempDirectory = Files.createTempDirectory(warToModify.getName()).toFile();
		// Create a new file to contain the resulting war
		resultWar = Files.createTempFile("WarCopy", ".war").toFile();
		// unzip the WAR to the temp directory
		unzipWarToDirectory(warToModify, tempDirectory);
		// copy the files to the temp directory containing the war contents.
		FileUtils.copyDirectory(directoryToAdd, tempDirectory);
		// create the WAR copy
		zipDirectoryToWar(tempDirectory, resultWar);
		File finalFile = resultWar;
		resultWar = null;
		return finalFile;
	}

	/**
	 * Unzip the given WAR file to the provided destination. This is from
	 * 
	 * @param war
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ArchiveException
	 */
	public static void unzipWarToDirectory(File war, File targetDir) throws IOException {
		try (InputStream fis = Files.newInputStream(war.toPath());
				BufferedInputStream bis = new BufferedInputStream(fis);
				ZipInputStream zipInputStream = new ZipInputStream(bis)) {
			ZipEntry entry = null;
			while ((entry = zipInputStream.getNextEntry()) != null) {
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
						IOUtils.copy(zipInputStream, output);
					}
				}
			}
		}
	}

	/**
	 * Write the given source directory to the provided new WAR file.
	 * 
	 * @param sourceDirectory
	 * @param destinationWar
	 * @throws IOException
	 */
	public static void zipDirectoryToWar(File sourceDirectory, File destinationWar) throws IOException {
		try (OutputStream fos = Files.newOutputStream(destinationWar.toPath());
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				ZipOutputStream zipOut = new ZipOutputStream(bos)) {
			Iterator<File> it = FileUtils.iterateFiles(sourceDirectory, TrueFileFilter.INSTANCE,
					TrueFileFilter.INSTANCE);
			while (it.hasNext()) {
				File file = it.next();
				Path relativePath = sourceDirectory.toPath().relativize(file.toPath());
				StringBuilder nameBuilder = new StringBuilder(relativePath.toString());
				if(file.isDirectory()) {
					nameBuilder.append("/");
				}
				ZipEntry entry = new ZipEntry(nameBuilder.toString());
				zipOut.putNextEntry(entry);
				if (!file.isDirectory()) {
					try (InputStream childIn = new FileInputStream(file);) {
						IOUtils.copy(childIn, zipOut);
					}
				}
				zipOut.closeEntry();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		try(WarBuilderImpl builder = new WarBuilderImpl(new File(args[0]), new File(args[1]))){
			File resultWar = builder.builder();
			System.out.println("Result WAR: "+resultWar.getAbsolutePath());
		}
	}
}
