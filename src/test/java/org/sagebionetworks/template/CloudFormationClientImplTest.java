package org.sagebionetworks.template;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
public class CloudFormationClientImplTest {

	public static final String SES_SYNAPSE_ORG_COMPLAINT_TOPIC_KEY = "SesSynapseOrgComplaintTopic";
	public static final String SES_SYNAPSE_ORG_COMPLAINT_TOPIC_VALUE = "theSesComplaintTopicArn";
	public static final String SES_SYNAPSE_ORG_BOUNCE_TOPIC_KEY = "SesSynapseOrgBounceTopic";
	public static final String SES_SYNAPSE_ORG_BOUNCE_TOPIC_VALUE = "theSesBounceTopicArn";

	@Mock
	AmazonCloudFormation mockCloudFormationClient;
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

	@Captor
	ArgumentCaptor<DescribeStacksRequest> describeStackRequestCapture;
	@Captor
	ArgumentCaptor<CreateStackRequest> createStackRequestCapture;
	@Captor
	ArgumentCaptor<UpdateStackRequest> updateStackRequestCapture;

	CloudFormationClientImpl client;

	String stackName;
	String tempalteBody;
	Parameter parameter;
	Parameter[] parameters;

	String stackId;
	DescribeStacksResult initDescribeResult, describeResult;
	UpdateStackResult updateResult;
	CreateStackResult createResult;
	CreateOrUpdateStackRequest inputReqequest;

	Stack initStack, stack;
	
	String bucket;
	
	String[] capabilities;

	@BeforeEach
	public void before() throws MalformedURLException {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		
		client = new CloudFormationClientImpl(mockCloudFormationClient, mockS3Client, mockConfig, mockLoggerFactory, mockThreadProvider);

		stackId = "theStackId";
		Collection<Output> outputs = new ArrayList<>();
		Output output1 = new Output().withOutputKey(SES_SYNAPSE_ORG_COMPLAINT_TOPIC_KEY).withOutputValue(SES_SYNAPSE_ORG_COMPLAINT_TOPIC_VALUE);
		Output output2 = new Output().withOutputKey(SES_SYNAPSE_ORG_BOUNCE_TOPIC_KEY).withOutputValue(SES_SYNAPSE_ORG_BOUNCE_TOPIC_VALUE);
		outputs.add(output1);
		outputs.add(output2);

		initStack = new Stack().withStackId(stackId).withOutputs(outputs);
		stack = new Stack().withStackId(stackId).withOutputs(outputs);

		initDescribeResult = new DescribeStacksResult().withStacks(initStack);
		describeResult = new DescribeStacksResult().withStacks(stack);

		updateResult = new UpdateStackResult().withStackId(stackId);

		createResult = new CreateStackResult().withStackId(stackId);
		
		capabilities = new String[] {"capOne", "capTwo"};
		
		stackName = "someStackName";
		tempalteBody = "body";
		parameter = new Parameter().withParameterKey("paramKey").withParameterValue("paramValue");
		parameters = new Parameter[] { parameter };
		inputReqequest = new CreateOrUpdateStackRequest()
				.withStackName(stackName)
				.withTemplateBody(tempalteBody)
				.withParameters(parameters)
				.withCapabilities(capabilities);
		
		bucket = "theBucket";

	}

	@Test
	public void testDescribeStack() {
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		// call under test
		Stack result = client.describeStack(stackName).get();
		Assertions.assertNotNull(result);
		Assertions.assertEquals(stackId, result.getStackId());
		verify(mockCloudFormationClient).describeStacks(describeStackRequestCapture.capture());
		Assertions.assertEquals(stackName, describeStackRequestCapture.getValue().getStackName());
	}
	
	@Test
	public void testDescribeStackWithDoesNotExist() {
		when(mockCloudFormationClient.describeStacks(any())).thenThrow(new AmazonCloudFormationException("does not exist"));
	
		// call under test
		assertEquals(Optional.empty(), client.describeStack(stackName));
		verify(mockCloudFormationClient).describeStacks(new DescribeStacksRequest().withStackName(stackName));
	}

	@Test
	public void testDoesStackNameExistTrue() {
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		// call under test
		boolean exists = client.doesStackNameExist(stackName);
		Assertions.assertTrue(exists);
	}

