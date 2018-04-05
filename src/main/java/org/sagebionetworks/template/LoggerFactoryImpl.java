package org.sagebionetworks.template;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple wrapper for the static LogManager.getLogger(clazz) method.
 *
 */
public class LoggerFactoryImpl implements LoggerFactory {

	@Override
	public Logger getLogger(Class<?> clazz) {
		return LogManager.getLogger(clazz);
	}

}
