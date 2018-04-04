package org.sagebionetworks.template;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.google.inject.Inject;

/**
 * Basic implementation CloudFormationClient 
 *
 */
public class CloudFormationClientImpl implements CloudFormationClient {

	AmazonCloudFormation cloudFormationClient;
	
	@Inject
	public CloudFormationClientImpl(AmazonCloudFormation cloudFormationClient) {
		super();
		this.cloudFormationClient = cloudFormationClient;
	}

	@Override
	public boolean doesStackNameExist(String stackName) {
		try {
			// describe will throw an AmazonCloudFormationException if the stack does not exist.
			describeStack(stackName);
			return true;
		} catch (AmazonCloudFormationException e) {
			// the stack does not exist
			return false;
		}
	}

	@Override
	public String updateStack(String stackName, String templateBody,Parameter... parameters) {
		UpdateStackRequest request = new UpdateStackRequest();
		request.setStackName(stackName);
		request.setTemplateBody(templateBody);
		request.withParameters(parameters);
		UpdateStackResult results = this.cloudFormationClient.updateStack(request);
		return results.getStackId();
	}

	@Override
	public String createStack(String stackName, String templateBody, Parameter... parameters) {
		CreateStackRequest request = new CreateStackRequest().withStackName(stackName);
		request.setStackName(stackName);
		request.setTemplateBody(templateBody);
		request.withParameters(parameters);
		CreateStackResult result = this.cloudFormationClient.createStack(request);
		return result.getStackId();
	}

	@Override
	public String createOrUpdateStack(String stackName, String templateBody, Parameter... parameters) {
		if(doesStackNameExist(stackName)) {
			return updateStack(stackName, templateBody, parameters);
		}else {
			return createStack(stackName, templateBody, parameters);
		}
	}

	@Override
	public Stack describeStack(String stackName) throws AmazonCloudFormationException {
		DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
		// throws an exception if it does not exist
		DescribeStacksResult results = cloudFormationClient.describeStacks(request);
		if(results.getStacks().size() > 1) {
			throw new IllegalStateException("More than one stack found for name: "+stackName);
		}
		return results.getStacks().get(0);
	}

}
