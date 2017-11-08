package org.sagebionetworks.stack.util;

import org.apache.log4j.Logger;
import java.lang.InterruptedException;


public class SleeperImpl implements Sleeper {
	private static Logger logger = Logger.getLogger(SleeperImpl.class.getName());
	
	@Override
	public void sleep(long sleepTimeMs)  throws InterruptedException {
		Thread.sleep(sleepTimeMs);
	}
	
}
