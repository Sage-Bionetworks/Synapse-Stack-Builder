package org.sagebionetworks.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.ResourceSignalStatus;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.SignalResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;

@ExtendWith(MockitoExtension.class)
public class CloudFormationClientWrapperImplTest {

	public static final String SES_SYNAPSE_ORG_COMPLAINT_TOPIC_KEY = "SesSynapseOrgComplaintTopic";
	public static final String SES_SYNAPSE_ORG_COMPLAINT_TOPIC_VALUE = "theSesComplaintTopicArn";
	public static final String SES_SYNAPSE_ORG_BOUNCE_TOPIC_KEY = "SesSynapseOrgBounceTopic";
	public static final String SES_SYNAPSE_ORG_BOUNCE_TOPIC_VALUE = "theSesBounceTopicArn";

	@Mock
	CloudFormationClient mockCloudFormationClient;
	@Mock
	AmazonS3 mockS3Client;
	@Mock
	Configuration mockConfig;
	@Mock
	Function<String, String> mockFunction;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;
	@Mock
	ThreadProvider mockThreadProvider;
	@Mock
	WaitConditionHandler mockWaitConditionHandler;

	@Captor
	ArgumentCaptor<DescribeStacksRequest> describeStackRequestCapture;
	@Captor
	ArgumentCaptor<CreateStackRequest> createStackRequestCapture;
	@Captor
	ArgumentCaptor<UpdateStackRequest> updateStackRequestCapture;

	CloudFormationClientWrapperImpl client;

	String stackName;
	String templateBody;
	Parameter parameter;
	Parameter[] parameters;

	String stackId;
	DescribeStacksResponse initDescribeResult, describeResult;
	// TODO: renove these 2
	UpdateStackResponse updateResult;
	CreateStackResponse createResult;
	
	Stack.Builder stackBuilder;
	DescribeStacksResponse.Builder describeStacksResponseBuilder;
	CreateStackResponse.Builder createStackResponseBuilder;
	UpdateStackResponse.Builder updateStackResponseBuilder;
	CreateOrUpdateStackRequest inputReqequest;

	// TODO: Remove these 2
	Stack initStack, stack;

	String bucket;

	Capability[] capabilities;

	@BeforeEach
	public void before() throws MalformedURLException {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

		client = new CloudFormationClientWrapperImpl(mockCloudFormationClient, mockS3Client, mockConfig, mockLoggerFactory, mockThreadProvider);

		stackId = "theStackId";
		Collection<Output> outputs = new ArrayList<>();
		Output output1 = Output.builder().outputKey(SES_SYNAPSE_ORG_COMPLAINT_TOPIC_KEY).outputValue(SES_SYNAPSE_ORG_COMPLAINT_TOPIC_VALUE).build();
		Output output2 = Output.builder().outputKey(SES_SYNAPSE_ORG_BOUNCE_TOPIC_KEY).outputValue(SES_SYNAPSE_ORG_BOUNCE_TOPIC_VALUE).build();
		outputs.add(output1);
		outputs.add(output2);

		initStack = Stack.builder().stackId(stackId).outputs(outputs).build();
		stack = Stack.builder().stackId(stackId).outputs(outputs).build();

		initDescribeResult = DescribeStacksResponse.builder().stacks(initStack).build();
		describeResult = DescribeStacksResponse.builder().stacks(stack).build();

		updateResult = UpdateStackResponse.builder().stackId(stackId).build();

		createResult = CreateStackResponse.builder().stackId(stackId).build();

		capabilities = new Capability[] {Capability.CAPABILITY_IAM, Capability.CAPABILITY_NAMED_IAM};

		stackName = "someStackName";
		templateBody = "body";
		parameter = Parameter.builder().parameterKey("paramKey").parameterValue("paramValue").build();
		parameters = new Parameter[] { parameter };
		inputReqequest = new CreateOrUpdateStackRequest()
				.withStackName(stackName)
				.withTemplateBody(templateBody)
				.withParameters(parameters)
				.withCapabilities(capabilities);

		bucket = "theBucket";

		stackBuilder = Stack.builder();
		describeStacksResponseBuilder = DescribeStacksResponse.builder();
		createStackResponseBuilder = CreateStackResponse.builder();
		updateStackResponseBuilder = UpdateStackResponse.builder();

	}

	@Test
	public void testDescribeStack() {
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);

