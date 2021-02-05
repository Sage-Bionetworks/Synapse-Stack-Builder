package org.sagebionetworks.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;

import com.amazonaws.services.cloudformation.model.Output;
import net.bytebuddy.build.ToStringPlugin;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;

@RunWith(MockitoJUnitRunner.class)
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
	DescribeStacksResult describeResult;
	UpdateStackResult updateResult;
	CreateStackResult createResult;
	CreateOrUpdateStackRequest inputReqequest;

	Stack stack;
	
	String bucket;
	
	String[] capabilities;

	@Before
	public void before() throws MalformedURLException {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		
		client = new CloudFormationClientImpl(mockCloudFormationClient, mockS3Client, mockConfig, mockLoggerFactory, mockThreadProvider);

		stackId = "theStackId";
		Collection<Output> outputs = new ArrayList<>();
		Output output1 = new Output().withOutputKey(SES_SYNAPSE_ORG_COMPLAINT_TOPIC_KEY).withOutputValue(SES_SYNAPSE_ORG_COMPLAINT_TOPIC_VALUE);
		Output output2 = new Output().withOutputKey(SES_SYNAPSE_ORG_BOUNCE_TOPIC_KEY).withOutputValue(SES_SYNAPSE_ORG_BOUNCE_TOPIC_VALUE);
		outputs.add(output1);
		outputs.add(output2);
		stack = new Stack().withStackId(stackId).withOutputs(outputs);
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
		
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeResult);
		when(mockCloudFormationClient.createStack(any(CreateStackRequest.class))).thenReturn(createResult);
		when(mockCloudFormationClient.updateStack(any(UpdateStackRequest.class))).thenReturn(updateResult);

		bucket = "theBucket";
		when(mockConfig.getConfigurationBucket()).thenReturn(bucket);
		
		when(mockThreadProvider.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,Long.MAX_VALUE);
		
	}

	@Test
	public void testDescribeStack() {
		// call under test
		Stack result = client.describeStack(stackName);
		assertNotNull(result);
		assertEquals(stackId, result.getStackId());
		verify(mockCloudFormationClient).describeStacks(describeStackRequestCapture.capture());
		assertEquals(stackName, describeStackRequestCapture.getValue().getStackName());
	}

	@Test
	public void testDoesStackNameExistTrue() {
		// call under test
		boolean exists = client.doesStackNameExist(stackName);
		assertTrue(exists);
	}

	@Test
	public void testDoesStackNameExistFalse() {
		// setup exception to trigger does not exist
		AmazonCloudFormationException exception = new AmazonCloudFormationException("Does not exist");
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(exception);
		// call under test
		boolean exists = client.doesStackNameExist(stackName);
		assertFalse(exists);
	}

	@Test
	public void testCreateStack() {
		// call under test
		client.createStack(inputReqequest);
		verify(mockCloudFormationClient).createStack(createStackRequestCapture.capture());
		CreateStackRequest captureRequest = createStackRequestCapture.getValue();
		assertEquals(stackName, captureRequest.getStackName());
		assertNotNull(captureRequest.getTemplateURL());
		assertNotNull(captureRequest.getParameters());
		assertEquals(1, captureRequest.getParameters().size());
		assertEquals(parameter, captureRequest.getParameters().get(0));
		List<String> caps = captureRequest.getCapabilities();
		assertNotNull(caps);
		assertEquals(capabilities.length, caps.size());
		assertEquals(capabilities[0], caps.get(0));
		assertEquals(capabilities[1], caps.get(1));
	}
	
	@Test
	public void testUpdateStack() {
		// call under test
		client.updateStack(inputReqequest);
		verify(mockCloudFormationClient).updateStack(updateStackRequestCapture.capture());
		UpdateStackRequest request = updateStackRequestCapture.getValue();
		assertEquals(stackName, request.getStackName());
		assertNotNull(request.getTemplateURL());
		assertNotNull(request.getParameters());
		assertEquals(1, request.getParameters().size());
		assertEquals(parameter, request.getParameters().get(0));
		List<String> caps = request.getCapabilities();
		assertNotNull(caps);
		assertEquals(capabilities.length, caps.size());
		assertEquals(capabilities[0], caps.get(0));
		assertEquals(capabilities[1], caps.get(1));
	}
	
	@Test
	public void testCreateOrUpdateAsUpdate() {
		// call under test
		client.createOrUpdateStack(inputReqequest);
		verify(mockCloudFormationClient).updateStack(any(UpdateStackRequest.class));
		verify(mockCloudFormationClient, never()).createStack(any(CreateStackRequest.class));
	}
	
	@Test
	public void testCreateOrUpdateAsCreate() {
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
		// call under test
		SourceBundle bundle = client.saveTempalteToS3(stackName, tempalteBody);
		assertNotNull(bundle);
		assertEquals(bucket, bundle.getBucket());
		assertNotNull(bundle.getKey());
		assertTrue(bundle.getKey().startsWith("templates/someStackName"));
		assertTrue(bundle.getKey().endsWith(".json"));
		ArgumentCaptor<PutObjectRequest> requestCapture = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(mockS3Client).putObject(requestCapture.capture());
		PutObjectRequest request = requestCapture.getValue();
		assertNotNull(request);
		assertEquals(bucket, request.getBucketName());
		assertEquals(bundle.getKey(), request.getKey());
		assertNotNull(request.getMetadata());
		assertEquals(4L, request.getMetadata().getContentLength());
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
		when(mockFunction.apply(anyString())).thenReturn(stackId);
		// call under test
		client.executeWithS3Template(inputReqequest, mockFunction);
		verify(mockS3Client).putObject(any(PutObjectRequest.class));
		verify(mockFunction).apply(anyString());
		verify(mockS3Client).deleteObject(anyString(), anyString());
	}
	
	
	@Test
	public void testExecuteWithS3TemplateNoUpdates() {
		AmazonCloudFormationException exception = new AmazonCloudFormationException(CloudFormationClientImpl.NO_UPDATES_ARE_TO_BE_PERFORMED);
		
		when(mockFunction.apply(anyString())).thenThrow(exception);
		// call under test
		client.executeWithS3Template(inputReqequest, mockFunction);
		verify(mockS3Client).putObject(any(PutObjectRequest.class));
		verify(mockFunction).apply(anyString());
		verify(mockS3Client).deleteObject(anyString(), anyString());
		verify(mockLogger).info(any(String.class));
	}
	
	@Test (expected=RuntimeException.class)
	public void testExecuteWithS3TemplateWithError() {
		AmazonCloudFormationException exception = new AmazonCloudFormationException("some other error");
		when(mockFunction.apply(anyString())).thenThrow(exception);
		// call under test
		client.executeWithS3Template(inputReqequest, mockFunction);
	}
	
	@Test
	public void testWaitForStackToCompleteCreateComplete() throws InterruptedException {
		stack.setStackStatus(StackStatus.CREATE_COMPLETE);
		// call under test
		Stack result = client.waitForStackToComplete(stackName);
		assertNotNull(result);
		verify(mockCloudFormationClient).describeStacks(any(DescribeStacksRequest.class));
	}
	
	@Test
	public void testWaitForStackToCompleteUpdateComplete() throws InterruptedException {
		stack.setStackStatus(StackStatus.UPDATE_COMPLETE);
		// call under test
		Stack result = client.waitForStackToComplete(stackName);
		assertNotNull(result);
		verify(mockCloudFormationClient).describeStacks(any(DescribeStacksRequest.class));
	}
	
	
	@Test
	public void testWaitForStackToCompleteTimeout() throws InterruptedException {
		stack.setStackStatus(StackStatus.CREATE_IN_PROGRESS);
		// call under test
		try {
			client.waitForStackToComplete(stackName);
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("Timed out"));
			
		}
		verify(mockCloudFormationClient, times(3)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockLogger, times(3)).info(any(String.class));
		verify(mockThreadProvider, times(3)).sleep(any(Long.class));
	}
	
	@Test
	public void testWaitForStackToCompleteTimeoutUpdate() throws InterruptedException {
		stack.setStackStatus(StackStatus.UPDATE_IN_PROGRESS);
		// call under test
		try {
			client.waitForStackToComplete(stackName);
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("Timed out"));
			
		}
		verify(mockCloudFormationClient, times(3)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockLogger, times(3)).info(any(String.class));
		verify(mockThreadProvider, times(3)).sleep(any(Long.class));
	}
	
	@Test
	public void testWaitForStackToCompleteTimeoutUpdateCleanup() throws InterruptedException {
		stack.setStackStatus(StackStatus.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS);
		// call under test
		try {
			client.waitForStackToComplete(stackName);
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("Timed out"));
			
		}
		verify(mockCloudFormationClient, times(3)).describeStacks(any(DescribeStacksRequest.class));
		verify(mockLogger, times(3)).info(any(String.class));
		verify(mockThreadProvider, times(3)).sleep(any(Long.class));
	}
	
	@Test (expected=RuntimeException.class)
	public void testWaitForStackToCompleteCreateFailed() throws InterruptedException {
		stack.setStackStatus(StackStatus.CREATE_FAILED);
		// call under test
		client.waitForStackToComplete(stackName);
	}
	
	@Test (expected=RuntimeException.class)
	public void testWaitForStackToCompleteRollbackComplete() throws InterruptedException {
		stack.setStackStatus(StackStatus.ROLLBACK_COMPLETE);
		// call under test
		client.waitForStackToComplete(stackName);
	}
	
	@Test (expected=RuntimeException.class)
	public void testWaitForStackToCompleteRollbackFailed() throws InterruptedException {
		stack.setStackStatus(StackStatus.ROLLBACK_FAILED);
		// call under test
		client.waitForStackToComplete(stackName);
	}
	
	@Test (expected=RuntimeException.class)
	public void testWaitForStackToCompleteRollbackProgress() throws InterruptedException {
		stack.setStackStatus(StackStatus.ROLLBACK_IN_PROGRESS);
		// call under test
		client.waitForStackToComplete(stackName);
	}
	
	@Test (expected=RuntimeException.class)
	public void testWaitForStackToCompleteUpdateFailed() throws InterruptedException {
		stack.setStackStatus(StackStatus.UPDATE_ROLLBACK_FAILED);
		// call under test
		client.waitForStackToComplete(stackName);
	}

	@Test
	public void testGetOutput() {
		// call under test
		String output = client.getOutput(stackName, SES_SYNAPSE_ORG_COMPLAINT_TOPIC_KEY);

		assertEquals(SES_SYNAPSE_ORG_COMPLAINT_TOPIC_VALUE, output);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetOutputInvalid() {
		// call under test
		String output = client.getOutput(stackName, "invalidKey");

	}
	
}
