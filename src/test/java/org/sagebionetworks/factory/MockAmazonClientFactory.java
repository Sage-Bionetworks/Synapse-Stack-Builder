package org.sagebionetworks.factory;

import org.mockito.Mockito;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;

/**
 * Mock client factory used for testing.
 * 
 * @author John
 *
 */
public class MockAmazonClientFactory implements AmazonClientFactory {
	
	AmazonS3Client mockS3Client;
	AmazonEC2Client mockEC2Client;
	AmazonSNSClient mockSNSClient;
	AmazonRDSClient mockRDSClient;
	AmazonCloudWatchClient mockCloudWatchClient;
	AWSElasticBeanstalkClient mockElasticBeanstalkClient;
	AmazonIdentityManagementClient mockIdentityManagementClient;
	AWSCredentials credentials;
	
	/**
	 * This will setup all mocks.
	 */
	public MockAmazonClientFactory(){
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		mockEC2Client = Mockito.mock(AmazonEC2Client.class);
		mockSNSClient = Mockito.mock(AmazonSNSClient.class);
		mockRDSClient = Mockito.mock(AmazonRDSClient.class);
		mockCloudWatchClient = Mockito.mock(AmazonCloudWatchClient.class);
		mockElasticBeanstalkClient = Mockito.mock(AWSElasticBeanstalkClient.class);
		mockIdentityManagementClient = Mockito.mock(AmazonIdentityManagementClient.class);
	}

	/**
	 * Capture the credentials.
	 */
	public void setCredentials(AWSCredentials credentials) {
		this.credentials = credentials;
	}

	/**
	 * Returns the same mock created in the constructor.
	 */
	public AmazonS3Client createS3Client() {
		return mockS3Client;
	}

	/**
	 * Returns the same mock created in the constructor.
	 */
	public AmazonEC2Client createEC2Client() {
		return mockEC2Client;
	}

	/**
	 * Returns the same mock created in the constructor.
	 */
	public AmazonSNSClient createSNSClient() {
		return mockSNSClient;
	}

	/**
	 * Returns the same mock created in the constructor.
	 */
	public AmazonRDSClient createRDSClient() {
		return mockRDSClient;
	}

	/**
	 * Returns the same mock created in the constructor.
	 */
	public AmazonCloudWatchClient createCloudWatchClient() {
		return mockCloudWatchClient;
	}

	public AWSElasticBeanstalkClient createBeanstalkClient() {
		return mockElasticBeanstalkClient;
	}

	public AmazonIdentityManagementClient createIdentityManagementClient() {
		// TODO Auto-generated method stub
		return null;
	}

}
