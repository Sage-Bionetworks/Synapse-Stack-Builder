package org.sagebionetworks.template;

import java.util.Optional;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;

public interface WaitConditionHandler {

	String getWaitConditionId();
	
	Optional<String> handle(Stack stack, StackEvent stackEvent);
	
}
