package org.sagebionetworks.template;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.Stack;

/**
 * Abstraction for CloudFormation Client operations that provides methods
 * for managing AWS CloudFormation stacks.
 * 
 * @author John
 *
 */
public interface CloudFormationClientWrapper {

	/**
	 * Checks if a stack with the specified name exists in CloudFormation.
	 * 
	 * @param stackName the name of the stack to check
	 * @return true if the stack exists, false otherwise
	 */
	boolean doesStackNameExist(String stackName);
	
	/**
	 * Retrieves detailed information about a stack with the specified name.
	 * 
	 * @param stackName the name of the stack to describe
	 * @return an Optional containing the Stack information if it exists, or empty if it doesn't
	 * @throws CloudFormationException if there's an error accessing CloudFormation
	 */
	Optional<Stack> describeStack(String stackName);

	/**
	 * Updates an existing CloudFormation stack using the configuration provided in the request.
	 * 
	 * @param request the CreateOrUpdateStackRequest containing stack name, template, parameters, capabilities, and tags
	 */
	void updateStack(CreateOrUpdateStackRequest request);

	/**
	 * Creates a new CloudFormation stack using the configuration provided in the request.
	 * 
	 * @param request the CreateOrUpdateStackRequest containing stack name, template, parameters, capabilities, and tags
	 */
	void createStack(CreateOrUpdateStackRequest request);

	/**
	 * Creates a new stack if it doesn't exist, or updates the stack if it already exists.
	 * This is a convenience method that combines stack existence check, creation, and update.
	 * 
	 * @param request the CreateOrUpdateStackRequest containing stack name, template, parameters, capabilities, and tags
	 */
	void createOrUpdateStack(CreateOrUpdateStackRequest request);
	
	/**
	 * Waits for a stack operation (create/update/delete) to complete by polling the stack status.
	 * Times out after a configured period if the stack operation doesn't complete.
	 * 
	 * @param stackName the name of the stack to wait for
	 * @return an Optional containing the final Stack if it exists, or empty if it doesn't
	 * @throws InterruptedException if the waiting thread is interrupted
	 * @throws RuntimeException if the stack operation fails or times out
	 */
	Optional<Stack> waitForStackToComplete(String stackName) throws InterruptedException;
	
	/**
	 * Waits for a stack operation to complete while handling CloudFormation wait conditions.
	 * Each wait condition handler in the provided set will be invoked when its corresponding
	 * wait condition is triggered during stack creation.
	 * 
	 * @param stackName the name of the stack to wait for
	 * @param waitConditionHandlers a set of handlers for processing CloudFormation wait conditions
	 * @return an Optional containing the final Stack if it exists, or empty if it doesn't
	 * @throws InterruptedException if the waiting thread is interrupted
	 * @throws RuntimeException if the stack operation fails, a wait condition fails, or the operation times out
	 */
	Optional<Stack> waitForStackToComplete(String stackName, Set<WaitConditionHandler> waitConditionHandlers) throws InterruptedException;

	/**
	 * Retrieves the value of a specific output from a CloudFormation stack.
	 *
	 * @param stackName the name of the stack containing the output
	 * @param outputKey the key of the output value to retrieve
	 * @return the value of the specified output
	 * @throws IllegalStateException if the stack doesn't exist
	 * @throws IllegalArgumentException if the output key doesn't exist in the stack
	 */
	String getOutput(String stackName, String outputKey);

	/**
	 * Creates a stream containing all CloudFormation stacks in the AWS account.
	 * This method handles pagination automatically to retrieve all stacks.
	 * 
	 * @return a Stream of Stack objects representing all stacks in the account
	 */
	Stream<Stack> streamOverAllStacks();

	/**
	 * Initiates the deletion of a CloudFormation stack with the specified name.
	 * Note that this method starts the deletion process but does not wait for it to complete.
	 * 
	 * @param stackName the name of the stack to delete
	 */
	void deleteStack(String stackName);

}
