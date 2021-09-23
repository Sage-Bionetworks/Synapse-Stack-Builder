package org.sagebionetworks.template.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;

@ExtendWith(MockitoExtension.class)
public class SynapseDocsBuilderImplTest {
	
	@Mock
	TransferManager mockTransferManager;
	
	@Mock
	AmazonS3 mockS3Client;
	
	@Mock
	RepoConfiguration mockConfig;
	
	@Mock
	ObjectListing mockSourceListing;
	
	@Mock
	ObjectListing mockDestinationListing;
	
	String prodInstance;
	String oldInstance;
	String sourceBucket;
	String destinationBucket;
	JSONObject instanceObjectOutOfDate;
	JSONObject instanceObjectUpToDate;
	String jsonOutOfDate;
	String jsonUpToDate;
	List<S3ObjectSummary> objects;
	S3ObjectSummary object;
	String prefix;
	
	@InjectMocks
	SynapseDocsBuilderImpl builder;
	
	SynapseDocsBuilderImpl builderSpy;
	
	@BeforeEach
	public void before() {
		prefix = "";
		prodInstance = "2";
		oldInstance = "1";
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
		builder = new SynapseDocsBuilderImpl(mockS3Client, mockConfig, mockTransferManager);
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
	public void testDeployDocsWithNoDeployment() {
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_SOURCE_BUCKET)).thenReturn(sourceBucket);
		when(mockConfig.getProperty(PROPERTY_KEY_DOCS_DESTINATION_BUCKET)).thenReturn(destinationBucket);
		doAnswer(invocation -> false).when(builderSpy).verifyDeployment(destinationBucket);
		builderSpy.deployDocs();
		verify(builderSpy).verifyDeployment(destinationBucket);
		verify(builderSpy, never()).sync(sourceBucket, destinationBucket);
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
	public void testSyncWithDestinationEmpty() {
		when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(mockDestinationListing, mockSourceListing);
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		builder.sync(sourceBucket, destinationBucket);
		verify(mockTransferManager).copy(sourceBucket, object.getKey(), destinationBucket, object.getKey());
		verify(mockS3Client, never()).deleteObject(any(), any());
		verify(mockS3Client).putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, jsonUpToDate);
	}
	
	@Test
	public void testSyncWithDestinationSameKeyWithSameETag() {
		when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(mockDestinationListing, mockSourceListing);
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		when(mockDestinationListing.getObjectSummaries()).thenReturn(objects);
		// call under test
		builder.sync(sourceBucket, destinationBucket);
		verify(mockTransferManager, never()).copy(sourceBucket, object.getKey(), destinationBucket, object.getKey());
		verify(mockS3Client, never()).deleteObject(any(), any());
		verify(mockS3Client).putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, jsonUpToDate);
	}
	
	@Test
	public void testSyncWithDestinationSameKeyWithDifferentETag() {
		S3ObjectSummary newObject = new S3ObjectSummary();
		newObject.setETag("different-etag");
		newObject.setKey(object.getKey());
		List<S3ObjectSummary> newObjects = Arrays.asList(newObject);
		when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(mockDestinationListing, mockSourceListing);
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		when(mockDestinationListing.getObjectSummaries()).thenReturn(newObjects);
		// call under test
		builder.sync(sourceBucket, destinationBucket);
		verify(mockTransferManager).copy(sourceBucket, object.getKey(), destinationBucket, object.getKey());
		verify(mockS3Client, never()).deleteObject(any(), any());
		verify(mockS3Client).putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, jsonUpToDate);
	}
	
	@Test
	public void testSyncWithDestinationDeleteExistingFile() {
		S3ObjectSummary newObject = new S3ObjectSummary();
		newObject.setKey("someKeyNotInSource");
		List<S3ObjectSummary> newObjects = Arrays.asList(newObject);
		when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(mockDestinationListing, mockSourceListing);
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		when(mockDestinationListing.getObjectSummaries()).thenReturn(newObjects);
		// call under test
		builder.sync(sourceBucket, destinationBucket);
		verify(mockTransferManager).copy(sourceBucket, object.getKey(), destinationBucket, object.getKey());
		verify(mockS3Client).deleteObject(destinationBucket, newObject.getKey());
		verify(mockS3Client).putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, jsonUpToDate);
	}
}
