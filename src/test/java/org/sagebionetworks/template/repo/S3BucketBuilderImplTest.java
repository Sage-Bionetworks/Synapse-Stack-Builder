package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_S3_BUCKETS_CSV;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;


@RunWith(MockitoJUnitRunner.class)
public class S3BucketBuilderImplTest {

	@Mock
	RepoConfiguration mockConfig;
	@Mock
	AmazonS3 mockS3Client;
	@Captor
	ArgumentCaptor<SetBucketEncryptionRequest> encryptionRequestCaptor;
	
	@InjectMocks
	S3BucketBuilderImpl builder;
	
	@Test
	public void testBuildAllBucketsAlreadyEncrypted() {
		String[] rawBucketNames = new String[] {"${stack}.one", "${stack}.two"};
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_S3_BUCKETS_CSV)).thenReturn(rawBucketNames);
		String stack = "dev";
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		
		// call under test
		builder.buildAllBuckets();
		verify(mockS3Client).createBucket("dev.one");
		verify(mockS3Client).getBucketEncryption("dev.one");
		verify(mockS3Client).createBucket("dev.two");
		verify(mockS3Client).getBucketEncryption("dev.two");
		verify(mockS3Client, never()).setBucketEncryption(any(SetBucketEncryptionRequest.class));
	}
	
	
	@Test
	public void testBuildAllBucketsNeedsEncypted() {
		String[] rawBucketNames = new String[] {"${stack}.one"};
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_S3_BUCKETS_CSV)).thenReturn(rawBucketNames);
		String stack = "dev";
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		
		AmazonServiceException notFound = new AmazonServiceException("NotFound");
		notFound.setStatusCode(404);
		doThrow(notFound).when(mockS3Client).getBucketEncryption("dev.one");
		
		// call under test
		builder.buildAllBuckets();
		
		verify(mockS3Client).createBucket("dev.one");
		verify(mockS3Client).getBucketEncryption("dev.one");
		verify(mockS3Client).setBucketEncryption(encryptionRequestCaptor.capture());
		SetBucketEncryptionRequest request = encryptionRequestCaptor.getValue();
		assertNotNull(request);
		assertEquals("dev.one",request.getBucketName());
		assertNotNull(request.getServerSideEncryptionConfiguration());
		assertNotNull(request.getServerSideEncryptionConfiguration().getRules());
		assertEquals(1, request.getServerSideEncryptionConfiguration().getRules().size());
		ServerSideEncryptionRule rule = request.getServerSideEncryptionConfiguration().getRules().get(0);
		assertNotNull(rule.getApplyServerSideEncryptionByDefault());
		assertEquals(SSEAlgorithm.AES256.name(), rule.getApplyServerSideEncryptionByDefault().getSSEAlgorithm());
		
	}
	
	@Test
	public void testBuildAllBucketsUnknowError() {
		String[] rawBucketNames = new String[] {"${stack}.one"};
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_S3_BUCKETS_CSV)).thenReturn(rawBucketNames);
		String stack = "dev";
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		
		// some other exception
		AmazonServiceException exception = new AmazonServiceException("some other error");
		exception.setStatusCode(500);
		doThrow(exception).when(mockS3Client).getBucketEncryption("dev.one");
		
		try {
			// call under test
			builder.buildAllBuckets();
			fail();
		} catch (Exception e) {
			assertEquals(exception, e);
		}
		
		verify(mockS3Client).createBucket("dev.one");
		verify(mockS3Client).getBucketEncryption("dev.one");
		verify(mockS3Client, never()).setBucketEncryption(any(SetBucketEncryptionRequest.class));
		
	}
	
	@Test
	public void testBuildAllBucketsBadName() {
		// bad name
		String[] rawBucketNames = new String[] {"${stack}.${instance}.one"};
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_S3_BUCKETS_CSV)).thenReturn(rawBucketNames);
		String stack = "dev";
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		
		try {
			// call under test
			builder.buildAllBuckets();
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
		
		verify(mockS3Client, never()).createBucket(anyString());
		verify(mockS3Client, never()).getBucketEncryption(anyString());
		verify(mockS3Client, never()).setBucketEncryption(any(SetBucketEncryptionRequest.class));
		
	}
}
