package org.sagebionetworks.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.s3.AmazonS3Client;

public class StackConfigurationSetupTest {
	
	AmazonS3Client mockClient;
	InputConfiguration config;
	GeneratedResources resources;
	StackConfigurationSetup setup;
	
	@Before
	public void before() throws IOException{
		config = TestHelper.createTestConfig("dev");
		mockClient = Mockito.mock(AmazonS3Client.class);
		resources = TestHelper.createTestResources(config);
		setup = new StackConfigurationSetup(mockClient, config, resources);
	}
	
	@Test
	public void testCreateConfigProperties() throws IOException{
		// Validate that all of the expected values are set.
		Properties template = InputConfiguration.loadPropertyFile(Constants.FILE_STACK_CONFIG_TEMPLATE);
		Properties results = setup.createConfigProperties();
		assertNotNull(results);
		assertEquals(template.size(), results.size());
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

}
