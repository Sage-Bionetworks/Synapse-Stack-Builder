package org.sagebionetworks.template;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;

public interface WaitConditionHandler {

	String getWaitConditionId();
	
	String getSignalId();
	
	void handle(Stack stack, StackEvent stackEvent);
	
}
