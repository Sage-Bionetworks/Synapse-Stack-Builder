package org.sagebionetworks;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.stack.Constants.IP_PROTOCOL_TCP;
import static org.sagebionetworks.stack.Constants.PORT_HTTP;
import static org.sagebionetworks.stack.Constants.PORT_SSH;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.stack.Constants;
import org.sagebionetworks.stack.SecuritySetup;

import static org.sagebionetworks.stack.Constants.*;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.IpPermission;

/**
 * 
 * @author John
 *
 */
public class SecuritySetupTest {

	AmazonEC2Client mockEC2Client;

	@Before
	public void before() {
		mockEC2Client = Mockito.mock(AmazonEC2Client.class);
	}
	
	@Test (expected=AmazonServiceException.class)
	public void testCreateGroupUnknownError(){
		// For this case make sure an unknown error gets thrown
		AmazonServiceException exception = new AmazonServiceException("Some error");
		exception.setErrorCode("unknown code");
		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
		when(mockEC2Client.createSecurityGroup(request)).thenThrow(exception);
		SecuritySetup.createSecurityGroup(mockEC2Client, request);
	}
	
	@Test
	public void testCreateGroupDuplicate(){
		// For this case we are simulating a duplicate group exception.
		// When the group already exists an exception should not be thrown.
		AmazonServiceException exception = new AmazonServiceException("Some error");
		exception.setErrorCode(Constants.ERROR_CODE_INVALID_GROUP_DUPLICATE);
		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
		when(mockEC2Client.createSecurityGroup(request)).thenThrow(exception);
		SecuritySetup.createSecurityGroup(mockEC2Client, request);
	}

	@Test (expected=AmazonServiceException.class)
	public void testAddPermissionUnknonwError() {
		// For this case make sure an unknown error gets thrown
		AmazonServiceException exception = new AmazonServiceException("Some error");
		exception.setErrorCode("unknown code");
		doThrow(exception).when(mockEC2Client).authorizeSecurityGroupIngress(
				any(AuthorizeSecurityGroupIngressRequest.class));
		SecuritySetup.addPermission(mockEC2Client, "groupName", new IpPermission());
	}

	@Test
	public void testAddPermissionDuplicate() {
		// When a duplicate error code is thrown then the exception should not be thrown
		AmazonServiceException exception = new AmazonServiceException("Some error");
		exception.setErrorCode(Constants.ERROR_CODE_INVALID_PERMISSION_DUPLICATE);
		doThrow(exception).when(mockEC2Client).authorizeSecurityGroupIngress(
				any(AuthorizeSecurityGroupIngressRequest.class));
		SecuritySetup.addPermission(mockEC2Client, "groupName", new IpPermission());
	}
	
	@Test
	public void testSetupElasticBeanstalkEC2SecutiryGroup(){
		String stack = "stack";
		String instance = "instnace";
		String cidrForSSH = "255.255.255/1";
		
		String expectedDescription = String.format(Constants.SECURITY_GROUP_DESCRIPTION_TEMPLATE, stack, instance);
		String expectedGroupName = String.format(Constants.SECURITY_GROUP_NAME_TEMPLATE, stack, instance);
		// Create the security group.
		String groupName = SecuritySetup.setupElasticBeanstalkEC2SecutiryGroup(mockEC2Client, stack, instance, cidrForSSH);
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
		list.add(new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_SSH).withToPort(PORT_SSH).withIpRanges(cidrForSSH));
		request = new AuthorizeSecurityGroupIngressRequest(groupName, list);
		verify(mockEC2Client).authorizeSecurityGroupIngress(request);
	}
}
