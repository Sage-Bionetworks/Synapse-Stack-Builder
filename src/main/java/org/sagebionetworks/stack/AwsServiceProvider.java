package org.sagebionetworks.stack;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Provides the various AWS clients.
 * @author John
 *
 */
public class AwsServiceProvider {
	
	/**
	 * Credentials used for the various clients
	 */
	AWSCredentials awsCredentials;
	
	/**
	 * 
	 * @param credentials
	 */
	public AwsServiceProvider(AWSCredentials credentials){
		this.awsCredentials = credentials;
	}
	
	/**
	 * The S3 Client
	 * @return
	 */
	public AmazonS3Client createS3Client(){
		return new AmazonS3Client(awsCredentials);
	}
}
