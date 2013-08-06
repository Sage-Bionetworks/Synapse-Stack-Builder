package org.sagebionetworks.stack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

import static org.sagebionetworks.stack.Constants.*;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import java.util.Arrays;

/**
 * Setup the security used by the rest of the stack.
 * 
 * @author jmhill
 *
 */
public class EC2SecuritySetup implements ResourceProcessor {
	
	private static Logger log = Logger.getLogger(EC2SecuritySetup.class.getName());
	
	private AmazonEC2Client ec2Client;
	private AmazonS3Client s3Client;
	private InputConfiguration config;
	private GeneratedResources resources;
	/**
	 * IoC constructor.
	 * @param ec2Client
	 * @param config
	 * @param resoruces 
	 */
	public EC2SecuritySetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		super();
		initialize(factory, config, resources);
	}
	
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		if(factory == null) throw new IllegalArgumentException("AmazonClientFactory cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		this.ec2Client = factory.createEC2Client();
		this.s3Client = factory.createS3Client();
		this.config = config;
		this.resources = resources;
	}
	
	/**
	 * Create the EC2 security group that all elastic beanstalk instances will belong to.
	 * 
	 * @param ec2Client - valid AmazonEC2Client
	 * @param stack - The name of this stack.
	 * @param instance - The name of this stack instance.
	 * @param cidrForSSH - The classless inter-domain routing to be used for SSH access to these machines.
	 * @return
	 */
	public void setupResources() {
		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
		request.setDescription(config.getElasticSecurityGroupDescription());
		request.setGroupName(config.getElasticSecurityGroupName());
		createSecurityGroup(request);
		//Setup the permissions for this group:
		// Allow anyone to access port 80 (HTTP)
		addPermission(request.getGroupName(), new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_HTTP).withToPort(PORT_HTTP).withIpRanges(CIDR_ALL_IP));
		// Allow anyone to access port 443 (HTTPS)
		addPermission(request.getGroupName(), new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_HTTPS).withToPort(PORT_HTTPS).withIpRanges(CIDR_ALL_IP));
		// Only allow ssh to the given address
		addPermission(request.getGroupName(), new IpPermission().withIpProtocol(IP_PROTOCOL_TCP).withFromPort(PORT_SSH).withToPort(PORT_SSH).withIpRanges(config.getCIDRForSSH()));
		// Return the group name
		DescribeSecurityGroupsResult result = ec2Client.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupNames(request.getGroupName()));
		if(result.getSecurityGroups() == null || result.getSecurityGroups().size() != 1) throw new IllegalStateException("Did not find one and ony one EC2 secruity group with the name: "+request.getGroupName());
		// Add this to the resources
		SecurityGroup group = result.getSecurityGroups().get(0);
		resources.setElasticBeanstalkEC2SecurityGroup(group);
		
		// Create the key pair.
		resources.setStackKeyPair(createOrGetKeyPair());
		
	}
	
	/*
	 * Teardown all EC2 security group resources
	 * NOTE: NoOp
	 */
	public void teardownResources() {
	}

	public void describeResources() {
		DescribeSecurityGroupsRequest req = new DescribeSecurityGroupsRequest();
		req.setGroupNames(Arrays.asList(config.getElasticSecurityGroupName()));
		DescribeSecurityGroupsResult res = ec2Client.describeSecurityGroups(req);
		if ((res.getSecurityGroups() != null) && res.getSecurityGroups().size() == 1) {
			SecurityGroup grp = res.getSecurityGroups().get(0);
			resources.setElasticBeanstalkEC2SecurityGroup(grp);
			String kpName = config.getStackKeyPairName();
			KeyPairInfo inf = describeKeyPair();
			if (inf != null) {
				resources.setStackKeyPair(inf);
			}
		} else {
			throw new IllegalStateException("Did not find one and ony one EC2 secruity group with the name: " + req.getGroupNames());
		}
	}

	/**
	 * Create the key par
	 * @return
	 */
	public KeyPairInfo createOrGetKeyPair(){
		String name =config.getStackKeyPairName();
		KeyPairInfo info = describeKeyPair();
		if(info == null){
			log.debug("Creating the Stack KeyPair: "+name+" for the first time");
			CreateKeyPairResult kpResult = ec2Client.createKeyPair(new CreateKeyPairRequest(name));
			
			File temp = null;
			FileOutputStream fos = null;
			try{
				temp = File.createTempFile("Temp", ".tmp");
				fos = new FileOutputStream(temp);
				// Write the material to the file.
				fos.write(kpResult.getKeyPair().getKeyMaterial().getBytes("UTF-8"));
				fos.close();
				// Now write the file to S3
				s3Client.putObject(new PutObjectRequest(config.getStackConfigS3BucketName(), config.getStackKeyPairS3File(), temp));
			} catch (IOException e) {
				// convert to runtime
				throw new RuntimeException(e);
			}finally{
				if(fos != null){
					try {
						fos.close();
					} catch (IOException e) {}
				}
				if(temp != null){
					temp.delete();
				}
			}
			
			
			return describeKeyPair();
		}else{
			log.debug("Stack KeyPair: "+name+" already exists");
			return info;
		}
		
	}
	
	/**
	 * Describe the key pair if it exists.
	 * 
	 * @return
	 */
	public KeyPairInfo describeKeyPair(){
		String name =config.getStackKeyPairName();
		try{
			DescribeKeyPairsResult result =ec2Client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(name));
			if(result.getKeyPairs().size() != 1) throw new IllegalStateException("Expceted one and only one key pair with the name: "+name);
			return result.getKeyPairs().get(0);
		}catch (AmazonServiceException e){
			if(Constants.ERROR_CODE_KEY_PAIR_NOT_FOUND.equals(e.getErrorCode())){
				return null;
			}else{
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Create a security group. If the group already exists
	 * @param ec2Client
	 * @param request
	 */
	void createSecurityGroup(CreateSecurityGroupRequest request) {
		try{
			// First create the EC2 group
			log.info("Creating Security Group: "+request.getGroupName()+"...");
			CreateSecurityGroupResult result = ec2Client.createSecurityGroup(request);
		}catch (AmazonServiceException e){
			if(ERROR_CODE_INVALID_GROUP_DUPLICATE.equals(e.getErrorCode())){
				// This group already exists
				log.info("Security Group: "+request.getGroupName()+" already exits");
			}else{
				throw e;
			}
		}
	}
	
	/**
	 * Add a single permission to the passed group.  If the permission already exists, this will be a no-operation.
	 * @param ec2Client
	 * @param groupName
	 * @param permission
	 */
	void addPermission(String groupName, IpPermission permission){
		// Make sure we can access the machines from with the VPN
		try{
			List<IpPermission> permissions = new LinkedList<IpPermission>();
			permissions.add(permission);
			// Configure this group
			AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest(groupName, permissions);
			log.info("Adding IpPermission to group: '"+groupName+"'...");
			log.info("IpPermission: "+permission.toString()+"");
			ec2Client.authorizeSecurityGroupIngress(ingressRequest);
		}catch(AmazonServiceException e){
			// Ignore duplicates
			if(ERROR_CODE_INVALID_PERMISSION_DUPLICATE.equals(e.getErrorCode())){
				// This already exists
				log.info("IpPermission: "+permission.toString()+" already exists for '"+groupName+"'");
			}else{
				// Throw any other error
				throw e;
			}
		}
	}
	

}
