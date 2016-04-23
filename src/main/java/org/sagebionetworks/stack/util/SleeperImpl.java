package org.sagebionetworks.stack.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SleeperImpl implements Sleeper {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SleeperImpl.class.getName());
	
	@Override
	public void sleep(long sleepTimeMs) {
		try {
			Thread.sleep(sleepTimeMs);
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
