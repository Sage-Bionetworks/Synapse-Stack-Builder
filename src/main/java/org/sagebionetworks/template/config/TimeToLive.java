package org.sagebionetworks.template.config;

import java.util.List;
import java.util.Optional;

import com.amazonaws.services.cloudformation.model.Parameter;

/**
 * Help to setup and check time-to-live (TTL) parameters for a cloud formation
 * stack. A cron job is used to delete all dev stacks that have exceeded their
 * TTL.
 *
 */
public interface TimeToLive {

	/**
	 * Create an optional time-to-live (TTL) parameter. Any stack with a TTL
	 * parameter will be automatically deleted after the expiration time.
	 * 
	 * @return
	 */
	Optional<Parameter> createTimeToLiveParameter();

	/**
	 * Search for the TTL parameter in the given parameters. Will return true if the
	 * parameter's value can be parsed into a date-time that is before now, else
	 * false.
	 * 
	 * @param dateTimeString
	 * @return
	 */
	boolean isTimeToLiveExpired(List<Parameter> parameters);
}
