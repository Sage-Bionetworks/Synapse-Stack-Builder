package org.sagebionetworks.stack;

import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 * Abstraction for resources processors.
 * 
 * @author jmhill
 *
 */
public interface ResourceProcessor {
	
	/**
	 * Initialize a processor with a factory, configuration, and resources.
	 * 
	 * @param factory
	 * @param config
	 * @param resources
	 */
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources);
	
	/**
	 * Setup any resources
	 */
	public void setupResources() throws InterruptedException;
	
	/**
	 * Teardown any resources.
	 */
	public void teardownResources();

}
