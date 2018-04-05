package org.sagebionetworks.template;

import org.apache.logging.log4j.Logger;

/**
 * Abstraction for creating a logger without static method calls.
 *
 */
public interface LoggerFactory {

	/**
	 * Create a logger for the given class.
	 * @param clazz
	 * @return
	 */
	 public Logger getLogger(final Class<?> clazz);
}
