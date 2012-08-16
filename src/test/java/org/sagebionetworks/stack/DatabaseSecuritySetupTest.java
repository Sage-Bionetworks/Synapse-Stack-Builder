package org.sagebionetworks.stack;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.stack.Constants.ERROR_CODE_AUTHORIZATION_ALREADY_EXITS;
import static org.sagebionetworks.stack.Constants.ERROR_CODE_DB_SECURITY_GROUP_ALREADY_EXISTS;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AuthorizeDBSecurityGroupIngressRequest;
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest;
import com.amazonaws.services.rds.model.CreateDBSecurityGroupRequest;
import com.amazonaws.services.rds.model.DBSecurityGroup;
import com.amazonaws.services.rds.model.DescribeDBSecurityGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBSecurityGroupsResult;
import org.sagebionetworks.factory.MockAmazonClientFactory;

public class DatabaseSecuritySetupTest {
	
	InputConfiguration config;	
	AmazonRDSClient mockClient = null;
	SecurityGroup elasticSecurityGroup;
	DatabaseSecuritySetup databaseSecuritySetup;
	GeneratedResources resources;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	
	@Before
	public void before() throws IOException{
		mockClient = factory.createRDSClient();
		config = TestHelper.createTestConfig("dev");
		elasticSecurityGroup = new SecurityGroup().withGroupName("ec2-security-group-name").withOwnerId("123");
		resources = new GeneratedResources();
		resources.setElasticBeanstalkEC2SecurityGroup(elasticSecurityGroup);
		databaseSecuritySetup = new DatabaseSecuritySetup(factory, config, resources);
	}
	
	
	/**
	 * The only error code we should catch is the duplicate error code.  All other exceptions should be re-thrown.
	 */
	@Test (expected=AmazonServiceException.class)
	public void testAddEC2SecurityGroupUknonwError(){
		String dbGroupName = "dbGroupName";
		AmazonServiceException unknown = new AmazonServiceException("Unknonwn");
		unknown.setErrorCode("unknown error code");
		when(mockClient.authorizeDBSecurityGroupIngress(any(AuthorizeDBSecurityGroupIngressRequest.class))).thenThrow(unknown);
		// Make the call
		databaseSecuritySetup.addEC2SecurityGroup(dbGroupName, elasticSecurityGroup);
	}
	
	
	/**
	 * When a duplicate is error is thrown we it should not be re-thrown.
	 */
	@Test
	public void testAddEC2SecurityGroupDuplicateError(){
		String dbGroupName = "dbGroupName";
		AuthorizeDBSecurityGroupIngressRequest expectedIngress = new AuthorizeDBSecurityGroupIngressRequest(dbGroupName);
		expectedIngress.setEC2SecurityGroupOwnerId(elasticSecurityGroup.getOwnerId());
		expectedIngress.setEC2SecurityGroupName(elasticSecurityGroup.getGroupName());
		
		AmazonServiceException unknown = new AmazonServiceException("Unknonwn");
		unknown.setErrorCode(ERROR_CODE_AUTHORIZATION_ALREADY_EXITS);
		when(mockClient.authorizeDBSecurityGroupIngress(any(AuthorizeDBSecurityGroupIngressRequest.class))).thenThrow(unknown);
		// Make the call
		databaseSecuritySetup.addEC2SecurityGroup(dbGroupName, elasticSecurityGroup);
		// Validate the data was passed
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(expectedIngress);
	}
	
	/**
	 * Verify the request is made as expected.
	 */
	@Test
	public void testAddEC2SecurityGroup(){
		String dbGroupName = "dbGroupName";
		AuthorizeDBSecurityGroupIngressRequest expectedIngress = new AuthorizeDBSecurityGroupIngressRequest(dbGroupName);
		expectedIngress.setEC2SecurityGroupOwnerId(elasticSecurityGroup.getOwnerId());
		expectedIngress.setEC2SecurityGroupName(elasticSecurityGroup.getGroupName());
		
		AmazonServiceException unknown = new AmazonServiceException("Unknonwn");
		unknown.setErrorCode(ERROR_CODE_AUTHORIZATION_ALREADY_EXITS);
		when(mockClient.authorizeDBSecurityGroupIngress(any(AuthorizeDBSecurityGroupIngressRequest.class))).thenReturn(new DBSecurityGroup());
		// Make the call
		databaseSecuritySetup.addEC2SecurityGroup(dbGroupName, elasticSecurityGroup);
		// Validate the data was passed
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(expectedIngress);
	}
	
