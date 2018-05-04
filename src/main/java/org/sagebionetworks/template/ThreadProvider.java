package org.sagebionetworks.template;

/**
 * Abstraction for static thread operations.
 *
 *
 */
public interface ThreadProvider {

	/**
	 * Sleep for the provided number of milliseconds.
	 * 
	 * @param sleepMS
	 * @throws InterruptedException
	 */
	public void sleep(long sleepMS) throws InterruptedException;

	/**
	 * Get the current time in milliseconds.
	 * 
	 * @return
	 */
	public long currentTimeMillis();

}
