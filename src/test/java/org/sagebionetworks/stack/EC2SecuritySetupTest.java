package org.sagebionetworks.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.stack.Constants.CIDR_ALL_IP;
import static org.sagebionetworks.stack.Constants.IP_PROTOCOL_TCP;
import static org.sagebionetworks.stack.Constants.PORT_HTTP;
import static org.sagebionetworks.stack.Constants.PORT_HTTPS;
import static org.sagebionetworks.stack.Constants.PORT_SSH;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.SecurityGroup;

/**
 * 
 * @author John
 *
 */
public class EC2SecuritySetupTest {
	
	InputConfiguration config;	

	AmazonEC2Client mockEC2Client;
	EC2SecuritySetup ec2SecuritySetup;
	GeneratedResources resources;

	@Before
	public void before() throws IOException {
		mockEC2Client = Mockito.mock(AmazonEC2Client.class);
		config = InputConfigHelper.createTestConfig("dev");
		resources = new GeneratedResources();
		ec2SecuritySetup = new EC2SecuritySetup(mockEC2Client, config, resources);
	}
	
	@Test (expected=AmazonServiceException.class)
	public void testCreateGroupUnknownError(){
		// For this case make sure an unknown error gets thrown
		AmazonServiceException exception = new AmazonServiceException("Some error");
		exception.setErrorCode("unknown code");
		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
		when(mockEC2Client.createSecurityGroup(request)).thenThrow(exception);
		ec2SecuritySetup.createSecurityGroup(request);
	}
	
	@Test
	public void testCreateGroupDuplicate(){
		// For this case we are simulating a duplicate group exception.
		// When the group already exists an exception should not be thrown.
		AmazonServiceException exception = new AmazonServiceException("Some error");
		exception.setErrorCode(Constants.ERROR_CODE_INVALID_GROUP_DUPLICATE);
		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
		when(mockEC2Client.createSecurityGroup(request)).thenThrow(exception);
		ec2SecuritySetup.createSecurityGroup(request);
	}

	@Test (expected=AmazonServiceException.class)
	public void testAddPermissionUnknonwError() {
		// For this case make sure an unknown error gets thrown
		AmazonServiceException exception = new AmazonServiceException("Some error");
		exception.setErrorCode("unknown code");
		doThrow(exception).when(mockEC2Client).authorizeSecurityGroupIngress(
				any(AuthorizeSecurityGroupIngressRequest.class));
		ec2SecuritySetup.addPermission("groupName", new IpPermission());
	}

	@Test
	public void testAddPermissionDuplicate() {
		// When a duplicate error code is thrown then the exception should not be thrown
		AmazonServiceException exception = new AmazonServiceException("Some error");
		exception.setErrorCode(Constants.ERROR_CODE_INVALID_PERMISSION_DUPLICATE);
		doThrow(exception).when(mockEC2Client).authorizeSecurityGroupIngress(
				any(AuthorizeSecurityGroupIngressRequest.class));
		ec2SecuritySetup.addPermission("groupName", new IpPermission());
	}
	
	@Test
	public void testSetupElasticBeanstalkEC2SecutiryGroup(){
		String expectedDescription = config.getElasticSecurityGroupDescription();
		String expectedGroupName = config.getElasticSecurityGroupName();
		DescribeSecurityGroupsResult result = new DescribeSecurityGroupsResult();
		SecurityGroup expectedGroup = new SecurityGroup().withGroupName(expectedGroupName).withOwnerId("123");
		result.withSecurityGroups(expectedGroup);
		when(mockEC2Client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class))).thenReturn(result);
		// Create the security group.
		SecurityGroup group = ec2SecuritySetup.setupElasticBeanstalkEC2SecutiryGroup();
		assertEquals(expectedGroup, group);
		String groupName = group.getGroupName();
		assertNotNull(groupName);
		assertEquals(expectedGroupName, groupName);
		
		CreateSecurityGroupRequest groupRequest = new CreateSecurityGroupRequest(expectedGroupName, expectedDescription);
		// The create group should be called
		verify(mockEC2Client).createSecurityGroup(groupRequest);
		// Three permission should be set
		// http
		List<IpPermission> list = new LinkedList<IpPermission>();
		list.add(new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_HTTP).withToPort(PORT_HTTP).withIpRanges(CIDR_ALL_IP));
		AuthorizeSecurityGroupIngressRequest request = new AuthorizeSecurityGroupIngressRequest(groupName, list);
		verify(mockEC2Client).authorizeSecurityGroupIngress(request);
		// https
		list = new LinkedList<IpPermission>();
		list.add(new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_HTTPS).withToPort(PORT_HTTPS).withIpRanges(CIDR_ALL_IP));
		request = new AuthorizeSecurityGroupIngressRequest(groupName, list);
		verify(mockEC2Client).authorizeSecurityGroupIngress(request);
		// ssh
		list = new LinkedList<IpPermission>();
		list.add(new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_SSH).withToPort(PORT_SSH).withIpRanges(config.getCIDRForSSH()));
		request = new AuthorizeSecurityGroupIngressRequest(groupName, list);
		verify(mockEC2Client).authorizeSecurityGroupIngress(request);
		// Make sure this is set
		assertNotNull(resources.getElasticBeanstalkEC2SecurityGroup());
	}
}
