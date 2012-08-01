package org.sagebionetworks.stack;

import java.io.IOException;
import java.util.Properties;

import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.util.PropertyFilter;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

/**
 * Builds and uploads the Stack configuration file used by the beanstalk instances.
 * @author John
 *
 */
public class StackConfigurationSetup {
	
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
	 */
	public void setupAndUploadStackConfig(){
		// Fist make sure the bucket exists
		String bucketName = config.getStackConfigS3BucketName();
		// This call is idempotent and will only actually create the bucket if it does not already exist.
		Bucket bucket = client.createBucket(bucketName);
		
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
