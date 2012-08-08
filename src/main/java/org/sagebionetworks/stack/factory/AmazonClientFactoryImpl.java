package org.sagebionetworks.stack.factory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;

/**
 * Simple implementation of the AmazonClientFactory
 * 
 * @author John
 *
 */
public class AmazonClientFactoryImpl implements AmazonClientFactory {
	
	AWSCredentials credentials;

	public void setCredentials(AWSCredentials credentials) {
		this.credentials = credentials;
	}

	public AmazonS3Client createS3Client() {
		return new AmazonS3Client(credentials);
	}

	public AmazonEC2Client createEC2Client() {
		return new AmazonEC2Client(credentials);
	}

	public AmazonSNSClient createSNSClient() {
		return new AmazonSNSClient(credentials);
	}

	public AmazonRDSClient createRDSClient() {
		return new AmazonRDSClient(credentials);
	}

	public AmazonCloudWatchClient createCloudWatchClient() {
		return new AmazonCloudWatchClient(credentials);
	}

	public AWSElasticBeanstalkClient createBeanstalkClient() {
		return new AWSElasticBeanstalkClient(credentials);
	}

	public AmazonIdentityManagementClient createIdentityManagementClient() {
		return new AmazonIdentityManagementClient(credentials);
	}

	public AmazonCloudSearchClient createCloudSearchClient() {
		return new AmazonCloudSearchClient(credentials);
	}

}
