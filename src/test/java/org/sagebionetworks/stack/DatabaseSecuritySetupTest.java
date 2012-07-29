package org.sagebionetworks.stack;

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

public class DatabaseSecuritySetupTest {
	
	Properties inputProperties;
	String id = "aws id";
	String password = "aws password";
	String encryptionKey = "encryptionKey";
	String stack = "dev";
	String instance ="A";
	InputConfiguration config;	
	AmazonRDSClient mockClient = null;
	SecurityGroup elasticSecurityGroup;
	String CIDR = "123.123.123/23";
	DatabaseSecuritySetup databaseSecuritySetup;
	
	@Before
	public void before() throws IOException{
		mockClient = Mockito.mock(AmazonRDSClient.class);
		inputProperties = new Properties();
		inputProperties.put(Constants.AWS_ACCESS_KEY, id);
		inputProperties.put(Constants.AWS_SECRET_KEY, password);
		inputProperties.put(Constants.STACK_ENCRYPTION_KEY, encryptionKey);
		inputProperties.put(Constants.STACK, stack);
		inputProperties.put(Constants.INSTANCE, instance);
		config = new InputConfiguration(inputProperties);
		Properties defaults = new Properties();
		defaults.put(Constants.KEY_CIDR_FOR_SSH, CIDR);
		config.addDefaultStackProperties(defaults);
		elasticSecurityGroup = new SecurityGroup().withGroupName("ec2-security-group-name").withOwnerId("123");
		databaseSecuritySetup = new DatabaseSecuritySetup(mockClient, config, elasticSecurityGroup);
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
		// Make the call
		databaseSecuritySetup.setupDatabaseAllSecuityGroups();
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
		ingressRequest.setCIDRIP(CIDR);
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(ingressRequest);
		// add to the stack db group
		// Check the access adds
		ingressRequest = new AuthorizeDBSecurityGroupIngressRequest(config.getStackDatabaseSecurityGroupName());
		ingressRequest.setEC2SecurityGroupOwnerId(elasticSecurityGroup.getOwnerId());
		ingressRequest.setEC2SecurityGroupName(elasticSecurityGroup.getGroupName());
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(ingressRequest);
		// add stack CIDR
		ingressRequest = new AuthorizeDBSecurityGroupIngressRequest(config.getStackDatabaseSecurityGroupName());
		ingressRequest.setCIDRIP(CIDR);
		verify(mockClient, times(1)).authorizeDBSecurityGroupIngress(ingressRequest);
	}

}
