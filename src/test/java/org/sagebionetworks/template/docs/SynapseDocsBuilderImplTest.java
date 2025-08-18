package org.sagebionetworks.template.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.*;
import java.util.function.Consumer;

import org.json.JSONObject;

import static org.mockito.Mockito.*;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_SOURCE_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DESTINATION_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG;
import static org.sagebionetworks.template.Constants.DOCS_STACK_INSTANCE_JSON_FILE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.config.RepoConfiguration;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.Copy;

@ExtendWith(MockitoExtension.class)
public class SynapseDocsBuilderImplTest {
	
	@Mock
	private S3Client mockS3Client;
	
	@Mock
	private RepoConfiguration mockConfig;
	
	private ListObjectsV2Response mockSourceListing;

	private ListObjectsV2Response mockDestinationListing;
	
	private ListObjectsV2Request mockSourceListRequest;
	
	private ListObjectsV2Request mockDestinationListRequest;
	
	private Copy mockCopy;
	private CompletedCopy mockCompletedCopy;
	
	private String prodInstance;
	private String sourceBucket;
	private String destinationBucket;
	private JSONObject instanceObjectOutOfDate;
	private JSONObject instanceObjectUpToDate;
	private String jsonOutOfDate;
	private String jsonUpToDate;
	private S3Object object;
	private List<S3Object> objects;
	private String objectKey;
	private String prefix;
	
	@InjectMocks
	private SynapseDocsBuilderImpl builder;
	
	private SynapseDocsBuilderImpl builderSpy;
	
	@BeforeEach
	public void before() {
		prefix = "";
		prodInstance = "2";
		// up to date (2)
		instanceObjectUpToDate = new JSONObject();
		instanceObjectUpToDate.put(PROPERTY_KEY_INSTANCE, 2);
		jsonUpToDate = instanceObjectUpToDate.toString();
		// out of date (1)
		instanceObjectOutOfDate = new JSONObject();
		instanceObjectOutOfDate.put(PROPERTY_KEY_INSTANCE, 1);
		jsonOutOfDate = instanceObjectOutOfDate.toString();
		sourceBucket = "sourceBucket";
		destinationBucket = "destinationBucket";
		mockSourceListRequest = ListObjectsV2Request.builder().build();
		mockDestinationListRequest = ListObjectsV2Request.builder().build();
		objectKey = "objectKey";
		object = S3Object.builder()
				.key(objectKey)
				.eTag("etag")
				.build();
		objects = Collections.singletonList(object);
		builder = new SynapseDocsBuilderImpl(mockS3Client, mockConfig);
		builderSpy = spy(builder);
	}
	
