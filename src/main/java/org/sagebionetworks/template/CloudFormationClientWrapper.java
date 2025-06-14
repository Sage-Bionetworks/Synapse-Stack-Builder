package org.sagebionetworks.template;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.Stack;

/**
 * Abstraction for CloudFormation Client operations
 * 
 * @author John
 *
 */
public interface CloudFormationClientWrapper {

	/**
	 * Does a stack with the given name exist.
	 * 
	 * @param stackName
	 * @return
	 */
	boolean doesStackNameExist(String stackName);
	
	/**
	 * Describe a stack given its name.
	 * 
	 * @param stackName
	 * @return
	 * @throws AmazonCloudFormationException When the stack does not exist.
	 */
	Optional<Stack> describeStack(String stackName);

	/**
	 * Update a stack with the given name using the provided template body.
	 * 
	 * @param stackName
	 * @param templateBody
	 * @return StackId
	 */
	void updateStack(CreateOrUpdateStackRequest request);

	/**
	 * Create a stack with the given name using the provided template body.
	 * 
	 * @param stackName
	 * @param templateBody
	 * @return StackId
	 */
	void createStack(CreateOrUpdateStackRequest request);

	/**
	 * If a stack does not exist the stack will be created else the stack will be
	 * updated.
	 * 
	 * @param stackName
	 * @param templateBody
	 * @return StackId
	 */
	void createOrUpdateStack(CreateOrUpdateStackRequest request);
	
	/**
	 * Wait for the given stack to complete.
	 * @param stackName
	 * @return
	 * @throws InterruptedException 
	 */
	Optional<Stack> waitForStackToComplete(String stackName) throws InterruptedException;
	
	/**
	 * Wait for the given stack to complete and handles any wait condition that is provided in the map (where the key is the logical id of the wait condition)
	 * @param stackName
	 * @param waitConditionHandlers
	 * @return
	 * @throws InterruptedException
	 */
	Optional<Stack> waitForStackToComplete(String stackName, Set<WaitConditionHandler> waitConditionHandlers) throws InterruptedException;

	/**
	 *
	 * @param stackName
	 * @return
	 */
	String getOutput(String stackName, String outputKey);

	/**
	 * Stream over all stacks.
	 * @return
	 */
	Stream<Stack> streamOverAllStacks();

	/**
	 * Delete a stack by name
	 * @param stackName
	 */
	void deleteStack(String stackName);

}