	@Test
	public void testDoesStackNameExistFalse() {
		// setup exception to trigger does not exist
		AmazonCloudFormationException exception = new AmazonCloudFormationException("Does not exist");
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(exception);
		// call under test
		boolean exists = client.doesStackNameExist(stackName);
		Assertions.assertFalse(exists);
	}

	@Test
	public void testIsStartedInUpdateRollbackCompleteFalse() {
		stack.setStackStatus(StackStatus.CREATE_COMPLETE);
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeResult);
		// call under test
		boolean isStartedInUpdateRollbackComplete = client.isStartedInUpdateRollbackComplete(stackName);
		Assertions.assertFalse(isStartedInUpdateRollbackComplete);
	}

	@Test
	public void testIsStartedInUpdateRollbackCompleteTrue() {
		stack.setStackStatus(StackStatus.UPDATE_ROLLBACK_COMPLETE);
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeResult);
		// call under test
		boolean isStartedInUpdateRollbackComplete = client.isStartedInUpdateRollbackComplete(stackName);
		Assertions.assertTrue(isStartedInUpdateRollbackComplete);
	}

	@Test
	public void testIsStartedInUpdateRollbackCompleteNoStack() {
		AmazonCloudFormationException exception = new AmazonCloudFormationException("Does not exist");
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(exception);
		// call under test
		boolean isStartedInUpdateRollbackComplete = client.isStartedInUpdateRollbackComplete(stackName);
		Assertions.assertFalse(isStartedInUpdateRollbackComplete);
	}


	@Test
	public void testCreateStack() {
		when(mockCloudFormationClient.createStack(any(CreateStackRequest.class))).thenReturn(createResult);
		// call under test
		client.createStack(inputReqequest);
		verify(mockCloudFormationClient).createStack(createStackRequestCapture.capture());
		CreateStackRequest captureRequest = createStackRequestCapture.getValue();
		Assertions.assertEquals(stackName, captureRequest.getStackName());
		Assertions.assertNotNull(captureRequest.getTemplateURL());
		Assertions.assertNotNull(captureRequest.getParameters());
		Assertions.assertEquals(1, captureRequest.getParameters().size());
		Assertions.assertEquals(parameter, captureRequest.getParameters().get(0));
		List<String> caps = captureRequest.getCapabilities();
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
		Assertions.assertEquals(stackName, request.getStackName());
		Assertions.assertNotNull(request.getTemplateURL());
		Assertions.assertNotNull(request.getParameters());
		Assertions.assertEquals(1, request.getParameters().size());
		Assertions.assertEquals(parameter, request.getParameters().get(0));
		List<String> caps = request.getCapabilities();
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
		when(mockCloudFormationClient.createStack(any(CreateStackRequest.class))).thenReturn(createResult);
		// setup exception to trigger does not exist
		AmazonCloudFormationException exception = new AmazonCloudFormationException("Does not exist");
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(exception);
		// call under test
		client.createOrUpdateStack(inputReqequest);
		verify(mockCloudFormationClient, never()).updateStack(any(UpdateStackRequest.class));
		verify(mockCloudFormationClient).createStack(any(CreateStackRequest.class));
	}
	
	@Test
	public void testSaveTempalteToS3() {
		when(mockConfig.getConfigurationBucket()).thenReturn(bucket);
		// call under test
		SourceBundle bundle = client.saveTempalteToS3(stackName, tempalteBody);
		Assertions.assertNotNull(bundle);
		Assertions.assertEquals(bucket, bundle.getBucket());
		Assertions.assertNotNull(bundle.getKey());
		Assertions.assertTrue(bundle.getKey().startsWith("templates/someStackName"));
		Assertions.assertTrue(bundle.getKey().endsWith(".json"));
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
		AmazonCloudFormationException exception = new AmazonCloudFormationException(CloudFormationClientImpl.NO_UPDATES_ARE_TO_BE_PERFORMED);
		
		when(mockFunction.apply(anyString())).thenThrow(exception);
		// call under test
		client.executeWithS3Template(inputReqequest, mockFunction);
		verify(mockS3Client).putObject(any(PutObjectRequest.class));
		verify(mockFunction).apply(anyString());
		verify(mockS3Client).deleteObject(anyString(), anyString());
		verify(mockLogger).info(any(String.class));
	}

	@Test
	public void testExecuteWithS3TemplateWithError() {
		when(mockConfig.getConfigurationBucket()).thenReturn(bucket);
		AmazonCloudFormationException exception = new AmazonCloudFormationException("some other error");
		when(mockFunction.apply(anyString())).thenThrow(exception);
		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.executeWithS3Template(inputReqequest, mockFunction);
		});
	}
	
	@Test
	public void testWaitForStackToCompleteCreateComplete() throws InterruptedException {
		initStack.setStackStatus(StackStatus.CREATE_IN_PROGRESS);
		stack.setStackStatus(StackStatus.CREATE_COMPLETE);
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		// call under test
		Stack result = client.waitForStackToComplete(stackName).get();
		Assertions.assertNotNull(result);
		verify(mockCloudFormationClient, times(2)).describeStacks(any(DescribeStacksRequest.class));
	}
	
	@Test
	public void testWaitForStackToCompleteUpdateComplete() throws InterruptedException {
		initStack.setStackStatus(StackStatus.UPDATE_IN_PROGRESS);
		stack.setStackStatus(StackStatus.UPDATE_COMPLETE);
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		// call under test
		Stack result = client.waitForStackToComplete(stackName).get();
		Assertions.assertNotNull(result);
		verify(mockCloudFormationClient, times(2)).describeStacks(any(DescribeStacksRequest.class));
	}
	
	
	@Test
	public void testWaitForStackToCompleteTimeout() throws InterruptedException {
		initStack.setStackStatus(StackStatus.CREATE_IN_PROGRESS);
		stack.setStackStatus(StackStatus.CREATE_IN_PROGRESS);
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		when(mockThreadProvider.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,Long.MAX_VALUE);
		// call under test
		try {
			client.waitForStackToComplete(stackName);
			Assertions.fail();
		} catch (RuntimeException e) {
			Assertions.assertTrue(e.getMessage().contains("Timed out"));
			
		}
		verify(mockCloudFormationClient, times(4)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockLogger, times(3)).info(any(String.class));
		verify(mockThreadProvider, times(3)).sleep(any(Long.class));
	}
	
	@Test
	public void testWaitForStackToCompleteTimeoutUpdate() throws InterruptedException {
		initStack.setStackStatus(StackStatus.UPDATE_IN_PROGRESS);
		stack.setStackStatus(StackStatus.UPDATE_IN_PROGRESS);
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		when(mockThreadProvider.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,Long.MAX_VALUE);		// call under test
		try {
			client.waitForStackToComplete(stackName);
			Assertions.fail();
		} catch (RuntimeException e) {
			Assertions.assertTrue(e.getMessage().contains("Timed out"));
			
		}
		verify(mockCloudFormationClient, times(4)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockLogger, times(3)).info(any(String.class));
		verify(mockThreadProvider, times(3)).sleep(any(Long.class));
	}
	
	@Test
	public void testWaitForStackToCompleteTimeoutUpdateCleanup() throws InterruptedException {
		initStack.setStackStatus(StackStatus.UPDATE_IN_PROGRESS);
		stack.setStackStatus(StackStatus.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS);
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		when(mockThreadProvider.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,Long.MAX_VALUE);
		// call under test
		try {
			client.waitForStackToComplete(stackName);
			Assertions.fail();
		} catch (RuntimeException e) {
			Assertions.assertTrue(e.getMessage().contains("Timed out"));
			
		}
		verify(mockCloudFormationClient, times(4)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockLogger, times(3)).info(any(String.class));
		verify(mockThreadProvider, times(3)).sleep(any(Long.class));
	}
	
	@Test
	public void testWaitForStackToCompleteCreateFailed() {
		stack.setStackStatus(StackStatus.CREATE_FAILED);
		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}
	
	@Test
	public void testWaitForStackToCompleteRollbackComplete() throws InterruptedException {
		stack.setStackStatus(StackStatus.ROLLBACK_COMPLETE);
		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}
	
	@Test
	public void testWaitForStackToCompleteRollbackFailed() throws InterruptedException {
		stack.setStackStatus(StackStatus.ROLLBACK_FAILED);
		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}
	
	@Test
	public void testWaitForStackToCompleteRollbackProgress() throws InterruptedException {
		stack.setStackStatus(StackStatus.ROLLBACK_IN_PROGRESS);
		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}
	
	@Test
	public void testWaitForStackToCompleteUpdateFailed() throws InterruptedException {
		stack.setStackStatus(StackStatus.UPDATE_ROLLBACK_FAILED);
		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}

	@Test
	public void testWaitForStackToCompleteUpdateCompleteToUpdateRollBackComplete() throws InterruptedException {
		initStack.setStackStatus(StackStatus.UPDATE_COMPLETE);
		stack.setStackStatus(StackStatus.UPDATE_ROLLBACK_COMPLETE);
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		RuntimeException expectedEx = Assertions.assertThrows(RuntimeException.class, () -> {
			// call under test
			client.waitForStackToComplete(stackName);
		});
	}

	@Test
	public void testWaitForStackToCompleteUpdateRollbackCompleteToUpdateRollBackComplete() throws InterruptedException {
		initStack.setStackStatus(StackStatus.UPDATE_ROLLBACK_COMPLETE);
		stack.setStackStatus(StackStatus.UPDATE_ROLLBACK_COMPLETE);
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(initDescribeResult, describeResult);
		// call under test
		Stack resultStack = client.waitForStackToComplete(stackName).get();
		verify(mockCloudFormationClient, times(2)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockThreadProvider, times(2)).currentTimeMillis();
		verify(mockThreadProvider, never()).sleep(anyLong());
		Assertions.assertNotNull(resultStack);
		Assertions.assertNotNull(resultStack.getStackStatus());
		Assertions.assertEquals(StackStatus.UPDATE_ROLLBACK_COMPLETE, StackStatus.fromValue(resultStack.getStackStatus()));
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
			String output = client.getOutput(stackName, "invalidKey");
		});
	}
	
	@Test
	public void testDeleteStackIfExistsWithDoesNotExist() {
		when(mockCloudFormationClient.describeStacks(any())).thenThrow(new AmazonCloudFormationException("does not exist"));
		// call under test
		client.deleteStackIfExists(stackName);
		verify(mockCloudFormationClient).describeStacks(new DescribeStacksRequest().withStackName(stackName));
		verify(mockCloudFormationClient, never()).deleteStack(any());
	}
	
	@Test
	public void testDeleteStackIfExists() {
		Stack createComplete = new Stack().withStackStatus(StackStatus.CREATE_COMPLETE);
		Stack deleteInProgress = new Stack().withStackStatus(StackStatus.DELETE_IN_PROGRESS);
		Stack deleteComplete = new Stack().withStackStatus(StackStatus.DELETE_COMPLETE);
		
		when(mockCloudFormationClient.describeStacks(any())).thenReturn(
				new DescribeStacksResult().withStacks(List.of(createComplete)),
				new DescribeStacksResult().withStacks(List.of(deleteInProgress)),
				new DescribeStacksResult().withStacks(List.of(deleteInProgress)),
				new DescribeStacksResult().withStacks(List.of(deleteComplete)));
		// call under test
		client.deleteStackIfExists(stackName);
		
		verify(mockCloudFormationClient, times(4)).describeStacks(new DescribeStacksRequest().withStackName(stackName));
		verify(mockCloudFormationClient).deleteStack(new DeleteStackRequest().withStackName(stackName));
	}
	
	@Test
	public void testDeleteStackIfExistsWithSkipComplete() {
		Stack createComplete = new Stack().withStackStatus(StackStatus.CREATE_COMPLETE);
		Stack deleteInProgress = new Stack().withStackStatus(StackStatus.DELETE_IN_PROGRESS);

		
		when(mockCloudFormationClient.describeStacks(any())).thenReturn(
				new DescribeStacksResult().withStacks(List.of(createComplete)),
				new DescribeStacksResult().withStacks(List.of(deleteInProgress)),
				new DescribeStacksResult().withStacks(List.of(deleteInProgress)))
		.thenThrow(new AmazonCloudFormationException("does not exist"));
		// call under test
		client.deleteStackIfExists(stackName);
		
		verify(mockCloudFormationClient, times(4)).describeStacks(new DescribeStacksRequest().withStackName(stackName));
		verify(mockCloudFormationClient).deleteStack(new DeleteStackRequest().withStackName(stackName));
	}

}
