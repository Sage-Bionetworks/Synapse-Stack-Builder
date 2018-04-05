package org.sagebionetworks.template;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

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

@RunWith(MockitoJUnitRunner.class)
public class CloudFormationClientImplTest {

	@Mock
	AmazonCloudFormation mockCloudFormationClient;
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

	Stack stack;

	@Before
	public void before() {
		client = new CloudFormationClientImpl(mockCloudFormationClient);

		stackId = "theStackId";
		stack = new Stack().withStackId(stackId);
		describeResult = new DescribeStacksResult().withStacks(stack);

		updateResult = new UpdateStackResult().withStackId(stackId);

		createResult = new CreateStackResult().withStackId(stackId);

		stackName = "someStackName";
		tempalteBody = "body";
		parameter = new Parameter().withParameterKey("paramKey").withParameterValue("paramValue");
		parameters = new Parameter[] { parameter };
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeResult);
		when(mockCloudFormationClient.createStack(any(CreateStackRequest.class))).thenReturn(createResult);
		when(mockCloudFormationClient.updateStack(any(UpdateStackRequest.class))).thenReturn(updateResult);
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
		String resultId = client.createStack(stackName, tempalteBody, parameters);
		assertEquals(stackId, resultId);
		verify(mockCloudFormationClient).createStack(createStackRequestCapture.capture());
		CreateStackRequest request = createStackRequestCapture.getValue();
		assertEquals(stackName, request.getStackName());
		assertEquals(tempalteBody, request.getTemplateBody());
		assertNotNull(request.getParameters());
		assertEquals(1, request.getParameters().size());
		assertEquals(parameter, request.getParameters().get(0));
	}
	
	@Test
	public void testUpdateStack() {
		// call under test
		String resultId = client.updateStack(stackName, tempalteBody, parameters);
		assertEquals(stackId, resultId);
		verify(mockCloudFormationClient).updateStack(updateStackRequestCapture.capture());
		UpdateStackRequest request = updateStackRequestCapture.getValue();
		assertEquals(stackName, request.getStackName());
		assertEquals(tempalteBody, request.getTemplateBody());
		assertNotNull(request.getParameters());
		assertEquals(1, request.getParameters().size());
		assertEquals(parameter, request.getParameters().get(0));
	}
	
	@Test
	public void testCreateOrUpdateAsUpdate() {
		// call under test
		String resultId = client.createOrUpdateStack(stackName, tempalteBody, parameters);
		assertEquals(stackId, resultId);
		verify(mockCloudFormationClient).updateStack(any(UpdateStackRequest.class));
		verify(mockCloudFormationClient, never()).createStack(any(CreateStackRequest.class));
	}
	
	@Test
	public void testCreateOrUpdateAsCreate() {
		// setup exception to trigger does not exist
		AmazonCloudFormationException exception = new AmazonCloudFormationException("Does not exist");
		when(mockCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(exception);
		// call under test
		String resultId = client.createOrUpdateStack(stackName, tempalteBody, parameters);
		assertEquals(stackId, resultId);
		verify(mockCloudFormationClient, never()).updateStack(any(UpdateStackRequest.class));
		verify(mockCloudFormationClient).createStack(any(CreateStackRequest.class));
	}
}
