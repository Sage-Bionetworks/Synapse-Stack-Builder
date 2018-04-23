package org.sagebionetworks.stack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;

/**
 * These are default properties that are shared across all instances of a stack.
 * The canonical values of these properties are in S3.
 * 
 * @author jmhill
 *
 */
public class StackDefaults {
	
	private static Logger log = LogManager.getLogger(StackDefaults.class.getName());
	
	/**
	 * The property keys we expect to find the default properties file.
	 */
	public static final String[] EXPECTED_PROPERTIES = new String[]{
		Constants.STACK_ENCRYPTION_KEY,
		Constants.KEY_DEFAULT_ID_GEN_PASSWORD_PLAIN_TEXT,
		Constants.KEY_CIDR_FOR_SSH,
	};
	
	AmazonS3Client s3Client;
	InputConfiguration config;
	
	
	/**
	 * The IoC constructor.
	 * @param s3Client
	 * @param config
	 */
	public StackDefaults(AmazonS3Client s3Client, InputConfiguration config) {
		if(config == null) throw new IllegalArgumentException("Configuration cannot be null");
		if(s3Client == null) throw new IllegalArgumentException("The AmazonS3Client cannot be null");
		this.s3Client = s3Client;
		this.config = config;
	}

	/**
	 * Connect to S3 and downloads the default properties for this stack.
	 * 
	 * @param stack
	 * @param mockS3Client
	 * @return
	 * @throws IOException 
	 */
	public Properties loadStackDefaultsFromS3() throws IOException{
		
		// Create the config bucket.
		String bucketName = config.getStackConfigS3BucketName();
		log.info("Creating S3 Bucket: "+bucketName);
		// This call is idempotent and will only actually create the bucket if it does not already exist.
		Bucket bucket = s3Client.createBucket(bucketName);
		
		// This is the buck where we expect to find the properties.
		bucketName = config.getDefaultS3BucketName();

		log.info("Creating S3 Bucket: "+bucketName);
		// This call is idempotent and will only actually create the bucket if it does not already exist.
		bucket = s3Client.createBucket(bucketName);
		String fileName = config.getDefaultPropertiesFileName();
		File temp = File.createTempFile("DefaultProps", ".properties");
		FileInputStream in = new FileInputStream(temp);
		try{
			// Download the file to a temp file.
			s3Client.getObject(new GetObjectRequest(bucketName, fileName), temp);
			Properties props = new Properties();
			props.load(in);
			// Did we get the expected properties?
			validateProperties(bucketName, fileName, props);
			// Done
			return props;
		}catch (IOException e){
			log.error("Failed to read the '"+fileName+"' downloaded from  S3 bucket: '"+bucketName+"'.  Expected the file to be a java.util.Properties file");
			throw e;
		}catch (AmazonClientException e){
			log.error("Failed to dowload the '"+fileName+"' from S3 bucket: '"+bucketName+"' make sure the file exists and try again.");
			throw e;
		}finally{
			in.close();
			// Delete the temp file
			temp.delete();
		}
	}

	/**
	 * Validate the properties.
	 * @param bucketName
	 * @param fileName
	 * @param props
	 */
	static void validateProperties(String bucketName, String fileName,	Properties props) {
		// Validate the properties
		for(String expectedKey: EXPECTED_PROPERTIES){
			String value = props.getProperty(expectedKey);
			if(value == null || value.length()<1) throw new IllegalArgumentException("The '"+fileName+"' file downloaed form S3 bucket: '"+bucketName+"' did not contain the expected property: '"+expectedKey+"'.");
		}
	}

}