		// call under test
		Optional<Stack> result = client.describeStack(stackName);

		assertTrue(result.isPresent());
		Assertions.assertEquals(stackId, result.get().stackId());
		verify(mockCloudFormationClient).describeStacks(describeStackRequestCapture.capture());
		Assertions.assertEquals(stackName, describeStackRequestCapture.getValue().stackName());
	}

	@Test
	public void testDescribeStackWithDoesNotExist() {
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.builder().message("does not exist").build());

		// call under test
		assertEquals(Optional.empty(), client.describeStack(stackName));
		verify(mockCloudFormationClient).describeStacks(DescribeStacksRequest.builder().stackName(stackName).build());
	}

	@Test
	public void testDoesStackNameExistTrue() {
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		// call under test
		boolean exists = client.doesStackNameExist(stackName);
		assertTrue(exists);
	}

	@Test
	public void testDoesStackNameExistFalse() {
		AwsServiceException exception = CloudFormationException.builder().message("Not found").build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(exception);

		// call under test
		boolean exists = client.doesStackNameExist(stackName);
		assertFalse(exists);
	}

	@Test
	public void testIsStartedInUpdateRollbackCompleteFalse() {
		Stack stack = stackBuilder.stackId(stackId).stackName(stackName).stackStatus(StackStatus.CREATE_COMPLETE).build();
		DescribeStacksResponse describeResponse = describeStacksResponseBuilder.stacks(stack).build();

		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeResponse);

		// call under test
		boolean isStartedInUpdateRollbackComplete = client.isStartedInUpdateRollbackComplete(stackName);

		assertFalse(isStartedInUpdateRollbackComplete);
	}

	@Test
	public void testIsStartedInUpdateRollbackCompleteTrue() {
		Stack stack = stackBuilder.stackId(stackId).stackName(stackName).stackStatus(StackStatus.UPDATE_ROLLBACK_COMPLETE).build();
		DescribeStacksResponse describeResponse = describeStacksResponseBuilder.stacks(stack).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeResponse);
		// call under test
		boolean isStartedInUpdateRollbackComplete = client.isStartedInUpdateRollbackComplete(stackName);
		assertTrue(isStartedInUpdateRollbackComplete);
	}

	@Test
	public void testIsStartedInUpdateRollbackCompleteNoStack() {
		AwsServiceException exception = CloudFormationException.builder().message("Not found").build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(exception);
		// call under test
		boolean isStartedInUpdateRollbackComplete = client.isStartedInUpdateRollbackComplete(stackName);
		assertFalse(isStartedInUpdateRollbackComplete);
	}


	@Test
	public void testCreateStack() {
		when(mockCloudFormationClient.createStack(any(CreateStackRequest.class))).thenReturn(createResult);
		// call under test
		client.createStack(inputReqequest);
		verify(mockCloudFormationClient).createStack(createStackRequestCapture.capture());
		CreateStackRequest captureRequest = createStackRequestCapture.getValue();
		Assertions.assertEquals(stackName, captureRequest.stackName());
		Assertions.assertNotNull(captureRequest.templateURL());
		Assertions.assertNotNull(captureRequest.parameters());
		Assertions.assertEquals(1, captureRequest.parameters().size());
		Assertions.assertEquals(parameter, captureRequest.parameters().get(0));
		List<Capability> caps = captureRequest.capabilities();
		Assertions.assertNotNull(caps);
		Assertions.assertEquals(capabilities.length, caps.size());
		Assertions.assertEquals(capabilities[0], caps.get(0));
		Assertions.assertEquals(capabilities[1], caps.get(1));
	}

	@Test
	public void testUpdateStack() {
		when(mockCloudFormationClient.updateStack(any(UpdateStackRequest.class))).thenReturn(updateResult);
		// call under test
		client.updateStack(inputReqequest);
		verify(mockCloudFormationClient).updateStack(updateStackRequestCapture.capture());
		UpdateStackRequest request = updateStackRequestCapture.getValue();
		Assertions.assertEquals(stackName, request.stackName());
		Assertions.assertNotNull(request.templateURL());
		Assertions.assertNotNull(request.parameters());
		Assertions.assertEquals(1, request.parameters().size());
		Assertions.assertEquals(parameter, request.parameters().get(0));
		List<Capability> caps = request.capabilities();
		Assertions.assertNotNull(caps);
		Assertions.assertEquals(capabilities.length, caps.size());
		Assertions.assertEquals(capabilities[0], caps.get(0));
		Assertions.assertEquals(capabilities[1], caps.get(1));
	}

	@Test
	public void testCreateOrUpdateAsUpdate() {
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeResult);
		when(mockCloudFormationClient.updateStack(any(UpdateStackRequest.class))).thenReturn(updateResult);
		// call under test
		client.createOrUpdateStack(inputReqequest);
		verify(mockCloudFormationClient).updateStack(any(UpdateStackRequest.class));
		verify(mockCloudFormationClient, never()).createStack(any(CreateStackRequest.class));
	}

	@Test
	public void testCreateOrUpdateAsCreate() {
		CreateStackResponse createStackResponse = createStackResponseBuilder.stackId(stackId).build();
		when(mockCloudFormationClient.createStack(any(CreateStackRequest.class))).thenReturn(createStackResponse);

		// setup exception to trigger does not exist
		AwsServiceException exception = CloudFormationException.builder().message("Not found").build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(exception);

		inputReqequest = new CreateOrUpdateStackRequest()
				.withStackName("stackNameToFind1")
				.withCapabilities(capabilities)
				.withParameters(parameters)
				.withTemplateBody(templateBody);

		// call under test
		client.createOrUpdateStack(inputReqequest);

		verify(mockCloudFormationClient, never()).updateStack(any(UpdateStackRequest.class));
		verify(mockCloudFormationClient).createStack(any(CreateStackRequest.class));
	}

	@Test
	public void testSaveTemplateToS3() {
		when(mockConfig.getConfigurationBucket()).thenReturn(bucket);
		// call under test
		SourceBundle bundle = client.saveTemplateToS3(stackName, templateBody);
		Assertions.assertNotNull(bundle);
		Assertions.assertEquals(bucket, bundle.getBucket());
		Assertions.assertNotNull(bundle.getKey());
		assertTrue(bundle.getKey().startsWith("templates/someStackName"));
		assertTrue(bundle.getKey().endsWith(".json"));
		ArgumentCaptor<PutObjectRequest> requestCapture = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(mockS3Client).putObject(requestCapture.capture());
		PutObjectRequest request = requestCapture.getValue();
		Assertions.assertNotNull(request);
		Assertions.assertEquals(bucket, request.getBucketName());
		Assertions.assertEquals(bundle.getKey(), request.getKey());
		Assertions.assertNotNull(request.getMetadata());
		Assertions.assertEquals(4L, request.getMetadata().getContentLength());
	}

	@Test
	public void testDeleteTemplate() {
		String key = "someKey";
		SourceBundle bundle = new SourceBundle(bucket, key);
		// call under test
		client.deleteTemplate(bundle);
		verify(mockS3Client).deleteObject(bucket, key);
	}

	@Test
	public void testExecuteWithS3Template() {
		when(mockConfig.getConfigurationBucket()).thenReturn(bucket);
		when(mockFunction.apply(anyString())).thenReturn(stackId);
		// call under test
		client.executeWithS3Template(inputReqequest, mockFunction);
		verify(mockS3Client).putObject(any(PutObjectRequest.class));
		verify(mockFunction).apply(anyString());
		verify(mockS3Client).deleteObject(anyString(), anyString());
	}


	@Test
	public void testExecuteWithS3TemplateNoUpdates() {
		when(mockConfig.getConfigurationBucket()).thenReturn(bucket);
		AwsServiceException exception = CloudFormationException.builder().message(CloudFormationClientWrapperImpl.NO_UPDATES_ARE_TO_BE_PERFORMED).build();

		when(mockFunction.apply(any(String.class))).thenThrow(exception);

		inputReqequest = new CreateOrUpdateStackRequest()
				.withStackName("stackNameToFind1")
				.withCapabilities(capabilities)
				.withParameters(parameters)
				.withTemplateBody(templateBody);

		// call under test
		client.executeWithS3Template(inputReqequest, mockFunction);

		verify(mockS3Client).putObject(any(PutObjectRequest.class));
		verify(mockFunction).apply(any(String.class));
		verify(mockS3Client).deleteObject(any(String.class), any(String.class));
		verify(mockLogger).info(any(String.class));
	}

	@Test
	public void testExecuteWithS3TemplateWithError() {
		when(mockConfig.getConfigurationBucket()).thenReturn(bucket);
		AwsServiceException exception = CloudFormationException.builder().message("some other error").build();

		when(mockFunction.apply(any(String.class))).thenThrow(exception);

		inputReqequest = new CreateOrUpdateStackRequest()
				.withStackName("stackNameToFind1")
				.withCapabilities(capabilities)
				.withParameters(parameters)
				.withTemplateBody(templateBody);

		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.executeWithS3Template(inputReqequest, mockFunction);
		});
	}

	@Test
	public void testWaitForStackToCompleteCreateComplete() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.CREATE_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1, describeStacksResponse2);
		// call under test
		Optional<Stack> result = client.waitForStackToComplete(stackName);
		assertTrue(result.isPresent());
		verify(mockCloudFormationClient, times(2)).describeStacks(any(DescribeStacksRequest.class));
	}

	@Test
	public void testWaitForStackToCompleteUpdateComplete() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.UPDATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.UPDATE_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1, describeStacksResponse2);

		// call under test
		Optional<Stack> result = client.waitForStackToComplete(stackName);
		assertTrue(result.isPresent());
		verify(mockCloudFormationClient, times(2)).describeStacks(any(DescribeStacksRequest.class));
	}


	@Test
	public void testWaitForStackToCompleteTimeout() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1, describeStacksResponse2);

		when(mockThreadProvider.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,Long.MAX_VALUE);

		// call under test
		try {
			client.waitForStackToComplete(stackName);
			Assertions.fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("Timed out"));

		}
		verify(mockCloudFormationClient, times(4)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockLogger, times(3)).info(any(String.class));
		verify(mockThreadProvider, times(3)).sleep(any(Long.class));
	}

	@Test
	public void testWaitForStackToCompleteTimeoutUpdate() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.UPDATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.UPDATE_IN_PROGRESS).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1, describeStacksResponse2);

		when(mockThreadProvider.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,Long.MAX_VALUE);

		// call under test
		try {
			client.waitForStackToComplete(stackName);
			Assertions.fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("Timed out"));

		}
		verify(mockCloudFormationClient, times(4)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockLogger, times(3)).info(any(String.class));
		verify(mockThreadProvider, times(3)).sleep(any(Long.class));
	}

	@Test
	public void testWaitForStackToCompleteTimeoutUpdateCleanup() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.UPDATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1, describeStacksResponse2);

		when(mockThreadProvider.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,Long.MAX_VALUE);

		// call under test
		try {
			client.waitForStackToComplete(stackName);
			Assertions.fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("Timed out"));

		}
		verify(mockCloudFormationClient, times(4)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockLogger, times(3)).info(any(String.class));
		verify(mockThreadProvider, times(3)).sleep(any(Long.class));
	}

	@Test
	public void testWaitForStackToCompleteCreateFailed() {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_FAILED).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1);

		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}

	@Test
	public void testWaitForStackToCompleteRollbackComplete() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.ROLLBACK_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1);

		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}

	@Test
	public void testWaitForStackToCompleteRollbackFailed() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.ROLLBACK_FAILED).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1);

		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}

	@Test
	public void testWaitForStackToCompleteRollbackProgress() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.ROLLBACK_IN_PROGRESS).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1);

		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}

	@Test
	public void testWaitForStackToCompleteUpdateFailed() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.UPDATE_ROLLBACK_FAILED).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1);

		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}

	@Test
	public void testWaitForStackToCompleteUpdateCompleteToUpdateRollBackComplete() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.UPDATE_COMPLETE).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.UPDATE_ROLLBACK_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1, describeStacksResponse2);

		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}

	@Test
	public void testWaitForStackToCompleteUpdateRollbackCompleteToUpdateRollBackComplete() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.UPDATE_ROLLBACK_COMPLETE).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.UPDATE_ROLLBACK_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse1, describeStacksResponse2);

		// call under test
		Optional<Stack> result = client.waitForStackToComplete(stackName);

		assertTrue(result.isPresent());
		verify(mockCloudFormationClient, times(2)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockThreadProvider, times(2)).currentTimeMillis();
		verify(mockThreadProvider, never()).sleep(any(Long.class));
		Stack resultStack = result.get();
		Assertions.assertNotNull(resultStack.stackStatus());
		Assertions.assertEquals(StackStatus.UPDATE_ROLLBACK_COMPLETE, resultStack.stackStatus());
	}

	@Test
	public void testWaitForStackToCompleteWithWaitConditionHandlers() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack3 = stackBuilder.stackStatus(StackStatus.CREATE_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		DescribeStacksResponse describeStacksResponse3 = describeStacksResponseBuilder.stacks(stack3).build();

		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(
				describeStacksResponse1,
				describeStacksResponse2,
				describeStacksResponse3
		);

		String waitConditionId = "waitConditionId";
		StackEvent waitConditionEvent = StackEvent.builder()
				.resourceType("AWS::CloudFormation::WaitCondition")
				.logicalResourceId(waitConditionId)
				.resourceStatus(ResourceStatus.CREATE_IN_PROGRESS)
				.build();

		when(mockWaitConditionHandler.getWaitConditionId()).thenReturn(waitConditionId);
		when(mockWaitConditionHandler.handle(waitConditionEvent)).thenReturn(Optional.of("done"));
		when(mockCloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(stackName).build()))
				.thenReturn(DescribeStackEventsResponse.builder().stackEvents(waitConditionEvent).build());

		// call under test
		Stack resultStack = client.waitForStackToComplete(stackName, Set.of(mockWaitConditionHandler)).get();

		verify(mockCloudFormationClient).signalResource(SignalResourceRequest.builder()
				.logicalResourceId(waitConditionId)
				.stackName(stackName)
				.status(ResourceSignalStatus.SUCCESS)
				.uniqueId("done")
				.build()
		);
	}

	@Test
	public void testWaitForStackToCompleteWithWaitConditionHandlersAndMultipleEvents() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack3 = stackBuilder.stackStatus(StackStatus.CREATE_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		DescribeStacksResponse describeStacksResponse3 = describeStacksResponseBuilder.stacks(stack3).build();

		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(
				describeStacksResponse1,
				describeStacksResponse2,
				describeStacksResponse3
		);

		String waitConditionId = "waitConditionId";

		StackEvent waitConditionEvent = StackEvent.builder()
				.resourceType("AWS::CloudFormation::WaitCondition")
				.logicalResourceId(waitConditionId)
				.resourceStatus(ResourceStatus.CREATE_IN_PROGRESS)
				.eventId("last")
				.build();

		when(mockWaitConditionHandler.getWaitConditionId()).thenReturn(waitConditionId);
		when(mockWaitConditionHandler.handle(waitConditionEvent)).thenReturn(Optional.of("done"));
		when(mockCloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(stackName).build()))
			.thenReturn(
				DescribeStackEventsResponse.builder().stackEvents(
					waitConditionEvent,
					StackEvent.builder()
						.resourceType("AWS::CloudFormation::WaitCondition")
						.logicalResourceId("anotherWaitConditionId")
						.resourceStatus(ResourceStatus.CREATE_COMPLETE)
						.build(),
				StackEvent.builder()
						.resourceType("anotherType")
						.logicalResourceId("anotherResourceId")
						.resourceStatus(ResourceStatus.CREATE_IN_PROGRESS)
						.build(),
				// Another event for the same condition id, should be discarded
					StackEvent.builder()
						.resourceType("AWS::CloudFormation::WaitCondition")
						.logicalResourceId(waitConditionId)
						.resourceStatus(ResourceStatus.CREATE_IN_PROGRESS)
						.eventId("previous")
						.build()
				).build()
			);

		// call under test
		Optional<Stack> resultStack = client.waitForStackToComplete(stackName, Set.of(mockWaitConditionHandler));

		assertTrue(resultStack.isPresent());

		verify(mockCloudFormationClient).signalResource(SignalResourceRequest.builder()
				.logicalResourceId(waitConditionId)
				.stackName(stackName)
				.status(ResourceSignalStatus.SUCCESS)
				.uniqueId("done")
				.build()
		);	}

	@Test
	public void testWaitForStackToCompleteWithWaitConditionHandlersAndAlreadyProcessed() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack3 = stackBuilder.stackStatus(StackStatus.CREATE_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		DescribeStacksResponse describeStacksResponse3 = describeStacksResponseBuilder.stacks(stack3).build();

		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(
				describeStacksResponse1,
				describeStacksResponse2,
				describeStacksResponse2,
				describeStacksResponse3
		);

		String waitConditionId = "waitConditionId";

		StackEvent waitConditionEvent = StackEvent.builder()
				.resourceType("AWS::CloudFormation::WaitCondition")
				.logicalResourceId(waitConditionId)
				.resourceStatus(ResourceStatus.CREATE_IN_PROGRESS)
				.build();

		when(mockWaitConditionHandler.getWaitConditionId()).thenReturn(waitConditionId);
		when(mockWaitConditionHandler.handle(waitConditionEvent)).thenReturn(Optional.of("done"));
		when(mockCloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(stackName).build()))
				.thenReturn(DescribeStackEventsResponse.builder().stackEvents(waitConditionEvent).build());

		// call under test
		Optional<Stack> resultStack = client.waitForStackToComplete(stackName, Set.of(mockWaitConditionHandler));

		assertTrue(resultStack.isPresent());

		verify(mockCloudFormationClient, times(2)).describeStackEvents(any(DescribeStackEventsRequest.class));

		// Should be invoked only once
		verify(mockWaitConditionHandler).handle(waitConditionEvent);
		verify(mockCloudFormationClient).signalResource(SignalResourceRequest.builder()
				.logicalResourceId(waitConditionId)
				.stackName(stackName)
				.status(ResourceSignalStatus.SUCCESS)
				.uniqueId("done")
				.build()
		);
	}

	@Test
	public void testWaitForStackToCompleteWithWaitConditionHandlersAndNoMatchingHandler() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack3 = stackBuilder.stackStatus(StackStatus.CREATE_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		DescribeStacksResponse describeStacksResponse3 = describeStacksResponseBuilder.stacks(stack3).build();

		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(
				describeStacksResponse1,
				describeStacksResponse2,
				describeStacksResponse3
		);

		String waitConditionId = "waitConditionId";

		StackEvent waitConditionEvent = StackEvent.builder()
				.resourceType("AWS::CloudFormation::WaitCondition")
				.logicalResourceId(waitConditionId)
				.resourceStatus(ResourceStatus.CREATE_IN_PROGRESS)
				.eventId("last")
				.build();

		when(mockWaitConditionHandler.getWaitConditionId()).thenReturn(waitConditionId + "-mistmatching");
		when(mockCloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(stackName).build()))
				.thenReturn(DescribeStackEventsResponse.builder().stackEvents(waitConditionEvent).build());

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {
			// call under test
			Optional<Stack> resultStack = client.waitForStackToComplete(stackName, Set.of(mockWaitConditionHandler));
		});

		assertEquals("Processing wait condition waitConditionId failed: could not find an handler.", result.getMessage());

		verify(mockCloudFormationClient).signalResource(SignalResourceRequest.builder()
				.logicalResourceId(waitConditionId)
				.stackName(stackName)
				.status(ResourceSignalStatus.FAILURE)
				.uniqueId("handler-not-found")
				.build()
		);

		verifyNoMoreInteractions(mockWaitConditionHandler);	}

	@Test
	public void testWaitForStackToCompleteWithWaitConditionHandlersAndException() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack3 = stackBuilder.stackStatus(StackStatus.CREATE_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		DescribeStacksResponse describeStacksResponse3 = describeStacksResponseBuilder.stacks(stack3).build();

		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(
				describeStacksResponse1,
				describeStacksResponse2,
				describeStacksResponse3
		);

		String waitConditionId = "waitConditionId";

		StackEvent waitConditionEvent = StackEvent.builder()
				.resourceType("AWS::CloudFormation::WaitCondition")
				.logicalResourceId(waitConditionId)
				.resourceStatus(ResourceStatus.CREATE_IN_PROGRESS)
				.eventId("last")
				.build();

		when(mockWaitConditionHandler.getWaitConditionId()).thenReturn(waitConditionId);

		RuntimeException cause = new RuntimeException("processing error");

		when(mockWaitConditionHandler.handle(waitConditionEvent)).thenThrow(cause);
		when(mockCloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(stackName).build()))
				.thenReturn(DescribeStackEventsResponse.builder().stackEvents(waitConditionEvent).build());

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {
			// call under test
			Optional<Stack> resultStack = client.waitForStackToComplete(stackName, Set.of(mockWaitConditionHandler));
		});

		assertEquals("Processing wait condition waitConditionId failed.", result.getMessage());
		assertEquals(cause, result.getCause());

		verify(mockCloudFormationClient).signalResource(SignalResourceRequest.builder()
				.logicalResourceId(waitConditionId)
				.stackName(stackName)
				.status(ResourceSignalStatus.FAILURE)
				.uniqueId("handler-failed")
				.build()
		);

		verifyNoMoreInteractions(mockWaitConditionHandler);	}

	@Test
	public void testWaitForStackToCompleteWithWaitConditionHandlersAndNoSignal() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack3 = stackBuilder.stackStatus(StackStatus.CREATE_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		DescribeStacksResponse describeStacksResponse3 = describeStacksResponseBuilder.stacks(stack3).build();

		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(
				describeStacksResponse1,
				describeStacksResponse2,
				describeStacksResponse3
		);

		String waitConditionId = "waitConditionId";

		StackEvent waitConditionEvent = StackEvent.builder()
				.resourceType("AWS::CloudFormation::WaitCondition")
				.logicalResourceId(waitConditionId)
				.resourceStatus(ResourceStatus.CREATE_IN_PROGRESS)
				.eventId("last")
				.build();

		when(mockWaitConditionHandler.getWaitConditionId()).thenReturn(waitConditionId);
		when(mockWaitConditionHandler.handle(waitConditionEvent)).thenReturn(Optional.empty());
		when(mockCloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(stackName).build()))
				.thenReturn(DescribeStackEventsResponse.builder().stackEvents(waitConditionEvent).build());

		// call under test
		Optional<Stack> resultStack = client.waitForStackToComplete(stackName, Set.of(mockWaitConditionHandler));

		assertTrue(resultStack.isPresent());

		verifyNoMoreInteractions(mockCloudFormationClient, mockWaitConditionHandler);	}

	@Test
	public void testWaitForStackToCompleteWithEmptyWaitConditionHandlers() throws InterruptedException {
		Stack stack1 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack2 = stackBuilder.stackStatus(StackStatus.CREATE_IN_PROGRESS).build();
		Stack stack3 = stackBuilder.stackStatus(StackStatus.CREATE_COMPLETE).build();
		DescribeStacksResponse describeStacksResponse1 = describeStacksResponseBuilder.stacks(stack1).build();
		DescribeStacksResponse describeStacksResponse2 = describeStacksResponseBuilder.stacks(stack2).build();
		DescribeStacksResponse describeStacksResponse3 = describeStacksResponseBuilder.stacks(stack3).build();

		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(
			describeStacksResponse1,
			describeStacksResponse2,
			describeStacksResponse3
		);

		// call under test
		Optional<Stack> resultStack = client.waitForStackToComplete(stackName, Collections.emptySet());

		verifyNoMoreInteractions(mockCloudFormationClient, mockWaitConditionHandler);
	}

	@Test
	public void testGetOutput() {
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		// call under test
		String output = client.getOutput(stackName, SES_SYNAPSE_ORG_COMPLAINT_TOPIC_KEY);

		Assertions.assertEquals(SES_SYNAPSE_ORG_COMPLAINT_TOPIC_VALUE, output);
	}

	@Test
	public void testGetOutputInvalid() {
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		IllegalArgumentException expectedEx = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			client.getOutput(stackName, "invalidKey");
		});
	}

	@Test
	public void testStreamOverAllStacks() {

		DescribeStacksResponse one = DescribeStacksResponse.builder()
				.stacks(
					Stack.builder().stackName("a").build(),
					Stack.builder().stackName("b").build())
				.nextToken("next a")
				.build();
		DescribeStacksResponse two = DescribeStacksResponse.builder()
				.stacks(
					Stack.builder().stackName("c").build(),
					Stack.builder().stackName("d").build())
				.nextToken(null)
				.build();

		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(one, two);

		// call under test
		List<String> stackNames = client.streamOverAllStacks().map(Stack::stackName).collect(Collectors.toList());
		assertEquals(List.of("a", "b", "c", "d"), stackNames);

		verify(mockCloudFormationClient).describeStacks(DescribeStacksRequest.builder().nextToken(null).build());
		verify(mockCloudFormationClient).describeStacks(DescribeStacksRequest.builder().nextToken("next a").build());
		verifyNoMoreInteractions(mockCloudFormationClient);
	}

	@Test
	public void testDeleteStack() {
		// call under test
		client.deleteStack("delete-me");
		verify(mockCloudFormationClient).deleteStack(DeleteStackRequest.builder().stackName("delete-me").build());
	}

}
