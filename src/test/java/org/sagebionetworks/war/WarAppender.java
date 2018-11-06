package org.sagebionetworks.war;

import java.io.File;

public interface WarAppender {

	/**
	 * Append files to a copy of the passed war file.
	 * 
	 * @param war      The original war;
	 * 
	 * @param callback
	 * @return The resulting copy of the war with the appended files.
	 */
	File appendFilesToWar(File war, AppenderCallback callback);

}
