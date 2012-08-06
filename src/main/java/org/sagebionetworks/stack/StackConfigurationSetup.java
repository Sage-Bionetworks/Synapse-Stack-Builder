package org.sagebionetworks.stack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.util.PropertyFilter;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * Builds and uploads the Stack configuration file used by the beanstalk instances.
 * @author John
 *
 */
public class StackConfigurationSetup {
	
	private static Logger log = Logger.getLogger(StackConfigurationSetup.class.getName());
	
	private AmazonS3Client client;
	private InputConfiguration config;
	private GeneratedResources resources;
	
	/**
	 * The IoC constructor.
	 * @param client
	 * @param config
	 * @param resources
	 */
	public StackConfigurationSetup(AmazonS3Client client, InputConfiguration config, GeneratedResources resources) {
		if(client == null) throw new IllegalArgumentException("AmazonS3Client cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		if(resources.getIdGeneratorDatabase() == null) throw new IllegalArgumentException("GeneratedResources.getIdGeneratorDatabase() cannot be null");
		if(resources.getIdGeneratorDatabase().getEndpoint() == null) throw new IllegalArgumentException("GeneratedResources.getIdGeneratorDatabase().getEndpoint() cannot be null");
		if(resources.getStackInstancesDatabase() == null) throw new IllegalArgumentException("GeneratedResources.getStackInstancesDatabase() cannot be null");
		if(resources.getStackInstancesDatabase().getEndpoint() == null) throw new IllegalArgumentException("GeneratedResources.getStackInstancesDatabase().getEndpoint() cannot be null");
		this.client = client;
		this.config = config;
		this.resources = resources;
	}
	
	/**
	 * Builds and uploads the Stack configuration file used by the beanstalk instances.
	 * @throws IOException 
	 */
	public void setupAndUploadStackConfig() throws IOException{
		// Fist make sure the bucket exists
		String bucketName = config.getStackConfigS3BucketName();
		log.info("Creating S3 Bucket: "+bucketName);
		// This call is idempotent and will only actually create the bucket if it does not already exist.
		Bucket bucket = client.createBucket(bucketName);
		// This is the final property file that will be uploaded to S3.
		Properties props = createConfigProperties();
		// Write to a temp file that will get deleted.
		File temp = File.createTempFile("TempProps", ".properties");
		saveUploadDelete(bucketName, props, temp);
		
	}

	/**
	 * Save the properties, upload the file then delete the temp file.
	 * @param bucketName
	 * @param props
	 * @param temp
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	void saveUploadDelete(String bucketName, Properties props, File temp)throws IOException, MalformedURLException {
		FileWriter writer = new FileWriter(temp);
		try{
			// Write it to file.
			props.store(writer, "This file was auto-generated and should NOT be directly modified");
			URL configUrl = new URL(config.getStackConfigurationFileURL());
			log.debug("Uploading file: "+configUrl);
			// Now upload the file to S3
			PutObjectResult result = client.putObject(bucketName, config.getStackConfigurationFileS3Path(), temp);
			resources.setStackConfigurationFileURL(configUrl);
		}finally{
			writer.close();
			// Delete the temp file.
			temp.delete();
		}
	}
	
	/**
	 * Create the configuration properties using everything gather to this point.
	 * @return
	 * @throws IOException
	 */
	Properties createConfigProperties() throws IOException{
		// First load the template
		Properties template = InputConfiguration.loadPropertyFile(Constants.FILE_STACK_CONFIG_TEMPLATE);
		// Create the union of the template and all configuration properties
		Properties union = config.createUnionOfInputAndConfig(template);
		// Add the required properties from the loaded resources
		// Capture the id gen DB end point.
		union.put(Constants.KEY_ID_GENERATOR_DB_ADDRESS, resources.getIdGeneratorDatabase().getEndpoint().getAddress());
		// Capture the stack instance DB end point.
		union.put(Constants.KEY_STACK_INSTANCE_DB_ADDRESS, resources.getStackInstancesDatabase().getEndpoint().getAddress());
		// Apply the filter to replace all regular expression with the final values
		PropertyFilter.replaceAllRegularExp(union);
		// The final step is to copy over the values defined in the template
		for (Object keyOb : template.keySet()) {
			String key = (String) keyOb;
			// Set the values
			String value = union.getProperty(key);
			if(value == null) throw new IllegalArgumentException("Failed to find a final value for the property key: "+key);
			template.setProperty(key, value);
		}
		return template;
	}
	
	
}
