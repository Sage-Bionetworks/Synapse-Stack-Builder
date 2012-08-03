package org.sagebionetworks.stack.factory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;

/**
 * Abstraction for creating the various Amazon clients.
 * 
 * Allows for tests to substitution of this factory with Mocks.
 * 
 * @author John
 *
 */
public interface AmazonClientFactory {
	
	/**
	 * Set the factory credentials.
	 * 
	 * @param credentials
	 */
	public void setCredentials(AWSCredentials credentials);
	
	/**
	 * Create a AmazonS3Client.
	 * @return
	 */
	public AmazonS3Client createS3Client();
	
	/**
	 * Create a AmazonEC2Client.
	 * @return
	 */
	public AmazonEC2Client createEC2Client();
	
	/**
	 * Create a AmazonSNSClient.
	 * @return
	 */
	public AmazonSNSClient createSNSClient();
	
	/**
	 * Create a AmazonRDSClient.
	 * @return
	 */
	public AmazonRDSClient createRDSClient();
	
	/**
	 * Create the AmazonCloudWatchClient.
	 * @return
	 */
	public AmazonCloudWatchClient createCloudWatchClient();

}
