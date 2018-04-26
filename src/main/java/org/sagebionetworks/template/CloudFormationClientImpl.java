package org.sagebionetworks.template;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

import org.sagebionetworks.template.repo.RepositoryPropertyProvider;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;

import com.amazonaws.HttpMethod;
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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.inject.Inject;

/**
 * Basic implementation CloudFormationClient 
 *
 */
public class CloudFormationClientImpl implements CloudFormationClient {

	AmazonCloudFormation cloudFormationClient;
	AmazonS3 s3Client;
	RepositoryPropertyProvider propertyProvider;
	
	@Inject
	public CloudFormationClientImpl(AmazonCloudFormation cloudFormationClient, AmazonS3 s3Client, RepositoryPropertyProvider propertyProvider) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.s3Client = s3Client;
		this.propertyProvider = propertyProvider;
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
		// Temporarily upload the template to S3.
		return executeWithS3Template(stackName, templateBody, new Function<String, String>() {
			
			@Override
			public String apply(String templateUrl) {
				UpdateStackRequest request = new UpdateStackRequest();
				request.setStackName(stackName);
				request.setTemplateURL(templateUrl);
				request.withParameters(parameters);
				UpdateStackResult results = cloudFormationClient.updateStack(request);
				return results.getStackId();
			}
		});
	}

	@Override
	public String createStack(final String stackName, final String templateBody, final Parameter... parameters) {
		// Temporarily upload the template to S3.
		return executeWithS3Template(stackName, templateBody, new Function<String, String>() {
			
			@Override
			public String apply(String templateUrl) {
				CreateStackRequest request = new CreateStackRequest().withStackName(stackName);
				request.setStackName(stackName);
				request.setTemplateURL(templateUrl);
				request.withParameters(parameters);
				CreateStackResult result = cloudFormationClient.createStack(request);
				return result.getStackId();
			}
		});
	}
	
	/**
	 * Execute a create or update using a template that is temporarily uploaded to S3.
	 * @param stackName
	 * @param templateBody
	 * @param function
	 * @return
	 */
	String executeWithS3Template(String stackName, String templateBody, Function<String, String> function) {
		// save the template file to S3
		SourceBundle bundle = saveTempalteToS3(stackName, templateBody);
		try {
			// provide an pre-signed URL to the template in S3
			String templateUrl = createPresignedUrl(bundle);
			// the function executes the create or update.
			return function.apply(templateUrl);
		}finally {
			// Delete the template from S3
			deleteTemplate(bundle);
		}
	}

	@Override
	public String createOrUpdateStack(String stackName, String templateBody, Parameter... parameters) {
		if(doesStackNameExist(stackName)) {
			return updateStack(stackName, templateBody, parameters);
		}else {
			return createStack(stackName, templateBody, parameters);
		}
	}

	/**
	 * Describe the stack with the given name
	 */
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
	
	/**
	 * Save the given template to to S3.
	 * @param tempalte
	 * @return
	 */
	SourceBundle saveTempalteToS3(String stackName, String tempalte) {
		try {
			String stack = propertyProvider.get(Constants.PROPERTY_KEY_STACK);
			String bucket = Constants.getConfigurationBucket(stack);
			String key = "templates/" + stackName + "-" + UUID.randomUUID() + ".json";
			byte[] bytes = tempalte.getBytes("UTF-8");
			ByteArrayInputStream input = new ByteArrayInputStream(bytes);
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(bytes.length);
			s3Client.putObject(bucket, key, input, metadata);
			return new SourceBundle(bucket, key);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Create a pre-signed URL for the given file.
	 * @param bundle
	 * @return
	 */
	String createPresignedUrl(SourceBundle bundle) {
		Date expiration = new Date(System.currentTimeMillis() + (1000 * 60));
		URL url = s3Client.generatePresignedUrl(bundle.getBucket(), bundle.getKey(), expiration,
				HttpMethod.GET);
		return url.toString();
	}
	
	/**
	 * Delete the template file for the given bundle.
	 * @param bundle
	 */
	void deleteTemplate(SourceBundle bundle) {
		s3Client.deleteObject(bundle.getBucket(), bundle.getKey());
	}

}
