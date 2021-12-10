package org.sagebionetworks.template.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.s3.S3TransferManager;
import org.sagebionetworks.template.s3.S3TransferManagerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Copy;

@ExtendWith(MockitoExtension.class)
public class SynapseDocsBuilderImplTest {
	
	@Mock
	private S3TransferManagerFactory mockS3TransferManagerFactory;
	
	@Mock
	private S3TransferManager mockS3TransferManager;
	
	@Mock
	private AmazonS3 mockS3Client;
	
	@Mock
	private RepoConfiguration mockConfig;
	
	@Mock
	private ObjectListing mockSourceListing;

	@Mock
	private ObjectListing mockDestinationListing;
	
	@Mock
	private ListObjectsRequest mockSourceListRequest;
	
	@Mock
	private ListObjectsRequest mockDestinationListRequest;
	
	@Mock
	private Copy mockCopy;
	
	private String prodInstance;
	private String sourceBucket;
	private String destinationBucket;
	private JSONObject instanceObjectOutOfDate;
	private JSONObject instanceObjectUpToDate;
	private String jsonOutOfDate;
	private String jsonUpToDate;
	private List<S3ObjectSummary> objects;
	private S3ObjectSummary object;
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
		object = new S3ObjectSummary();
		object.setKey("objectKey");
		object.setETag("etag");
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
		when(mockS3Client.doesObjectExist(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(true);
		when(mockS3Client.getObjectAsString(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(jsonUpToDate);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		assertFalse(builder.verifyDeployment(destinationBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithOutOfDateDocs() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(true);
		when(mockS3Client.doesObjectExist(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(true);
		when(mockS3Client.getObjectAsString(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(jsonOutOfDate);
		// JSON tracking of instance < prod instance
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		assertTrue(builder.verifyDeployment(destinationBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithNoInstanceJsonFile() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)).thenReturn(true);
		when(mockS3Client.doesObjectExist(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(false);
		// call under test
		assertTrue(builder.verifyDeployment(destinationBucket));
	}
	
	@Test
	public void testSyncWithDestinationEmpty() throws Exception {
		doAnswer(invocation -> mockDestinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> mockSourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> new ArrayList<S3ObjectSummary>())
			.when(builderSpy).getAllS3Objects(mockDestinationListRequest);
		doAnswer(invocation -> objects)
			.when(builderSpy).getAllS3Objects(mockSourceListRequest);
		when(mockS3TransferManagerFactory.createNewS3TransferManager()).thenReturn(mockS3TransferManager);
		when(mockS3TransferManager.copy(any(), any(), any(), any())).thenReturn(mockCopy);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		verify(mockS3TransferManager).close();
		verify(mockS3TransferManager).copy(sourceBucket, object.getKey(), destinationBucket, object.getKey());
		verify(mockS3Client, never()).deleteObject(any(), any());
		verify(mockS3Client).putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, jsonUpToDate);
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
		verify(mockS3TransferManager, never()).copy(any(), any(), any(), any());
		verify(mockS3Client, never()).deleteObject(any(), any());
		verify(mockS3Client).putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, jsonUpToDate);
	}
	
	@Test
	public void testSyncWithDestinationSameKeyWithDifferentETag() throws Exception {
		S3ObjectSummary newObject = new S3ObjectSummary();
		newObject.setETag("different-etag");
		newObject.setKey(object.getKey());
		List<S3ObjectSummary> newObjects = Arrays.asList(newObject);
		doAnswer(invocation -> mockDestinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> mockSourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> newObjects).when(builderSpy).getAllS3Objects(mockDestinationListRequest);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(mockSourceListRequest);
		when(mockS3TransferManagerFactory.createNewS3TransferManager()).thenReturn(mockS3TransferManager);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		when(mockS3TransferManager.copy(any(), any(), any(), any())).thenReturn(mockCopy);
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		verify(mockS3TransferManager).close();
		verify(mockS3TransferManager).copy(sourceBucket, object.getKey(), destinationBucket, object.getKey());
		verify(mockS3Client, never()).deleteObject(any(), any());
		verify(mockS3Client).putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, jsonUpToDate);
	}
	
	@Test
	public void testSyncWithDestinationDeleteExistingFile() throws Exception {
		S3ObjectSummary newObject = new S3ObjectSummary();
		newObject.setKey("someKeyNotInSource");
		List<S3ObjectSummary> newObjects = Arrays.asList(newObject);
		doAnswer(invocation -> mockDestinationListRequest)
			.when(builderSpy).createListObjectsRequest(destinationBucket, prefix);
		doAnswer(invocation -> mockSourceListRequest)
			.when(builderSpy).createListObjectsRequest(sourceBucket, prefix);
		doAnswer(invocation -> newObjects).when(builderSpy).getAllS3Objects(mockDestinationListRequest);
		doAnswer(invocation -> objects).when(builderSpy).getAllS3Objects(mockSourceListRequest);
		when(mockS3TransferManagerFactory.createNewS3TransferManager()).thenReturn(mockS3TransferManager);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		when(mockS3TransferManager.copy(any(), any(), any(), any())).thenReturn(mockCopy);
		// call under test
		builderSpy.sync(sourceBucket, destinationBucket);
		verify(mockS3TransferManager).close();
		verify(mockS3TransferManager).copy(sourceBucket, object.getKey(), destinationBucket, object.getKey());
		verify(mockS3Client).deleteObject(destinationBucket, newObject.getKey());
		verify(mockS3Client).putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, jsonUpToDate);
	}
	
	@Test
	public void testGetAllS3Objects() {
		when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(mockSourceListing);
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects);
		when(mockSourceListing.isTruncated()).thenReturn(false);
		// call under test
		List<S3ObjectSummary> allObjects = builder.getAllS3Objects(mockSourceListRequest);
		verify(mockSourceListRequest).setMarker(mockSourceListing.getNextMarker());
		assertEquals(allObjects, objects);
	}
	
	@Test
	public void testGetAllS3ObjectsWithTruncatedList() {
		S3ObjectSummary nextObject = new S3ObjectSummary();
		List<S3ObjectSummary> nextPageObjects = Arrays.asList(nextObject);
		when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(mockSourceListing);
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects,nextPageObjects);
		when(mockSourceListing.isTruncated()).thenReturn(true, false);
		// call under test
		List<S3ObjectSummary> allObjects = builder.getAllS3Objects(mockSourceListRequest);
		List<S3ObjectSummary> expected = Arrays.asList(object, nextObject);
		verify(mockSourceListRequest, times(2)).setMarker(mockSourceListing.getNextMarker());
		assertEquals(allObjects, expected);
	}
	
	@Test
	public void testCreateListObjectsRequest() {
		// call under test
		ListObjectsRequest request = builder.createListObjectsRequest(sourceBucket, prefix);
		assertEquals(request.getBucketName(), sourceBucket);
		assertEquals(request.getPrefix(), prefix);
	}
}
