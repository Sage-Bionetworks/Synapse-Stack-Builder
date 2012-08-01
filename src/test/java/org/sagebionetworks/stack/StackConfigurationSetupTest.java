package org.sagebionetworks.stack;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
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
		Properties results = setup.createConfigProperties();
		assertNotNull(results);
//		File temp = File.createTempFile("Sample", ".properties");
//		results.store(new FileWriter(temp), "Auto-generated");
//		System.out.println(temp.getAbsolutePath());
		for (Object keyOb : results.keySet()) {
			String key = (String) keyOb;
			// Set the values
			String value = results.getProperty(key);
			System.out.println(key+"="+value);
		}
	}

}
