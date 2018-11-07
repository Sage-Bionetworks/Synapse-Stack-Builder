package org.sagebionetworks.war;

import java.io.File;
import java.util.function.Consumer;

public interface WarAppender {

	/**
	 * Append files to a copy of the provided war file.
	 * 
	 * @param originalWar The original war file.
	 * @param callback Callback to append files to the war.
	 * @return A copy of the original war file containing the appended files.
	 */
	public File appendFilesCopyOfWar(File originalWar, Consumer<File> callback);
}
