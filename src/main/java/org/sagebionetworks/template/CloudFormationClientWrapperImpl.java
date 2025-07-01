package org.sagebionetworks.template;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.inject.Inject;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.ResourceSignalStatus;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.SignalResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;

/**
 * Basic implementation CloudFormationClient
 *
 */
public class CloudFormationClientWrapperImpl implements CloudFormationClientWrapper {

	public static final String S3_URL_TEMPLATE = "https://s3.amazonaws.com/%s/%s";

	public static final long TIMEOUT_MS = 60 * 60 * 1000; // one hour.

	public static final int SLEEP_TIME = 10 * 1000;
	public static final String NO_UPDATES_ARE_TO_BE_PERFORMED = "No updates are to be performed";
	CloudFormationClient cloudFormationClient;
	AmazonS3 s3Client;
	Configuration configuration;
	Logger logger;
	ThreadProvider threadProvider;

	@Inject
	public CloudFormationClientWrapperImpl(CloudFormationClient cloudFormationClient, AmazonS3 s3Client,
										   Configuration configuration, LoggerFactory loggerFactory, ThreadProvider threadProvider) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.s3Client = s3Client;
		this.configuration = configuration;
		this.logger = loggerFactory.getLogger(CloudFormationClientWrapperImpl.class);
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
				UpdateStackRequest.Builder builder = UpdateStackRequest.builder()
						.stackName(requestInput.getStackName())
						.templateURL(templateUrl);
				if (requestInput.getParameters() != null) {
					builder.parameters(requestInput.getParameters());
				}
				if (requestInput.getCapabilities() != null) {
					builder.capabilities(requestInput.getCapabilities());
				}
				if (requestInput.getTags() != null) {
					builder.tags(requestInput.getTags());
				}
				UpdateStackRequest request = builder.build();
				UpdateStackResponse results = cloudFormationClient.updateStack(request);
				return results.stackId();
			}
		});	}

	@Override
	public void createStack(final CreateOrUpdateStackRequest requestInput) {
		// Temporarily upload the template to S3.
		executeWithS3Template(requestInput, new Function<String, String>() {

			@Override
			public String apply(String templateUrl) {
				CreateStackRequest.Builder builder = CreateStackRequest.builder()
						.stackName(requestInput.getStackName())
						.templateURL(templateUrl);
				if (requestInput.getParameters() != null) {
					builder.parameters(
							Arrays.stream(requestInput.getParameters())
									.filter(Objects::nonNull)
									.collect(Collectors.toList()));
				}
				if (requestInput.getCapabilities() != null) {
					builder.capabilities(
							Arrays.stream(requestInput.getCapabilities())
									.filter(Objects::nonNull)
									.collect(Collectors.toList()));
				}
				if (requestInput.getTags() != null) {
					builder.tags(requestInput.getTags().stream().filter(Objects::nonNull).collect(Collectors.toList()));;
				}
				if (requestInput.getEnableTerminationProtection() != null) {
					builder.enableTerminationProtection(requestInput.getEnableTerminationProtection());
				}
				CreateStackRequest request = builder.build();
				CreateStackResponse result = cloudFormationClient.createStack(request);
				return result.stackId();
			}
		});
	}

	/**
	 * Execute a create or update using a template that is temporarily uploaded to
	 * S3.
	 * 
	 * @param function
	 * @return
	 */
	void executeWithS3Template(final CreateOrUpdateStackRequest requestInput, Function<String, String> function) {
		// save the template file to S3
		SourceBundle bundle = saveTemplateToS3(requestInput.getStackName(), requestInput.getTemplateBody());
		try {
			// provide an pre-signed URL to the template in S3
			String templateUrl = createS3Url(bundle);
			// the function executes the create or update.
			try {
				function.apply(templateUrl);
			} catch (CloudFormationException e) {
				if (e.getMessage().contains(NO_UPDATES_ARE_TO_BE_PERFORMED)) {
					logger.info("There were no updates for stack: " + requestInput.getStackName());
				} else {
					throw new RuntimeException(e);
				}
			}
		} finally {
			// Delete the template from S3
			deleteTemplate(bundle);
		}
	}

	@Override
	public void createOrUpdateStack(CreateOrUpdateStackRequest request) {
		if (doesStackNameExist(request.getStackName())) {
			updateStack(request);
		} else {
			logger.info(String.format("createOrUpdateStack() DEBUG: %s", request));
			createStack(request);
		}
	}

	/**
	 * Describe the stack with the given name
	 */
	@Override
	public Optional<Stack> describeStack(String stackName) throws CloudFormationException {
		DescribeStacksRequest request = DescribeStacksRequest.builder().stackName(stackName).build();
		try {
			// throws an exception if it does not exist
			DescribeStacksResponse results = cloudFormationClient.describeStacks(request);
			// TODO: can this case happen?
			if (results.stacks().size() > 1) {
				throw new IllegalStateException("More than one stack found for name: " + stackName);
			}
			return Optional.of(results.stacks().get(0));
		} catch (CloudFormationException e) {
			return Optional.empty();
		}
	}

	/**
	 * Save the given template to to S3.
	 * 
	 * @param template
	 * @return
	 */
	SourceBundle saveTemplateToS3(String stackName, String template) {
		try {
			String bucket = configuration.getConfigurationBucket();
			String key = "templates/" + stackName + "-" + UUID.randomUUID() + ".json";
			byte[] bytes = template.getBytes("UTF-8");
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
	 * 
	 * @param bundle
	 * @return
	 */
	String createS3Url(SourceBundle bundle) {
		return String.format(S3_URL_TEMPLATE, bundle.getBucket(), bundle.getKey());
	}

	/**
	 * Delete the template file for the given bundle.
	 * 
	 * @param bundle
	 */
	void deleteTemplate(SourceBundle bundle) {
		s3Client.deleteObject(bundle.getBucket(), bundle.getKey());
	}

	public boolean isStartedInUpdateRollbackComplete(String stackName) {
		return describeStack(stackName)
				.map(s -> StackStatus.UPDATE_ROLLBACK_COMPLETE.equals(s.stackStatus()))
				.orElse(false);
	}

	@Override
	public Optional<Stack> waitForStackToComplete(String stackName) throws InterruptedException {
		return waitForStackToComplete(stackName, Collections.emptySet());
	}
	
	@Override
	public Optional<Stack> waitForStackToComplete(String stackName, Set<WaitConditionHandler> waitConditionHandlers) throws InterruptedException {
		boolean startedInUpdateRollbackComplete = isStartedInUpdateRollbackComplete(stackName); // Initial state
		
		Map<String, WaitConditionHandler> waitConditionHandlerMap = waitConditionHandlers.stream()
			.collect(Collectors.toMap(WaitConditionHandler::getWaitConditionId, Function.identity()));
		
		// To avoid re-processing the same wait condition multiple times we need to keep track of them
		Set<String> processedWaitConditionSet = new HashSet<>();
		
		long start = threadProvider.currentTimeMillis();
		while (true) {
			long elapse = threadProvider.currentTimeMillis() - start;
			if (elapse > TIMEOUT_MS) {
				throw new RuntimeException("Timed out waiting for stack: '" + stackName + "' status to complete");
			}
			Optional<Stack> optional = describeStack(stackName);
			if (optional.isEmpty()) {
				return Optional.empty();
			}
			Stack stack = optional.get();
			StackStatus status = stack.stackStatus();
			switch (status) {
			case CREATE_COMPLETE:
			case UPDATE_COMPLETE:
			case DELETE_COMPLETE:
				// done
				return optional;
			case CREATE_IN_PROGRESS:
			case UPDATE_IN_PROGRESS:
				handleWaitConditions(stackName, waitConditionHandlerMap, processedWaitConditionSet);
			case DELETE_IN_PROGRESS:
			case UPDATE_COMPLETE_CLEANUP_IN_PROGRESS:
				logger.info("Waiting for stack: '" + stackName + "' to complete.  Current status: " + status.name() + "...");
				threadProvider.sleep(SLEEP_TIME);
				break;
			case UPDATE_ROLLBACK_COMPLETE:
				if (startedInUpdateRollbackComplete) { // There was nothing to do, state unchanged
					return optional;
				}
			default:
				throw new RuntimeException("Stack '" + stackName + "' did not complete.  Status: " + status.name()
						+ " with reason: " + stack.stackStatusReason());
			}
		}
	}

	void handleWaitConditions(String stackName, Map<String, WaitConditionHandler> waitConditionHandlers, Set<String> processedWaitConditionSet) {
		if (waitConditionHandlers.isEmpty()) {
			return;
		}

		Set<String> waitConditionEventIds = new HashSet<>();

		List<StackEvent> waitConditionEvents = cloudFormationClient.describeStackEvents(
						DescribeStackEventsRequest.builder().stackName(stackName).build()
				)
				.stackEvents()
				.stream()
				.filter(event ->  "AWS::CloudFormation::WaitCondition".equals(event.resourceType()))
				// We only need the latest event for each wait condition
				.filter(event -> waitConditionEventIds.add(event.logicalResourceId()))
				.filter(event -> ResourceStatus.CREATE_IN_PROGRESS.equals(event.resourceStatus()))
				.collect(Collectors.toList());

		for (StackEvent waitConditionEvent : waitConditionEvents) {
			String waitConditionId = waitConditionEvent.logicalResourceId();

			if (processedWaitConditionSet.contains(waitConditionId)) {
				logger.warn("Wait condition {} already processed, skipping.", waitConditionId);
				continue;
			}

			logger.info("Processing wait condition {} (Status: {}, Reason: {})...", waitConditionId, waitConditionEvent.resourceStatus(), waitConditionEvent.resourceStatusReason());

			WaitConditionHandler waitConditionHandler = waitConditionHandlers.get(waitConditionId);

			if (waitConditionHandler == null) {

				cloudFormationClient.signalResource(SignalResourceRequest.builder()
						.stackName(stackName)
						.logicalResourceId(waitConditionId)
						.status(ResourceSignalStatus.FAILURE)
						.uniqueId("handler-not-found")
						.build()
				);

				throw new IllegalStateException("Processing wait condition " + waitConditionId + " failed: could not find an handler.");

			} else {
				logger.info("Processing wait condition {} started...", waitConditionId);

				try {
					waitConditionHandler.handle(waitConditionEvent).ifPresentOrElse(signalId -> {
						logger.info("Processing wait condition {} completed with signal {}.", waitConditionId, signalId);

						cloudFormationClient.signalResource(SignalResourceRequest.builder()
								.stackName(stackName)
								.logicalResourceId(waitConditionId)
								.status(ResourceSignalStatus.SUCCESS)
								.uniqueId(signalId)
								.build()
						);

						processedWaitConditionSet.add(waitConditionId);
					}, () -> {
						logger.info("Processing wait condition {} didn't return a signal, will process later.", waitConditionId);
					});

				} catch (Exception e) {
					logger.error("Processing wait condition {} failed exceptionally: ", waitConditionId, e);

					cloudFormationClient.signalResource(SignalResourceRequest.builder()
							.stackName(stackName)
							.logicalResourceId(waitConditionId)
							.status(ResourceSignalStatus.FAILURE)
							.uniqueId("handler-failed")
							.build()
					);

					throw new IllegalStateException("Processing wait condition " + waitConditionId + " failed.", e);
				}
			}

		}

	}

	@Override
	public String getOutput(String stackName, String outputKey) {
		String res = null;
		Stack stack = describeStack(stackName)
				.orElseThrow(() -> new IllegalStateException("Stack does not exist: " + stackName));
		List<Output> outputs = stack.outputs();
		for (Output output : outputs) {
			if (output.outputKey().equals(outputKey)) {
				res = output.outputValue();
				break;
			}
		}
		if (res == null) {
			throw new IllegalArgumentException("The output key " + outputKey + " was not found.");
		}
		return res;
	}

	@Override
	public Stream<Stack> streamOverAllStacks() {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(new PageIterator<>(new StackPageProvider()), Spliterator.ORDERED),
				false);
	}

	/**
	 * Stateful stack PageProvider for getting all Stacks using pagination.
	 *
	 */
	private class StackPageProvider implements PageIterator.PageProvider<Stack> {

		private boolean isDone = false;
		private String nextPageToken;

		public List<Stack> nextPage() {
			if(isDone) {
				return Collections.emptyList();
			}
			DescribeStacksResponse r = cloudFormationClient.describeStacks(DescribeStacksRequest.builder().nextToken(nextPageToken).build());
			nextPageToken = r.nextToken();
			if(nextPageToken == null) {
				isDone = true;
			}
			return r.stacks();
		}
	}

	@Override
	public void deleteStack(String stackName) {
		cloudFormationClient.deleteStack(DeleteStackRequest.builder().stackName(stackName).build());
	}

}
