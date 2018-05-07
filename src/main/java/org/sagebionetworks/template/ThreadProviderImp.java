package org.sagebionetworks.template;

/**
 * Simple implementation of a thread provider.
 *
 */
public class ThreadProviderImp implements ThreadProvider {

	@Override
	public void sleep(long sleepMS) throws InterruptedException {
		Thread.sleep(sleepMS);
	}

	@Override
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

}
