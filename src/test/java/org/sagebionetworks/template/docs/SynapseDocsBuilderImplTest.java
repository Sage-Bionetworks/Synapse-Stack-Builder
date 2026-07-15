package org.sagebionetworks.template.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
public class SynapseDocsBuilderImplTest {

	@Mock
	private S3Client mockS3Client;

	@Mock
	private RepoConfiguration mockConfig;

	private ListObjectsV2Request sourceListRequest;
	private ListObjectsV2Request destinationListRequest;

	private String prodInstance;
	private String sourceBucket;
	private String destinationBucket;
	private JSONObject instanceObjectUpToDate;
	private JSONObject instanceObjectOutOfDate;
	private String jsonUpToDate;
	private String jsonOutOfDate;
	private List<S3Object> objects;
	private S3Object object;
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
		object = S3Object.builder().key("objectKey").eTag("etag").build();
		objects = Arrays.asList(object);
		sourceListRequest = ListObjectsV2Request.builder().bucket(sourceBucket).prefix(prefix).build();
		destinationListRequest = ListObjectsV2Request.builder().bucket(destinationBucket).prefix(prefix).build();
		builder = new SynapseDocsBuilderImpl(mockS3Client, mockConfig);
		builderSpy = spy(builder);
	}

	@Test
	public void testDeployDocs() {
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_SOURCE_BUCKET)).thenReturn(sourceBucket);
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_DESTINATION_BUCKET)).thenReturn(destinationBucket);
		doAnswer(invocation -> true).when(builderSpy).verifyDeployment(destinationBucket);
		doAnswer(invocation -> null).when(builderSpy).sync(sourceBucket, destinationBucket);
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
		when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());
		when(mockS3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(
				ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), jsonUpToDate.getBytes(StandardCharsets.UTF_8)));
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		assertFalse(builder.verifyDeployment(destinationBucket));
	}

	@Test
	public void testVerifyDeploymentWithOutOfDateDocs() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(true);
		when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());
		when(mockS3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(
				ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), jsonOutOfDate.getBytes(StandardCharsets.UTF_8)));
		// JSON tracking of instance < prod instance
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		assertTrue(builder.verifyDeployment(destinationBucket));
	}

	@Test
	public void testVerifyDeploymentWithNoInstanceJsonFile() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(true);
		// A HEAD on a missing key has no body, so the SDK reports a generic S3Exception (404),
		// not the typed NoSuchKeyException.
		when(mockS3Client.headObject(any(HeadObjectRequest.class)))
			.thenThrow((S3Exception) S3Exception.builder().statusCode(404).build());
		// call under test
		assertTrue(builder.verifyDeployment(destinationBucket));
	}

	@Test
	public void testVerifyDeploymentWithMissingInstanceJsonFileAsNoSuchKey() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(true);
		// NoSuchKeyException (statusCode 404) is also treated as absent.
		when(mockS3Client.headObject(any(HeadObjectRequest.class)))
			.thenThrow((NoSuchKeyException) NoSuchKeyException.builder().statusCode(404).build());
		// call under test
		assertTrue(builder.verifyDeployment(destinationBucket));
	}

	@Test
	public void testVerifyDeploymentWithHeadObjectAccessDenied() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(true);
		// A non-404 error (e.g. 403 AccessDenied) must not be swallowed as "absent".
		S3Exception accessDenied = (S3Exception) S3Exception.builder().statusCode(403).build();
		when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(accessDenied);
		// call under test
		S3Exception thrown = assertThrows(S3Exception.class, () -> builder.verifyDeployment(destinationBucket));
		assertSame(accessDenied, thrown);
	}

	@Test
	public void testSyncWithDestinationEmpty() throws Exception {
		doAnswer(invocation -> destinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> sourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> new ArrayList<S3Object>())
			.when(builderSpy).getAllS3Objects(destinationListRequest);
		doAnswer(invocation -> objects)
			.when(builderSpy).getAllS3Objects(sourceListRequest);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		verifyCopy();
		verify(mockS3Client, never()).deleteObject(any(DeleteObjectRequest.class));
		verifyInstancePut();
	}

	@Test
	public void testSyncWithDestinationSameKeyWithSameETag() throws Exception {
		doAnswer(invocation -> destinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> sourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(destinationListRequest);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(sourceListRequest);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		verify(mockS3Client, never()).copyObject(any(CopyObjectRequest.class));
		verify(mockS3Client, never()).deleteObject(any(DeleteObjectRequest.class));
		verifyInstancePut();
	}

	@Test
	public void testSyncWithDestinationSameKeyWithDifferentETag() throws Exception {
		S3Object newObject = S3Object.builder().key(object.key()).eTag("different-etag").build();
		List<S3Object> newObjects = Arrays.asList(newObject);
		doAnswer(invocation -> destinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> sourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> newObjects).when(builderSpy).getAllS3Objects(destinationListRequest);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(sourceListRequest);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		verifyCopy();
		verify(mockS3Client, never()).deleteObject(any(DeleteObjectRequest.class));
		verifyInstancePut();
	}

	@Test
	public void testSyncWithDestinationDeleteExistingFile() throws Exception {
		S3Object newObject = S3Object.builder().key("someKeyNotInSource").build();
		List<S3Object> newObjects = Arrays.asList(newObject);
		doAnswer(invocation -> destinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> sourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> newObjects).when(builderSpy).getAllS3Objects(destinationListRequest);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(sourceListRequest);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		verifyCopy();
		ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(mockS3Client).deleteObject(deleteCaptor.capture());
		assertEquals(destinationBucket, deleteCaptor.getValue().bucket());
		assertEquals(newObject.key(), deleteCaptor.getValue().key());
		verifyInstancePut();
	}

	/**
	 * Asserts that a single copy from source to destination for the shared {@link #object} occurred.
	 */
	private void verifyCopy() {
		ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
		verify(mockS3Client).copyObject(copyCaptor.capture());
		CopyObjectRequest request = copyCaptor.getValue();
		assertEquals(sourceBucket, request.sourceBucket());
		assertEquals(object.key(), request.sourceKey());
		assertEquals(destinationBucket, request.destinationBucket());
		assertEquals(object.key(), request.destinationKey());
	}

	/**
	 * Asserts that the instance tracking JSON was written to the destination bucket.
	 */
	private void verifyInstancePut() throws Exception {
		ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
		verify(mockS3Client).putObject(putCaptor.capture(), bodyCaptor.capture());
		assertEquals(destinationBucket, putCaptor.getValue().bucket());
		assertEquals(DOCS_STACK_INSTANCE_JSON_FILE, putCaptor.getValue().key());
		byte[] content = bodyCaptor.getValue().contentStreamProvider().newStream().readAllBytes();
		assertEquals(jsonUpToDate, new String(content, StandardCharsets.UTF_8));
	}

	@Test
	public void testGetAllS3Objects() {
		when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(
				ListObjectsV2Response.builder().contents(objects).isTruncated(false).build());
		// call under test
		List<S3Object> allObjects = builder.getAllS3Objects(sourceListRequest);
		verify(mockS3Client).listObjectsV2(any(ListObjectsV2Request.class));
		assertEquals(objects, allObjects);
	}

	@Test
	public void testGetAllS3ObjectsWithTruncatedList() {
		S3Object nextObject = S3Object.builder().key("nextKey").build();
		ListObjectsV2Response firstPage = ListObjectsV2Response.builder()
				.contents(object).isTruncated(true).nextContinuationToken("token").build();
		ListObjectsV2Response secondPage = ListObjectsV2Response.builder()
				.contents(nextObject).isTruncated(false).build();
		when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(firstPage, secondPage);
		// call under test
		List<S3Object> allObjects = builder.getAllS3Objects(sourceListRequest);
		List<S3Object> expected = Arrays.asList(object, nextObject);
		verify(mockS3Client, org.mockito.Mockito.times(2)).listObjectsV2(any(ListObjectsV2Request.class));
		assertEquals(expected, allObjects);
	}

	@Test
	public void testCreateListObjectsRequest() {
		// call under test
		ListObjectsV2Request request = builder.createListObjectsRequest(sourceBucket, prefix);
		assertEquals(sourceBucket, request.bucket());
		assertEquals(prefix, request.prefix());
	}
}
