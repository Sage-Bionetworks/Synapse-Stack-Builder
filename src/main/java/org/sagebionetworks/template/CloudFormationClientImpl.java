package org.sagebionetworks.template;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.inject.Inject;

/**
 * Basic implementation CloudFormationClient 
 *
 */
public class CloudFormationClientImpl implements CloudFormationClient {

	public static final String S3_URL_TEMPLATE = "https://s3.amazonaws.com/%s/%s";

	public static final long TIMEOUT_MS = 60*60*1000; // one hour.
	
	public static final int SLEEP_TIME = 10*1000;
	public static final String NO_UPDATES_ARE_TO_BE_PERFORMED = "No updates are to be performed";
	AmazonCloudFormation cloudFormationClient;
	AmazonS3 s3Client;
	Configuration configuration;
	Logger logger;
	ThreadProvider threadProvider;
	
	@Inject
	public CloudFormationClientImpl(AmazonCloudFormation cloudFormationClient, AmazonS3 s3Client, Configuration configuration, LoggerFactory loggerFactory, ThreadProvider threadProvider) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.s3Client = s3Client;
		this.configuration = configuration;
		this.logger = loggerFactory.getLogger(CloudFormationClientImpl.class);
		this.threadProvider = threadProvider;
	}

	@Override
	public boolean doesStackNameExist(String stackName) {
		return describeStack(stackName).isPresent();
	}

	@Override
	public void updateStack(final CreateOrUpdateStackRequest requestInput) {
		// Temporarily upload the template to S3.
		executeWithS3Template(requestInput, new Function<String, String>() {
			
			@Override
			public String apply(String templateUrl) {
				UpdateStackRequest request = new UpdateStackRequest();
				request.setStackName(requestInput.getStackName());
				request.setTemplateURL(templateUrl);
				if(requestInput.getParameters() != null) {
					request.withParameters(requestInput.getParameters());
				}
				if(requestInput.getCapabilities() != null) {
					request.withCapabilities(requestInput.getCapabilities());
				}
				if(requestInput.getTags() != null) {
					request.withTags(requestInput.getTags());
				}
				UpdateStackResult results = cloudFormationClient.updateStack(request);
				return results.getStackId();
			}
		});
	}

	@Override
	public void createStack(final CreateOrUpdateStackRequest requestInput) {
		// Temporarily upload the template to S3.
		executeWithS3Template(requestInput, new Function<String, String>() {
			
			@Override
			public String apply(String templateUrl) {
				CreateStackRequest request = new CreateStackRequest();
				request.setStackName(requestInput.getStackName());
				request.setTemplateURL(templateUrl);
				if(requestInput.getParameters() != null) {
					request.withParameters(requestInput.getParameters());
				}
				if(requestInput.getCapabilities() != null) {
					request.withCapabilities(requestInput.getCapabilities());
				}
				if(requestInput.getTags() != null) {
					request.withTags(requestInput.getTags());
				}
				if (requestInput.getEnableTerminationProtection() != null) {
					request.withEnableTerminationProtection(requestInput.getEnableTerminationProtection());
				}
				CreateStackResult result = cloudFormationClient.createStack(request);
				return result.getStackId();
			}
		});
	}
	
	/**
	 * Execute a create or update using a template that is temporarily uploaded to S3.
	 * @param function
	 * @return
	 */
	void executeWithS3Template(final CreateOrUpdateStackRequest requestInput, Function<String, String> function) {
		// save the template file to S3
		SourceBundle bundle = saveTempalteToS3(requestInput.getStackName(), requestInput.getTemplateBody());
		try {
			// provide an pre-signed URL to the template in S3
			String templateUrl = createS3Url(bundle);
			// the function executes the create or update.
			try {
				function.apply(templateUrl);
			} catch (AmazonCloudFormationException e) {
				if(e.getMessage().contains(NO_UPDATES_ARE_TO_BE_PERFORMED)) {
					logger.info("There were no updates for stack: "+requestInput.getStackName());
				}else {
					throw new RuntimeException(e);
				}
			}
		}finally {
			// Delete the template from S3
			deleteTemplate(bundle);
		}
	}

	@Override
	public void createOrUpdateStack(CreateOrUpdateStackRequest request) {
		if(doesStackNameExist(request.getStackName())) {
			updateStack(request);
		}else {
			createStack(request);
		}
	}

	/**
	 * Describe the stack with the given name
	 */
	@Override
	public Optional<Stack> describeStack(String stackName) throws AmazonCloudFormationException {
		DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
		try {
			// throws an exception if it does not exist
			DescribeStacksResult results = cloudFormationClient.describeStacks(request);
			if(results.getStacks().size() > 1) {
				throw new IllegalStateException("More than one stack found for name: "+stackName);
			}
			return Optional.of(results.getStacks().get(0));
		}catch(AmazonCloudFormationException e) {
			return Optional.empty();
		}
	}
	
	/**
	 * Save the given template to to S3.
	 * @param tempalte
	 * @return
	 */
	SourceBundle saveTempalteToS3(String stackName, String tempalte) {
		try {
			String bucket = configuration.getConfigurationBucket();
			String key = "templates/" + stackName + "-" + UUID.randomUUID() + ".json";
			byte[] bytes = tempalte.getBytes("UTF-8");
			ByteArrayInputStream input = new ByteArrayInputStream(bytes);
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(bytes.length);
			s3Client.putObject(new PutObjectRequest(bucket, key, input, metadata));
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
	String createS3Url(SourceBundle bundle) {
		return String.format(S3_URL_TEMPLATE, bundle.getBucket(), bundle.getKey());
	}
	
	/**
	 * Delete the template file for the given bundle.
	 * @param bundle
	 */
	void deleteTemplate(SourceBundle bundle) {
		s3Client.deleteObject(bundle.getBucket(), bundle.getKey());
	}

	public boolean isStartedInUpdateRollbackComplete(String stackName) {
		return describeStack(stackName)
				.map(s -> StackStatus.UPDATE_ROLLBACK_COMPLETE.equals(StackStatus.fromValue(s.getStackStatus())))
				.orElse(false);
	}

	@Override
	public Optional<Stack> waitForStackToComplete(String stackName) throws InterruptedException {
		boolean startedInUpdateRollbackComplete = isStartedInUpdateRollbackComplete(stackName); // Initial state
		long start = threadProvider.currentTimeMillis();
		while(true) {
			long elapse = threadProvider.currentTimeMillis()-start;
			if(elapse > TIMEOUT_MS) {
				throw new RuntimeException("Timed out waiting for stack: '"+stackName+"' status to complete");
			}
			Optional<Stack> optional = describeStack(stackName);
			if(optional.isEmpty()) {
				return Optional.empty();
			}
			Stack stack = optional.get();
			StackStatus status = StackStatus.fromValue(stack.getStackStatus());
			switch(status) {
			case CREATE_COMPLETE:
			case UPDATE_COMPLETE:
			case DELETE_COMPLETE:
				// done
				return optional;
			case CREATE_IN_PROGRESS:
			case UPDATE_IN_PROGRESS:
			case DELETE_IN_PROGRESS:
			case UPDATE_COMPLETE_CLEANUP_IN_PROGRESS:
				logger.info("Waiting for stack: '"+stackName+"' to complete.  Current status: "+status.name()+"...");
				threadProvider.sleep(SLEEP_TIME);
				break;
			case UPDATE_ROLLBACK_COMPLETE:
				if (startedInUpdateRollbackComplete) { // There was nothing to do, state unchanged
					return optional;
				}
			default:
			throw new RuntimeException("Stack '"+stackName+"' did not complete.  Status: "+status.name()+" with reason: "+stack.getStackStatusReason());
			}
		}
	}

	@Override
	public String getOutput(String stackName, String outputKey) {
		String res = null;
		Stack stack = describeStack(stackName).orElseThrow(()->new IllegalStateException("Stack does not exist: "+stackName));
		List<Output> outputs = stack.getOutputs();
		for (Output output: outputs) {
			if (output.getOutputKey().equals(outputKey)) {
				res = output.getOutputValue();
				break;
			}
		}
		if (res == null) {
			throw new IllegalArgumentException("The output key " + outputKey + " was not found.");
		}
		return res;
	}

}