	@Test
	public void testDeployDocs() {
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_SOURCE_BUCKET)).thenReturn(sourceBucket);
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_DESTINATION_BUCKET)).thenReturn(destinationBucket);
		doAnswer(invocation -> true).when(builderSpy).verifyDeployment(destinationBucket);
		doNothing().when(builderSpy).sync(sourceBucket, destinationBucket);
		builderSpy.deployDocs();
		verify(builderSpy).verifyDeployment(destinationBucket);
		verify(builderSpy).sync(sourceBucket, destinationBucket);
	}
	
	@Test
	public void testDeployDocsWithMissingSourceBucketName() {
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_SOURCE_BUCKET)).thenThrow(ConfigurationPropertyNotFound.class);
		builderSpy.deployDocs();
		verify(builderSpy, never()).verifyDeployment(any());
		verify(builderSpy, never()).sync(any(), any());
	}
	
	@Test
	public void testDeployDocsWithMissingDestinationBucketName() {
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_SOURCE_BUCKET)).thenReturn(sourceBucket);
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_DESTINATION_BUCKET)).thenThrow(ConfigurationPropertyNotFound.class);
		builderSpy.deployDocs();
		verify(builderSpy, never()).verifyDeployment(any());
		verify(builderSpy, never()).sync(any(), any());
	}
	
	@Test
	public void testDeployDocsWithNoDeployment() {
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_SOURCE_BUCKET)).thenReturn(sourceBucket);
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_DESTINATION_BUCKET)).thenReturn(destinationBucket);
		doAnswer(invocation -> false).when(builderSpy).verifyDeployment(destinationBucket);
		builderSpy.deployDocs();
		verify(builderSpy).verifyDeployment(destinationBucket);
		verify(builderSpy, never()).sync(any(), any());
	}	
	
	@Test
	public void testVerifyDeploymentWithFalseFlag() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(false);
		// call under test
		assertFalse(builder.verifyDeployment(destinationBucket));
	}

	@Test
	public void testVerifyDeploymentWithMissingDeploymentFlag() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG))
			.thenThrow(ConfigurationPropertyNotFound.class);
		// call under test
		assertFalse(builder.verifyDeployment(destinationBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithUpToDateDocs() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(true);
		ArgumentCaptor<Consumer<HeadObjectRequest.Builder>> headObjectRequestCaptor = ArgumentCaptor.forClass(Consumer.class);
		HeadObjectResponse headObjectResponse = HeadObjectResponse.builder().build();
		when(mockS3Client.headObject(headObjectRequestCaptor.capture())).thenReturn(headObjectResponse);
		ArgumentCaptor<Consumer<GetObjectRequest.Builder>> getObjectRequestCaptor = ArgumentCaptor.forClass(Consumer.class);
		ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), jsonUpToDate.getBytes());
		when(mockS3Client.getObjectAsBytes(getObjectRequestCaptor.capture())).thenReturn(responseBytes);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		assertFalse(builder.verifyDeployment(destinationBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithOutOfDateDocs() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(true);
		ArgumentCaptor<Consumer<HeadObjectRequest.Builder>> headObjectRequestCaptor = ArgumentCaptor.forClass(Consumer.class);
		HeadObjectResponse headObjectResponse = HeadObjectResponse.builder().build();
		when(mockS3Client.headObject(headObjectRequestCaptor.capture())).thenReturn(headObjectResponse);
		ArgumentCaptor<Consumer<GetObjectRequest.Builder>> getObjectRequestCaptor = ArgumentCaptor.forClass(Consumer.class);
		ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), jsonOutOfDate.getBytes());
		when(mockS3Client.getObjectAsBytes(getObjectRequestCaptor.capture())).thenReturn(responseBytes);
		// JSON tracking of instance < prod instance
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		assertTrue(builder.verifyDeployment(destinationBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithNoInstanceJsonFile() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(true);
		doAnswer(invocation -> false).when(builderSpy).doesObjectExist(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE);
		// call under test
		assertTrue(builderSpy.verifyDeployment(destinationBucket));
	}
	
	@Test
	public void testSyncWithDestinationEmpty() throws Exception {

		// Source pages
		ListObjectsV2Response srcPage1 = ListObjectsV2Response.builder()
		        .isTruncated(true)
		        .nextContinuationToken("token-2")
		        .contents(
		                S3Object.builder().key("a.txt").eTag("etag-a").build(),
		                S3Object.builder().key("b.txt").eTag("etag-b").build()
		        ).build();

		ListObjectsV2Response srcPage2 = ListObjectsV2Response.builder()
		        .isTruncated(false)
		        .contents(S3Object.builder().key("c.txt").eTag("etag-c").build())
		        .build();

		// Destination single empty page
		ListObjectsV2Response dstEmpty = ListObjectsV2Response.builder()
		        .isTruncated(false)
		        .contents(java.util.Collections.emptyList())
		        .build();

		// Return a real paginator; decide which pages via listObjectsV2 stubs
		when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
		        .thenAnswer(inv -> new ListObjectsV2Iterable(mockS3Client, inv.getArgument(0)));

		// Stub the underlying page fetches with null-safe matchers
		when(mockS3Client.listObjectsV2(argThat((ListObjectsV2Request r) ->
		        r != null && sourceBucket.equals(r.bucket()) && r.continuationToken() == null)))
		        .thenReturn(srcPage1);

		when(mockS3Client.listObjectsV2(argThat((ListObjectsV2Request r) ->
		        r != null && sourceBucket.equals(r.bucket()) && "token-2".equals(r.continuationToken()))))
		        .thenReturn(srcPage2);

		when(mockS3Client.listObjectsV2(argThat((ListObjectsV2Request r) ->
		        r != null && destinationBucket.equals(r.bucket()))))
		        .thenReturn(dstEmpty);

		when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());

		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);

		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		
		verify(mockS3Client, never()).deleteObject(any(DeleteObjectRequest.class));
		
		// Verify putObject is called with the correct parameters
		ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
		verify(mockS3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());
		
		PutObjectRequest capturedPutObjectRequest = putObjectRequestCaptor.getValue();
		assertEquals(destinationBucket, capturedPutObjectRequest.bucket());
		assertEquals(DOCS_STACK_INSTANCE_JSON_FILE, capturedPutObjectRequest.key());
	}
	
	@Test
	public void testSyncWithDestinationSameKeyWithSameETag() throws Exception {
/*
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		
		verify(mockS3Client, never()).deleteObject(any(DeleteObjectRequest.class));
		
		// Verify putObject is called with the correct parameters
		ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
		verify(mockS3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());
		
		PutObjectRequest capturedPutObjectRequest = putObjectRequestCaptor.getValue();
		assertEquals(destinationBucket, capturedPutObjectRequest.bucket());
		assertEquals(DOCS_STACK_INSTANCE_JSON_FILE, capturedPutObjectRequest.key());
 */

		// Source page
		ListObjectsV2Response srcPage2 = ListObjectsV2Response.builder()
				.isTruncated(false)
				.contents(S3Object.builder().key("c.txt").eTag("etag-c").build())
				.build();

		// Destination single empty page
		ListObjectsV2Response dstEmpty = ListObjectsV2Response.builder()
				.isTruncated(false)
				.contents(S3Object.builder().key("c.txt").eTag("etag-c").build())
				.build();

		// Return a real paginator; decide which pages via listObjectsV2 stubs
		when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
				.thenAnswer(inv -> new ListObjectsV2Iterable(mockS3Client, inv.getArgument(0)));

		// Stub the underlying page fetches with null-safe matchers
		when(mockS3Client.listObjectsV2(argThat((ListObjectsV2Request r) ->
				r != null && sourceBucket.equals(r.bucket()))))
				.thenReturn(srcPage2);

		when(mockS3Client.listObjectsV2(argThat((ListObjectsV2Request r) ->
				r != null && destinationBucket.equals(r.bucket()))))
				.thenReturn(dstEmpty);

		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);

		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);

		verify(mockS3Client, never()).copyObject(any(CopyObjectRequest.class));
		verify(mockS3Client, never()).deleteObject(any(DeleteObjectRequest.class));

		// Verify putObject is called with the correct parameters
		ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
		verify(mockS3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());

		PutObjectRequest capturedPutObjectRequest = putObjectRequestCaptor.getValue();
		assertEquals(destinationBucket, capturedPutObjectRequest.bucket());
		assertEquals(DOCS_STACK_INSTANCE_JSON_FILE, capturedPutObjectRequest.key());
	}
	
	@Test
	public void testSyncWithDestinationSameKeyWithDifferentETag() throws Exception {
		// Source page
		ListObjectsV2Response srcPage2 = ListObjectsV2Response.builder()
				.isTruncated(false)
				.contents(S3Object.builder().key("c.txt").eTag("etag-c").build())
				.build();

		// Destination single empty page
		ListObjectsV2Response dstEmpty = ListObjectsV2Response.builder()
				.isTruncated(false)
				.contents(S3Object.builder().key("c.txt").eTag("etag-d").build())
				.build();

		// Return a real paginator; decide which pages via listObjectsV2 stubs
		when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
				.thenAnswer(inv -> new ListObjectsV2Iterable(mockS3Client, inv.getArgument(0)));

		// Stub the underlying page fetches with null-safe matchers
		when(mockS3Client.listObjectsV2(argThat((ListObjectsV2Request r) ->
				r != null && sourceBucket.equals(r.bucket()))))
				.thenReturn(srcPage2);

		when(mockS3Client.listObjectsV2(argThat((ListObjectsV2Request r) ->
				r != null && destinationBucket.equals(r.bucket()))))
				.thenReturn(dstEmpty);

		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);

		CopyObjectResponse expectedCopyObjectResponse = CopyObjectResponse.builder().build();
		when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(expectedCopyObjectResponse);

		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);

		ArgumentCaptor<CopyObjectRequest> copyObjectRequestCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
		verify(mockS3Client).copyObject(copyObjectRequestCaptor.capture());
		CopyObjectRequest actualCopyObjectRequest = copyObjectRequestCaptor.getValue();
		assertEquals(destinationBucket, actualCopyObjectRequest.destinationBucket());
		assertEquals("c.txt", actualCopyObjectRequest.destinationKey());

		verify(mockS3Client, never()).deleteObject(any(DeleteObjectRequest.class));

		// Verify putObject is called with the correct parameters
		ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
		verify(mockS3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());

		PutObjectRequest capturedPutObjectRequest = putObjectRequestCaptor.getValue();
		assertEquals(destinationBucket, capturedPutObjectRequest.bucket());
		assertEquals(DOCS_STACK_INSTANCE_JSON_FILE, capturedPutObjectRequest.key());
	}
	
	@Test
	public void testSyncWithDestinationDeleteExistingFile() throws Exception {
		// Source page
		ListObjectsV2Response srcPage2 = ListObjectsV2Response.builder()
				.isTruncated(false)
				.contents(S3Object.builder().key("c.txt").eTag("etag-c").build())
				.build();

		// Destination page
		ListObjectsV2Response dstPage = ListObjectsV2Response.builder()
				.isTruncated(false)
				.contents(S3Object.builder().key("d.txt").eTag("etag-d").build())
				.build();

		// Return a real paginator; decide which pages via listObjectsV2 stubs
		when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
				.thenAnswer(inv -> new ListObjectsV2Iterable(mockS3Client, inv.getArgument(0)));

		// Stub the underlying page fetches with null-safe matchers
		when(mockS3Client.listObjectsV2(argThat((ListObjectsV2Request r) ->
				r != null && sourceBucket.equals(r.bucket()))))
				.thenReturn(srcPage2);

		when(mockS3Client.listObjectsV2(argThat((ListObjectsV2Request r) ->
				r != null && destinationBucket.equals(r.bucket()))))
				.thenReturn(dstPage);

		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);

		CopyObjectResponse expectedCopyObjectResponse = CopyObjectResponse.builder().build();
		when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(expectedCopyObjectResponse);

		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);

		ArgumentCaptor<CopyObjectRequest> copyObjectRequestCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
		verify(mockS3Client).copyObject(copyObjectRequestCaptor.capture());
		CopyObjectRequest actualCopyObjectRequest = copyObjectRequestCaptor.getValue();
		assertEquals(destinationBucket, actualCopyObjectRequest.destinationBucket());
		assertEquals("c.txt", actualCopyObjectRequest.destinationKey());

		ArgumentCaptor<DeleteObjectsRequest> deleteObjectsRequestCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
		verify(mockS3Client).deleteObjects(deleteObjectsRequestCaptor.capture());
		DeleteObjectsRequest actualDeleteObjectsRequest = deleteObjectsRequestCaptor.getValue();
		assertEquals(destinationBucket, actualDeleteObjectsRequest.bucket());
		assertEquals(1, actualDeleteObjectsRequest.delete().objects().size());
		assertEquals("d.txt", actualDeleteObjectsRequest.delete().objects().get(0).key());

		// Verify putObject is called with the correct parameters
		ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
		verify(mockS3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());

		PutObjectRequest capturedPutObjectRequest = putObjectRequestCaptor.getValue();
		assertEquals(destinationBucket, capturedPutObjectRequest.bucket());
		assertEquals(DOCS_STACK_INSTANCE_JSON_FILE, capturedPutObjectRequest.key());
	}
	
}