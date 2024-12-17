package org.sagebionetworks.template;

import java.util.Optional;

import com.amazonaws.services.cloudformation.model.StackEvent;

/**
 * Interface for an handler of a wait condition defined in a cloud formation template. Note that wait conditions updates are not supported and will be invoked only when the stack is created.
 */
public interface WaitConditionHandler {

	/**
	 * @return The logical resource id for the wait condition defined in the cloud formation template, this handler will be mapped to the returned id
	 */
	String getWaitConditionId();
	
	/**
	 * When the last wait condition event matches the {@link #getWaitConditionId()} and the event is in the CREATE_IN_PROGRESS status this handler will
	 * be invoked. The handle implementation should be idempotent since it is possible that it is invoked multiple times during the stack creation.
	 * 
	 * @param stackEvent
	 * @return An optional signal id to send back to cloud formation if the condition could be processed
	 */
	Optional<String> handle(StackEvent stackEvent) throws InterruptedException;
	
}
