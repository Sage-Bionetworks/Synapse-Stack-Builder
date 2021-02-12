package org.sagebionetworks.template;

import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.Stack;

/**
 * Abstraction for CloudFormation Client operations
 * 
 * @author John
 *
 */
public interface CloudFormationClient {

	/**
	 * Does a stack with the given name exist.
	 * 
	 * @param stackName
	 * @return
	 */
	public boolean doesStackNameExist(String stackName);
	
	/**
	 * Describe a stack given its name.
	 * 
	 * @param stackName
	 * @return
	 * @throws AmazonCloudFormationException When the stack does not exist.
	 */
	public Stack describeStack(String stackName) throws AmazonCloudFormationException;

	/**
	 * Update a stack with the given name using the provided template body.
	 * 
	 * @param stackName
	 * @param templateBody
	 * @return StackId
	 */
	public void updateStack(CreateOrUpdateStackRequest request);

	/**
	 * Create a stack with the given name using the provided template body.
	 * 
	 * @param stackName
	 * @param templateBody
	 * @return StackId
	 */
	public void createStack(CreateOrUpdateStackRequest request);

	/**
	 * If a stack does not exist the stack will be created else the stack will be
	 * updated.
	 * 
	 * @param stackName
	 * @param templateBody
	 * @return StackId
	 */
	public void createOrUpdateStack(CreateOrUpdateStackRequest request);
	
	/**
	 * Wait for the given stack to complete.
	 * @param stackName
	 * @return
	 * @throws InterruptedException 
	 */
	public Stack waitForStackToComplete(String stackName) throws InterruptedException;

	/**
	 *
	 * @param stackName
	 * @return
	 */
	public String getOutput(String stackName, String outputKey);
}
