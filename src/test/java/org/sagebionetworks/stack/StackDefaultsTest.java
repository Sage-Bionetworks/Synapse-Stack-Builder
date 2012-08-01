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
	
	@Before
	public void before() throws IOException{
		config = InputConfigHelper.createTestConfig("dev");
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
		AmazonS3Client mockClient = Mockito.mock(AmazonS3Client.class);
		Bucket bucket = new Bucket(bucketString);
		when(mockClient.createBucket(bucketString)).thenReturn(new Bucket());
		// This should fail since the expected properties are missing.
		StackDefaults.loadStackDefaultsFromS3(config, mockClient);
	}
}
