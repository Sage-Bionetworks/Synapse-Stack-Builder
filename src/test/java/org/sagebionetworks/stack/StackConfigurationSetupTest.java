package org.sagebionetworks.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

public class StackConfigurationSetupTest {
	
	AmazonS3Client mockClient;
	InputConfiguration config;
	GeneratedResources resources;
	StackConfigurationSetup setup;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	
	@Before
	public void before() throws IOException{
		config = TestHelper.createTestConfig("dev");
		mockClient = factory.createS3Client();
		resources = TestHelper.createTestResources(config);
		setup = new StackConfigurationSetup(factory, config, resources);
	}
	
	@Test
	public void testCreateConfigProperties() throws IOException{
		// Validate that all of the expected values are set.
		Properties template = InputConfiguration.loadPropertyFile(Constants.FILE_STACK_CONFIG_TEMPLATE);
		Properties results = setup.createConfigProperties();
		assertNotNull(results);
		assertEquals(template.size()+(resources.getStackInstanceTablesDatabases().size()*2)+1, results.size());
		// Are all of the values set
		for(String key: template.stringPropertyNames()){
			String value = results.getProperty(key);
			assertNotNull("Failed to find expected value for key: "+key, value);
		}
	}
	
	@Test
	public void testSaveUploadDelete() throws IOException{
		File tempFile = File.createTempFile("SomePrefix", ".tmp");
		Properties props = new Properties();
		setup.saveUploadDelete(config.getStackConfigS3BucketName(), props, tempFile);
		verify(mockClient, times(1)).putObject(config.getStackConfigS3BucketName(), config.getStackConfigurationFileS3Path(), tempFile);
		// The temp file should be deleted
		assertFalse(tempFile.exists());
	}
	
	@Test
	public void testSetupAndUploadStackConfig() throws IOException{
		// Make the call.
		setup.setupAndUploadStackConfig();
		verify(mockClient, times(1)).createBucket(config.getStackConfigS3BucketName());
		// The resources should be set
		URL expectedUrl = new URL(config.getStackConfigurationFileURL());
		assertEquals(expectedUrl, resources.getStackConfigurationFileURL());
	}

	@Test
	public void testSetupMainFileBucket() throws IOException{
		Bucket mockBucket = mock(Bucket.class);
		when(mockClient.createBucket(config.getMainFileS3BucketName())).thenReturn(mockBucket);
		setup.setupMainFileBucket();
		verify(mockClient, times(1)).createBucket(config.getMainFileS3BucketName());
		// The resources should be set
		assertNotNull(resources.getMainFileS3Bucket());
		assertEquals(mockBucket, resources.getMainFileS3Bucket());
	}
	
	@Test
	public void testTablesDatabaseStackConfig() throws IOException{
		Properties results = setup.createConfigProperties();
		assertEquals("2", results.get(Constants.KEY_TABLE_CLUSTER_DATABASE_COUNT));
		// one
		assertEquals("tables.endpoint.one", results.get(Constants.KEY_TABLE_CLUSTER_DATABASE_ENDPOINT_PREFIX+0));
		assertEquals("devA", results.get(Constants.KEY_TABLE_CLUSTER_DATABASE_SCHEMA_PREFIX+0));
		// two
		// one
		assertEquals("tables.endpoint.two", results.get(Constants.KEY_TABLE_CLUSTER_DATABASE_ENDPOINT_PREFIX+1));
		assertEquals("devA", results.get(Constants.KEY_TABLE_CLUSTER_DATABASE_SCHEMA_PREFIX+1));
	}
	
}