	/**
	 * The only error code we should catch is the duplicate error code.  All other exceptions should be re-thrown.
	 */
	@Test (expected=AmazonServiceException.class)
	public void testAddCIDRToGroupUnknonwError(){
		String dbGroupName = "dbGroupName";
		String cIDR = "0.0.0.0/255";
		AmazonServiceException unknown = new AmazonServiceException("Unknonwn");
		unknown.setErrorCode("unknown error code");
		when(mockClient.authorizeDBSecurityGroupIngress(any(AuthorizeDBSecurityGroupIngressRequest.class))).thenThrow(unknown);
		// Make the call
		databaseSecuritySetup.addCIDRToGroup(dbGroupName, cIDR);
	}
	
	
	/**
	 * When a duplicate is error is thrown we it should not be re-thrown.
	 */
	@Test
	public void testAddCIDRToGroupDuplicateError(){
		String dbGroupName = "dbGroupName";
		String cIDR = "0.0.0.0/255";
		AuthorizeDBSecurityGroupIngressRequest expectedIngress = new AuthorizeDBSecurityGroupIngressRequest(dbGroupName);
		expectedIngress.setCIDRIP(cIDR);
		
		AmazonServiceException unknown = new AmazonServiceException("Unknonwn");
		unknown.setErrorCode(ERROR_CODE_AUTHORIZATION_ALREADY_EXITS);
		when(mockClient.authorizeDBSecurityGroupIngress(any(AuthorizeDBSecurityGroupIngressRequest.class))).thenThrow(unknown);
		// Make the call
		databaseSecuritySetup.addCIDRToGroup(dbGroupName, cIDR);
		// Validate the data was passed
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(expectedIngress);
	}
	
	/**
	 * Verify the request is made as expected.
	 */
	@Test
	public void testAddCIDRToGroup(){
		String dbGroupName = "dbGroupName";
		String cIDR = "0.0.0.0/255";
		AuthorizeDBSecurityGroupIngressRequest expectedIngress = new AuthorizeDBSecurityGroupIngressRequest(dbGroupName);
		expectedIngress.setCIDRIP(cIDR);
		AmazonServiceException unknown = new AmazonServiceException("Unknonwn");
		unknown.setErrorCode(ERROR_CODE_AUTHORIZATION_ALREADY_EXITS);
		when(mockClient.authorizeDBSecurityGroupIngress(any(AuthorizeDBSecurityGroupIngressRequest.class))).thenReturn(new DBSecurityGroup());
		// Make the call
		databaseSecuritySetup.addCIDRToGroup(dbGroupName, cIDR);
		// Validate the data was passed
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(expectedIngress);
	}
	
	/**
	 * Unknown error codes should be re-thrown.
	 */
	@Test (expected=AmazonServiceException.class)
	public void testCreateSecurityGroupUnknownError(){
		CreateDBSecurityGroupRequest request = new CreateDBSecurityGroupRequest();
		request.setDBSecurityGroupName("name");
		request.setDBSecurityGroupDescription("description");
		AmazonServiceException exception = new AmazonServiceException("unknown");
		exception.setErrorCode("unknown error code");
		when(mockClient.createDBSecurityGroup(request)).thenThrow(exception);
		databaseSecuritySetup.createSecurityGroup(request);
	}
	
	/**
	 * Duplicate error codes are expected and should be ignored.
	 */
	@Test
	public void testCreateSecurityGroupDuplicate(){
		CreateDBSecurityGroupRequest request = new CreateDBSecurityGroupRequest();
		request.setDBSecurityGroupName("name");
		request.setDBSecurityGroupDescription("description");
		AmazonServiceException exception = new AmazonServiceException("unknown");
		exception.setErrorCode(ERROR_CODE_DB_SECURITY_GROUP_ALREADY_EXISTS);
		when(mockClient.createDBSecurityGroup(request)).thenThrow(exception);
		databaseSecuritySetup.createSecurityGroup(request);
		verify(mockClient, times(1)).createDBSecurityGroup(request);
	}
	
