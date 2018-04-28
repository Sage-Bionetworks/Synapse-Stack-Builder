package org.sagebionetworks.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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
import com.amazonaws.services.s3.model.PutObjectRequest;

@RunWith(MockitoJUnitRunner.class)
public class CloudFormationClientImplTest {

	@Mock
	AmazonCloudFormation mockCloudFormationClient;
	@Mock
	AmazonS3 mockS3Client;
	@Mock
	Configuration mockConfig;
	@Mock
	Function<String, String> mockFunction;
	
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
	
	URL presignedUrl;

	Stack stack;
	
	String bucket;

	@Before
	public void before() throws MalformedURLException {
		client = new CloudFormationClientImpl(mockCloudFormationClient, mockS3Client, mockConfig);

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
		
		bucket = "theBucket";
		when(mockConfig.getConfigurationBucket()).thenReturn(bucket);
		
		presignedUrl = new URL("http://www.amazon.com/bucket/key");
		when(mockS3Client.generatePresignedUrl(anyString(), anyString(), any(Date.class), any(HttpMethod.class))).thenReturn(presignedUrl);
		
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
		assertEquals(presignedUrl.toString(), request.getTemplateURL());
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
		assertEquals(presignedUrl.toString(), request.getTemplateURL());
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
		String resultId = client.executeWithS3Template(stackName, tempalteBody, mockFunction);
		assertEquals(stackId, resultId);
		verify(mockS3Client).putObject(any(PutObjectRequest.class));
		verify(mockFunction).apply(presignedUrl.toString());
		verify(mockS3Client).deleteObject(anyString(), anyString());
	}
	
}
