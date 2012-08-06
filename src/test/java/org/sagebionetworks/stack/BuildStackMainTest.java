package org.sagebionetworks.stack;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsResult;
import com.amazonaws.services.rds.model.DescribeDBParametersRequest;
import com.amazonaws.services.rds.model.DescribeDBParametersResult;
import com.amazonaws.services.rds.model.Parameter;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.Subscription;

public class BuildStackMainTest {
	
	Properties inputProps;
	Properties defaultProps;
	MockAmazonClientFactory clientFactory;
	
	@Before
	public void before() throws IOException{
		inputProps = TestHelper.createInputProperties("dev");
		InputConfiguration config = TestHelper.createTestConfig("dev");
		defaultProps = TestHelper.createDefaultProperties();
		clientFactory = new MockAmazonClientFactory();
		AmazonS3Client mockS3Client = clientFactory.createS3Client();
		AmazonEC2Client mockEC2Client = clientFactory.createEC2Client();
		AmazonSNSClient mockSNSnsClient = clientFactory.createSNSClient();
		AmazonRDSClient mockRdsClient = clientFactory.createRDSClient();
		
		// Write the default properties.
		when(mockS3Client.getObject(any(GetObjectRequest.class), any(File.class))).thenAnswer(new Answer<ObjectMetadata>() {
			public ObjectMetadata answer(InvocationOnMock invocation) throws Throwable {
				// Write the property file
				File file = (File) invocation.getArguments()[1];
				FileWriter writer = new FileWriter(file);
				try{
					defaultProps.store(writer, "test generated");
				}finally{
					writer.close();
				}
				return new ObjectMetadata();
			}
		});
		// Return a valid EC2 security group.
		DescribeSecurityGroupsRequest dsgr = new DescribeSecurityGroupsRequest().withGroupNames(config.getElasticSecurityGroupName());
		when(mockEC2Client.describeSecurityGroups(dsgr)).thenReturn(new DescribeSecurityGroupsResult().withSecurityGroups(new SecurityGroup().withGroupName(config.getElasticSecurityGroupName())));
		// Return a valid topic
		String topicArn = "some:arn";
		when(mockSNSnsClient.createTopic(new CreateTopicRequest(config.getRDSAlertTopicName()))).thenReturn(new CreateTopicResult().withTopicArn(topicArn));
		when(mockSNSnsClient.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn))).thenReturn(new ListSubscriptionsByTopicResult().withSubscriptions(new Subscription()));
		// return a valid group
		when(mockRdsClient.describeDBParameterGroups(new DescribeDBParameterGroupsRequest().withDBParameterGroupName(config.getDatabaseParameterGroupName()))).thenReturn(new DescribeDBParameterGroupsResult().withDBParameterGroups(new DBParameterGroup().withDBParameterGroupName(config.getDatabaseParameterGroupName())));
		when(mockRdsClient.describeDBParameters(new DescribeDBParametersRequest().withDBParameterGroupName(config.getDatabaseParameterGroupName()))).thenReturn(new DescribeDBParametersResult().withParameters(new Parameter().withParameterName(Constants.DB_PARAM_KEY_SLOW_QUERY_LOG)).withParameters(new Parameter().withParameterName(Constants.DB_PARAM_KEY_LONG_QUERY_TIME)));
	}
	
	@Test
	public void testBuildStack() throws FileNotFoundException, IOException{
		// This is is a crazy test.
//		GeneratedResources resource = BuildStackMain.buildStack(inputProps, clientFactory);
//		assertNotNull(resource);
	}

}
