package org.sagebionetworks.factory;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.certificatemanager.AWSCertificateManagerClient;
import com.amazonaws.services.cloudsearchv2.AmazonCloudSearchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;

/**
 * Mock client factory used for testing.
 * 
 * @author John
 *
 */
public class MockAmazonClientFactory implements AmazonClientFactory {
	
	@Mock
	AmazonS3Client mockS3Client;
	@Mock
	AmazonEC2Client mockEC2Client;
	@Mock
	AmazonSNSClient mockSNSClient;
	@Mock
	AmazonRDSClient mockRDSClient;
	@Mock
	AmazonCloudWatchClient mockCloudWatchClient;
	@Mock
	AWSElasticBeanstalkClient mockElasticBeanstalkClient;
	@Mock
	AmazonIdentityManagementClient mockIdentityManagementClient;
	@Mock
	AmazonCloudSearchClient mockCloudSearchClient;
	@Mock
	AmazonRoute53Client mockRoute53Client;
	@Mock
	AmazonElasticLoadBalancingClient mockLoadBalancingClient;
	@Mock
	AWSCertificateManagerClient mockCertificateManagerClient;
	@Mock
	AWSCredentials credentials;
	
	/**
	 * This will setup all mocks.
	 */
	public MockAmazonClientFactory(){
		MockitoAnnotations.initMocks(this);
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
		return mockIdentityManagementClient;
	}

	public AmazonCloudSearchClient createCloudSearchClient() {
		return mockCloudSearchClient;
	}
	
	public AmazonRoute53Client createRoute53Client() {
		return mockRoute53Client;
	}
	
	@Override
	public AmazonElasticLoadBalancingClient createElasticLoadBalancingClient() {
		return mockLoadBalancingClient;
	}

	@Override
	public AWSCertificateManagerClient createCertificateManagerClient() {
		return mockCertificateManagerClient;
	}

}
