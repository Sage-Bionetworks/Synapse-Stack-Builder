package org.sagebionetworks.stack;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.IpPermission;

/**
 * Setup the security used by the rest of the stack.
 * 
 * @author jmhill
 *
 */
public class SecuritySetup {
	
	private static Logger log = Logger.getLogger(SecuritySetup.class.getName());
	
	public static final String IP_PROTOCOL_TCP = "tcp";
	public static final int PORT_HTTPS = 443;
	public static final int PORT_HTTP = 80;
	public static final int PORT_SSH = 22;
	
	/**
	 * The classless inter-domain routing to allow access to all IPs
	 */
	public static final String CIDR_ALL_IP = "0.0.0.0/0";
	
	/**
	 * Create the EC2 security group that all elastic beanstalk instances will belong to.
	 * 
	 * @param ec2Client - valid AmazonEC2Client
	 * @param stack - The name of this stack.
	 * @param instance - The name of this stack instance.
	 * @param cidrForSSH - The classless inter-domain routing to be used for SSH access to these machines.
	 * @return
	 */
	public static String setupElasticBeanstalkEC2SecutiryGroup(AmazonEC2Client ec2Client, String stack, String instance, String cidrForSSH){
		if(ec2Client == null) throw new IllegalArgumentException("AmazonEC2Client cannot be null");
		if(stack == null) throw new IllegalArgumentException("Stack cannot be null");
		if(instance == null) throw new IllegalArgumentException("Stack instances cannot be null");
		if(cidrForSSH == null) throw new IllegalArgumentException("The classless inter-domain routing for SSH cannot be null");
		
		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
		request.setDescription("All elastic beanstalk instances of stack:'"+stack+"' instance:'"+instance+"' belong to this EC2 security group");
		request.setGroupName(String.format("elastic-beanstalk-%1$s-%2$s", stack, instance));
		try{
			// First create the EC2 group
			CreateSecurityGroupResult result = ec2Client.createSecurityGroup(request);
		}catch (AmazonServiceException e){
			if("InvalidGroup.Duplicate".equals(e.getErrorCode())){
				// This group already exists
				log.info("Security Group: "+request.getGroupName()+" already exits");
			}else{
				throw e;
			}
		}
		//Setup the permissions for this group:
		// Allow anyone to access port 80 (HTTP)
		addPermission(ec2Client, request.getGroupName(), new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_HTTP).withToPort(PORT_HTTP).withIpRanges(CIDR_ALL_IP));
		// Allow anyone to access port 443 (HTTPS)
		addPermission(ec2Client, request.getGroupName(), new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_HTTPS).withToPort(PORT_HTTPS).withIpRanges(CIDR_ALL_IP));
		// Only allow ssh to the given address
		addPermission(ec2Client, request.getGroupName(), new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_SSH).withToPort(PORT_SSH).withIpRanges(cidrForSSH));

		// Return the group name
		return request.getGroupName();

	}
	
	/**
	 * Add a single permission to the passed group.  If the permission already exists, this will be a no-operation.
	 * @param ec2Client
	 * @param groupName
	 * @param permission
	 */
	private static void addPermission(AmazonEC2Client ec2Client, String groupName, IpPermission permission){
		// Make sure we can access the machines from with the VPN
		try{
			List<IpPermission> permissions = new LinkedList<IpPermission>();
			permissions.add(permission);
			// Configure this group
			AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest(groupName, permissions);
			ec2Client.authorizeSecurityGroupIngress(ingressRequest);
		}catch(AmazonServiceException e){
			// Ignore duplicates
			if("InvalidPermission.Duplicate".equals(e.getErrorCode())){
				// This already exists
			}else{
				// Throw any other error
				throw e;
			}
		}
	}
	

}
