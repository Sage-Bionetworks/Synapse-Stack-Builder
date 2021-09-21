package org.sagebionetworks.template.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.json.JSONObject;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROD_STACK_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DEV_RELEASE_DOCS_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REST_DOCS_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOC_DEPLOYMENT_FLAG;
import static org.sagebionetworks.template.Constants.DOCS_STACK_INSTANCE_JSON_FILE;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.services.s3.AmazonS3;
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
	String devBucket;
	String docsBucket;
	JSONObject instanceObjectOutOfDate;
	JSONObject instanceObjectUpToDate;
	String jsonOutOfDate;
	String jsonUpToDate;
	List<S3ObjectSummary> objects;
	S3ObjectSummary object;
	
	@InjectMocks
	SynapseDocsBuilderImpl builder;
	
	@BeforeEach
	public void before() {
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
		devBucket = "devBucket";
		docsBucket = "docsBucket";
		object = new S3ObjectSummary();
		object.setKey("objectKey");
		objects = Arrays.asList(object);
		builder = new SynapseDocsBuilderImpl(mockS3Client, mockConfig, mockTransferManager);
	}
	
	@AfterEach
	public void after() {
		return;
	}
	
	@Test
	public void testDeployDocs() {
		when(mockConfig.getProperty(PROPERTY_KEY_DEV_RELEASE_DOCS_BUCKET)).thenReturn(devBucket);
		when(mockConfig.getProperty(PROPERTY_KEY_REST_DOCS_BUCKET)).thenReturn(docsBucket);
		// return true on verify deployment
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOC_DEPLOYMENT_FLAG)).thenReturn(true);
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(PROD_STACK_NAME);
		when(mockS3Client.doesObjectExist(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(true);
		when(mockS3Client.getObjectAsString(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(jsonOutOfDate);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// return true on successful sync
		String prefix = "";
		when(mockS3Client.listObjects(devBucket, prefix)).thenReturn(mockSourceListing);
		when(mockS3Client.listObjects(docsBucket, prefix)).thenReturn(new ObjectListing());
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects);
		assertTrue(builder.deployDocs());
	}
	
	@Test
	public void testDeployDocsWithNoDeployment() {
		when(mockConfig.getProperty(PROPERTY_KEY_DEV_RELEASE_DOCS_BUCKET)).thenReturn(devBucket);
		when(mockConfig.getProperty(PROPERTY_KEY_REST_DOCS_BUCKET)).thenReturn(docsBucket);
		// verify deployment returns false
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOC_DEPLOYMENT_FLAG)).thenReturn(false);
		// call under test
		assertFalse(builder.deployDocs());
	}
	
	@Test
	public void testVerifyDeploymentWithFalseFlag() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOC_DEPLOYMENT_FLAG)).thenReturn(false);
		// call under test
		assertFalse(builder.verifyDeployment(docsBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithMissingDeploymentFlag() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOC_DEPLOYMENT_FLAG))
			.thenThrow(ConfigurationPropertyNotFound.class);
		// call under test
		assertFalse(builder.verifyDeployment(docsBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithNonProd() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOC_DEPLOYMENT_FLAG)).thenReturn(true);
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		// call under test
		assertFalse(builder.verifyDeployment(docsBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithUpToDateDocs() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOC_DEPLOYMENT_FLAG)).thenReturn(true);
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(PROD_STACK_NAME);
		when(mockS3Client.doesObjectExist(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(true);
		when(mockS3Client.getObjectAsString(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(jsonUpToDate);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		assertFalse(builder.verifyDeployment(docsBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithOutOfDateDocs() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOC_DEPLOYMENT_FLAG)).thenReturn(true);
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(PROD_STACK_NAME);
		when(mockS3Client.doesObjectExist(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(true);
		when(mockS3Client.getObjectAsString(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(jsonOutOfDate);
		// json tracking of instance < prod instance
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		assertTrue(builder.verifyDeployment(docsBucket));
	}
	
	@Test
	public void testVerifyDeploymentWithNoInstanceJsonFile() {
		when(mockConfig.getBooleanProperty(PROPERTY_KEY_DOC_DEPLOYMENT_FLAG)).thenReturn(true);
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(PROD_STACK_NAME);
		when(mockS3Client.doesObjectExist(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE)).thenReturn(false);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(prodInstance);
		// call under test
		assertTrue(builder.verifyDeployment(docsBucket));
		verify(mockS3Client).putObject(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE, jsonUpToDate);
	}
	
	@Test
	public void testSyncWithDestinationEmpty() {
		String prefix = "";
		when(mockS3Client.listObjects(devBucket, prefix)).thenReturn(mockSourceListing);
		when(mockS3Client.listObjects(docsBucket, prefix)).thenReturn(new ObjectListing());
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects);
		// call under test
		assertTrue(builder.sync(devBucket, docsBucket));
		verify(mockTransferManager).copy(devBucket, object.getKey(), docsBucket, object.getKey());
		verify(mockS3Client, never()).deleteObject(any(), any());
	}
	
	@Test
	public void testSyncWithDestinationOverwriteExistingFile() {
		String prefix = "";
		when(mockS3Client.listObjects(devBucket, prefix)).thenReturn(mockSourceListing);
		when(mockS3Client.listObjects(docsBucket, prefix)).thenReturn(mockDestinationListing);
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects);
		when(mockDestinationListing.getObjectSummaries()).thenReturn(objects);
		// call under test
		assertTrue(builder.sync(devBucket, docsBucket));
		verify(mockTransferManager).copy(devBucket, object.getKey(), docsBucket, object.getKey());
		verify(mockS3Client, never()).deleteObject(any(), any());
	}
	
	@Test
	public void testSyncWithDestinationDeleteExistingFile() {
		String prefix = "";
		when(mockS3Client.listObjects(devBucket, prefix)).thenReturn(mockSourceListing);
		when(mockS3Client.listObjects(docsBucket, prefix)).thenReturn(mockDestinationListing);
		when(mockSourceListing.getObjectSummaries()).thenReturn(objects);
		List<S3ObjectSummary> differentObjects = new ArrayList<>();
		S3ObjectSummary otherObject = new S3ObjectSummary();
		otherObject.setKey("otherObject");
		differentObjects.add(otherObject);
		when(mockDestinationListing.getObjectSummaries()).thenReturn(differentObjects);
		// call under test
		assertTrue(builder.sync(devBucket, docsBucket));
		verify(mockTransferManager).copy(devBucket, object.getKey(), docsBucket, object.getKey());
		verify(mockS3Client).deleteObject(docsBucket, otherObject.getKey());
	}
}
