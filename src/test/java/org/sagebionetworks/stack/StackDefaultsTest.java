package org.sagebionetworks.stack;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

/**
 * Unit test for the StackDefaults class
 * @author jmhill
 *
 */
public class StackDefaultsTest {
	
	InputConfiguration config;
	AmazonS3Client mockClient;
	StackDefaults defaults;
	@Before
	public void before() throws IOException{
		config = TestHelper.createTestConfig("dev");
		mockClient = Mockito.mock(AmazonS3Client.class);
		defaults = new StackDefaults(mockClient, config);
	}

	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePropertiesEmpty(){
		Properties props = new Properties();
		StackDefaults.validateProperties("bucket", "file", props);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePropertiesMissing(){
		Properties props = new Properties();
		props.put("some.key", "some.value");
		StackDefaults.validateProperties("bucket", "file", props);
	}
	
	@Test 
	public void testValidateProperties(){
		Properties props = new Properties();
		int i=0;
		for(String key: StackDefaults.EXPECTED_PROPERTIES){
			props.put(key, "value"+i);
			i++;
		}
		StackDefaults.validateProperties("bucket", "file", props);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testLoadStackDefaultsFromS3() throws IOException{
		String bucketString = config.getDefaultS3BucketName();
		
		Bucket bucket = new Bucket(bucketString);
		when(mockClient.createBucket(bucketString)).thenReturn(new Bucket());
		// This should fail since the expected properties are missing.
		defaults.loadStackDefaultsFromS3();
	}
}