	/**
	 * Test that that the data is passed to the client as expected.
	 */
	@Test
	public void testCreateSecurityGroup(){
		CreateDBSecurityGroupRequest request = new CreateDBSecurityGroupRequest();
		request.setDBSecurityGroupName("name");
		request.setDBSecurityGroupDescription("description");
		when(mockClient.createDBSecurityGroup(request)).thenReturn(new DBSecurityGroup());
		databaseSecuritySetup.createSecurityGroup(request);
		verify(mockClient, times(1)).createDBSecurityGroup(request);
	}
	
	/**
	 * Test that the expected security groups are created.
	 */
	@Test
	public void testSetupDatabaseAllSecuityGroups(){
		// Id gen
		DBSecurityGroup expectedIdGroup = new DBSecurityGroup().withDBSecurityGroupName(config.getIdGeneratorDatabaseSecurityGroupName());
		DescribeDBSecurityGroupsResult result = new DescribeDBSecurityGroupsResult().withDBSecurityGroups(expectedIdGroup);
		when(mockClient.describeDBSecurityGroups(new DescribeDBSecurityGroupsRequest().withDBSecurityGroupName(config.getIdGeneratorDatabaseSecurityGroupName()))).thenReturn(result);
		// stack
		DBSecurityGroup expectedStackGroup = new DBSecurityGroup().withDBSecurityGroupName(config.getStackDatabaseSecurityGroupName());
		result = new DescribeDBSecurityGroupsResult().withDBSecurityGroups(expectedStackGroup);
		when(mockClient.describeDBSecurityGroups(new DescribeDBSecurityGroupsRequest().withDBSecurityGroupName(config.getStackDatabaseSecurityGroupName()))).thenReturn(result);
		
		// Make the call
		databaseSecuritySetup.setupDatabaseAllSecurityGroups();
		// Verify the expected calls
		// Id gen db security group
		CreateDBSecurityGroupRequest request = new CreateDBSecurityGroupRequest();
		request.setDBSecurityGroupDescription(config.getIdGeneratorDatabaseSecurityGroupDescription());
		request.setDBSecurityGroupName(config.getIdGeneratorDatabaseSecurityGroupName());
		verify(mockClient, times(1)).createDBSecurityGroup(request);
		// Stack db security group
		request = new CreateDBSecurityGroupRequest();
		request.setDBSecurityGroupDescription(config.getStackDatabaseSecurityGroupDescription());
		request.setDBSecurityGroupName(config.getStackDatabaseSecurityGroupName());
		verify(mockClient, times(1)).createDBSecurityGroup(request);
		// Check the access adds
		// Add to id gen group
		AuthorizeDBSecurityGroupIngressRequest ingressRequest = new AuthorizeDBSecurityGroupIngressRequest(config.getIdGeneratorDatabaseSecurityGroupName());
		ingressRequest.setEC2SecurityGroupOwnerId(elasticSecurityGroup.getOwnerId());
		ingressRequest.setEC2SecurityGroupName(elasticSecurityGroup.getGroupName());
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(ingressRequest);
		// add id gen CIDR
		ingressRequest = new AuthorizeDBSecurityGroupIngressRequest(config.getIdGeneratorDatabaseSecurityGroupName());
		ingressRequest.setCIDRIP(config.getCIDRForSSH());
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(ingressRequest);
		// add to the stack db group
		// Check the access adds
		ingressRequest = new AuthorizeDBSecurityGroupIngressRequest(config.getStackDatabaseSecurityGroupName());
		ingressRequest.setEC2SecurityGroupOwnerId(elasticSecurityGroup.getOwnerId());
		ingressRequest.setEC2SecurityGroupName(elasticSecurityGroup.getGroupName());
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(ingressRequest);
		// add stack CIDR
		ingressRequest = new AuthorizeDBSecurityGroupIngressRequest(config.getStackDatabaseSecurityGroupName());
		ingressRequest.setCIDRIP(config.getCIDRForSSH());
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(ingressRequest);
		
		// Make sure the groups are set in the resources
		assertEquals(expectedIdGroup, resources.getIdGeneratorDatabaseSecurityGroup());
		assertEquals(expectedStackGroup, resources.getStackInstancesDatabaseSecurityGroup());
		
	}

}
