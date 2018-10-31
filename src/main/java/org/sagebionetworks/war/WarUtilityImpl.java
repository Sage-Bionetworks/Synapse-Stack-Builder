package org.sagebionetworks.war;

import java.io.File;
import java.io.IOException;

public class WarUtilityImpl implements WarUtility {

	@Override
	public File addDirectoryToCopyOfWar(File warToModify, File directoryToAdd) throws IOException {
		try (WarBuilder builder = new WarBuilderImpl(warToModify, directoryToAdd)) {
			return builder.builder();
		}
	}
	

}
