package org.sagebionetworks.template.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.json.JSONObject;

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
import org.sagebionetworks.template.s3.S3TransferManagerFactory;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;

@ExtendWith(MockitoExtension.class)
public class SynapseDocsBuilderImplTest {
	
	@Mock
	private S3TransferManagerFactory mockS3TransferManagerFactory;
	
	@Mock
	private S3TransferManager mockS3TransferManager;
	
	@Mock
	private S3Client mockS3Client;
	
	@Mock
	private RepoConfiguration mockConfig;
	
	private ListObjectsV2Response mockSourceListing;

	private ListObjectsV2Response mockDestinationListing;
	
	@Mock
	private ListObjectsV2Request mockSourceListRequest;
	
	@Mock
	private ListObjectsV2Request mockDestinationListRequest;
	
	@Mock
	private Copy mockCopy;
	
	@Mock
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
		objectKey = "objectKey";
		object = S3Object.builder()
				.key(objectKey)
				.eTag("etag")
				.build();
		objects = Arrays.asList(object);
		builder = new SynapseDocsBuilderImpl(mockS3Client, mockConfig, mockS3TransferManagerFactory);
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
		doAnswer(invocation -> mockDestinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> mockSourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> new ArrayList<S3Object>())
			.when(builderSpy).getAllS3Objects(mockDestinationListRequest);
		doAnswer(invocation -> objects)
			.when(builderSpy).getAllS3Objects(mockSourceListRequest);
		when(mockS3TransferManagerFactory.createNewS3TransferManager()).thenReturn(mockS3TransferManager);
		
		// Mock the Copy and CompletedCopy
		when(mockCopy.completionFuture()).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockCompletedCopy));
		ArgumentCaptor<CopyRequest> copyRequestCaptor = ArgumentCaptor.forClass(CopyRequest.class);
		when(mockS3TransferManager.copy(copyRequestCaptor.capture())).thenReturn(mockCopy);
		
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		
		verify(mockS3TransferManager).close();
		verify(mockS3TransferManager).copy(any(CopyRequest.class));
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
		doAnswer(invocation -> mockDestinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> mockSourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(mockDestinationListRequest);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(mockSourceListRequest);
		when(mockS3TransferManagerFactory.createNewS3TransferManager()).thenReturn(mockS3TransferManager);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		
		verify(mockS3TransferManager).close();
		verify(mockS3TransferManager, never()).copy(any(CopyRequest.class));
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
		S3Object newObject = S3Object.builder()
				.key(objectKey)
				.eTag("different-etag")
				.build();
		List<S3Object> newObjects = Arrays.asList(newObject);
		doAnswer(invocation -> mockDestinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> mockSourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> newObjects).when(builderSpy).getAllS3Objects(mockDestinationListRequest);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(mockSourceListRequest);
		when(mockS3TransferManagerFactory.createNewS3TransferManager()).thenReturn(mockS3TransferManager);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		
		// Mock the Copy and CompletedCopy
		when(mockCopy.completionFuture()).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockCompletedCopy));
		ArgumentCaptor<CopyRequest> copyRequestCaptor = ArgumentCaptor.forClass(CopyRequest.class);
		when(mockS3TransferManager.copy(copyRequestCaptor.capture())).thenReturn(mockCopy);
		
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		
		verify(mockS3TransferManager).close();
		verify(mockS3TransferManager).copy(any(CopyRequest.class));
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
		S3Object newObject = S3Object.builder().key("someKeyNotInSource").build();
		List<S3Object> newObjects = Arrays.asList(newObject);
		doAnswer(invocation -> mockDestinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> mockSourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> newObjects).when(builderSpy).getAllS3Objects(mockDestinationListRequest);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(mockSourceListRequest);
		when(mockS3TransferManagerFactory.createNewS3TransferManager()).thenReturn(mockS3TransferManager);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		
		// Mock the Copy and CompletedCopy
		when(mockCopy.completionFuture()).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockCompletedCopy));
		ArgumentCaptor<CopyRequest> copyRequestCaptor = ArgumentCaptor.forClass(CopyRequest.class);
		when(mockS3TransferManager.copy(copyRequestCaptor.capture())).thenReturn(mockCopy);
		
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		
		verify(mockS3TransferManager).close();
		verify(mockS3TransferManager).copy(any(CopyRequest.class));
		
		// Verify deleteObject is called with the correct parameters
		ArgumentCaptor<DeleteObjectRequest> deleteObjectRequestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(mockS3Client).deleteObject(deleteObjectRequestCaptor.capture());
		
		DeleteObjectRequest capturedDeleteObjectRequest = deleteObjectRequestCaptor.getValue();
		assertEquals(destinationBucket, capturedDeleteObjectRequest.bucket());
		assertEquals(newObject.key(), capturedDeleteObjectRequest.key());
		
		// Verify putObject is called with the correct parameters
		ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
		verify(mockS3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());
		
		PutObjectRequest capturedPutObjectRequest = putObjectRequestCaptor.getValue();
		assertEquals(destinationBucket, capturedPutObjectRequest.bucket());
		assertEquals(DOCS_STACK_INSTANCE_JSON_FILE, capturedPutObjectRequest.key());
	}
	
	@Test
	public void testGetAllS3Objects() {
		S3Object object1 = S3Object.builder()
				.key("key1")
				.eTag("etag1")
				.build();
		S3Object object2 = S3Object.builder()
				.key("key2")
				.eTag("etag2")
				.build();

		List<S3Object> mockObjects = Arrays.asList(object1, object2);

		ListObjectsV2Iterable mockPaginator = mock(ListObjectsV2Iterable.class);

		when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
				.thenReturn(mockPaginator);
		SdkIterable<S3Object> mockIterable = () -> mockObjects.stream().iterator();
		when(mockPaginator.contents()).thenReturn(mockIterable);
		
		ListObjectsV2Request mockRequest = mock(ListObjectsV2Request.class);

		// call under test
		List<S3Object> allObjects = builder.getAllS3Objects(mockRequest);

		verify(mockS3Client).listObjectsV2Paginator(any(ListObjectsV2Request.class));

		assertEquals(mockObjects, allObjects);
	}
	
	@Test
	public void testCreateListObjectsV2Request() {
		// call under test
		ListObjectsV2Request request = builder.createListObjectsRequest(sourceBucket, prefix);
		assertEquals(sourceBucket, request.bucket());
		assertEquals(prefix, request.prefix());
	}
}