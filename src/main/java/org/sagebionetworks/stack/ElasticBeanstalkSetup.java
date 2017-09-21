package org.sagebionetworks.stack;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;
import static org.sagebionetworks.stack.Constants.*;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.DeleteConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.UpdateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.UpdateConfigurationTemplateResult;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentResult;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;

/**
 * Setup the elastic beanstalk environments.
 * 
 * @author John
 *
 */
public class ElasticBeanstalkSetup implements ResourceProcessor {
	
	private static Logger logger = Logger.getLogger(ElasticBeanstalkSetup.class);
	
	private AWSElasticBeanstalkClient beanstalkClient;
	private AmazonIdentityManagementClient aimClient;
	private InputConfiguration config;
	private GeneratedResources resources;
	private ExecutorService executor = Executors.newFixedThreadPool(Constants.SVC_PREFIXES.size());
	private CompletionService<EnvironmentDescription> completionSvc = new ExecutorCompletionService<EnvironmentDescription>(executor);
	
	/**
	 * Will grant full access to S3 when applied.
	 */
	private static String ROLE_POLICY = "{ \"Version\": \"2012-10-17\",  \"Statement\": [ {  \"Effect\": \"Allow\", \"Action\": \"s3:*\", \"Resource\": \"*\"  }  ]}";
	
	/**
	 * Allows an EC2 instance to assume the role.
	 */
	private static String AssumeRolePolicyDocument = "{ \"Version\": \"2008-10-17\",  \"Statement\": [  {  \"Sid\": \"\",  \"Effect\": \"Allow\",   \"Principal\": { \"Service\": [ \"ec2.amazonaws.com\"   ] }, \"Action\": \"sts:AssumeRole\" }  ]}";
	/**
	 * The IoC constructor.
	 * 
	 * @param config
	 * @param resources
	 */
	public ElasticBeanstalkSetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		this.initialize(factory, config, resources);
	}

	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		if(factory == null) throw new IllegalArgumentException("AWSClientFactory cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");

		//TODO: Move all checks to private methods
		
		// There are many dependencies for this setup.
//		if(resources.getSslCertificate(StackEnvironmentType.REPO) == null) throw new IllegalArgumentException("GeneratedResources.getSslCertificate('plfm') cannot be null");
//		if(resources.getSslCertificate(StackEnvironmentType.WORKERS) == null) throw new IllegalArgumentException("GeneratedResources.getSslCertificate('worker') cannot be null");
//		if(resources.getSslCertificate(StackEnvironmentType.PORTAL) == null) throw new IllegalArgumentException("GeneratedResources.getSslCertificate('portal') cannot be null");
		
		checkACMCertificateARNs(resources);
		
		if(resources.getPortalApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getPortalApplicationVersion() cannot be null");
		if(resources.getRepoApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getReopApplicationVersion() cannot be null");
		if(resources.getWorkersApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getWorkersApplicationVersion() cannot be null");
		
		if(resources.getStackKeyPair() == null) throw new IllegalArgumentException("GeneratedResources.getStackKeyPair() cannot be null");
		
		this.beanstalkClient = factory.createBeanstalkClient();
		this.config = config;
		this.resources = resources;
		this.aimClient = factory.createIdentityManagementClient();
	}
	
	private void checkACMCertificateARNs(GeneratedResources resources) {
		for (StackEnvironmentType t: StackEnvironmentType.values()) {
			if(resources.getACMCertificateArn(t)== null){
				throw new IllegalArgumentException("Missing generated resource for ACM certificate ARN for environment: " + t.name());
			}
		}
	}
	
	public void setupResources() {
		this.createAllEnvironments();
	}
	
	public void teardownResources() {
		this.terminateAllEnvironments();
	}
	
	public void describeResources() {
		resources.setEnvironment(StackEnvironmentType.PORTAL, describeEnvironment(config.getEnvironmentName(PREFIX_PORTAL)));
		resources.setEnvironment(StackEnvironmentType.REPO, describeEnvironment(config.getEnvironmentName(PREFIX_REPO)));
		resources.setEnvironment(StackEnvironmentType.WORKERS, describeEnvironment(config.getEnvironmentName(PREFIX_WORKERS)));
	}

	/**
	 * Create the environments
	 */
	public void createAllEnvironments(){
		// setup the role, policy, and profile needed for rolling logs to S3.
		configureInstanceProfileForLogRolingToS3();
		// Create a profile that will used to enable logging.
		String plfmElbTemplateName = config.getElasticBeanstalkTemplateName() + "-plfm";
		String workerElbTemplateName = config.getElasticBeanstalkTemplateName() + "-worker";
		String portalElbTemplateName = config.getElasticBeanstalkTemplateName() + "-portal";
		// First create or update the templates using the current data.
		List<ConfigurationOptionSetting> cfgOptSettings = getAllElasticBeanstalkOptions(StackEnvironmentType.REPO);
		resources.setElasticBeanstalkConfigurationTemplate("plfm", createOrUpdateConfigurationTemplate(plfmElbTemplateName, cfgOptSettings));
		cfgOptSettings = getAllElasticBeanstalkOptions(StackEnvironmentType.WORKERS);
		resources.setElasticBeanstalkConfigurationTemplate("worker", createOrUpdateConfigurationTemplate(workerElbTemplateName, cfgOptSettings));
		cfgOptSettings = getAllElasticBeanstalkOptions(StackEnvironmentType.PORTAL);
		resources.setElasticBeanstalkConfigurationTemplate("portal", createOrUpdateConfigurationTemplate(portalElbTemplateName, cfgOptSettings));

		// Create the environments
		// portal
		createOrUpdateEnvironment(PREFIX_PORTAL, portalElbTemplateName, resources.getPortalApplicationVersion());
			// repo
		createOrUpdateEnvironment(PREFIX_REPO, plfmElbTemplateName, resources.getRepoApplicationVersion());
		// workers svc
		createOrUpdateEnvironment(PREFIX_WORKERS, workerElbTemplateName, resources.getWorkersApplicationVersion());
		
		// Fetch all of the results
		for (int numEnvironments = 0; numEnvironments < Constants.SVC_PREFIXES.size(); numEnvironments++) {
			try {
				Future<EnvironmentDescription> futureEnvDesc = completionSvc.take();
				EnvironmentDescription envDesc = futureEnvDesc.get();
				addCreatedEnvironmentDescriptionToResources(envDesc);
				logger.info("Environment created for : %s\n".format(envDesc.getApplicationName()));
			} catch (InterruptedException e) { // Exception in task
				logger.error("Error: InterruptedException");
				e.printStackTrace();
			} catch (ExecutionException e) { // Exception getting result
				logger.error("Error: ExecutionException");
				e.printStackTrace();
			}
		}
	}
	
	public void addCreatedEnvironmentDescriptionToResources(EnvironmentDescription ed) {
		if (ed == null) {
			throw new IllegalArgumentException("EnvironmentDescription cannot be null.");
		}
		if (! config.getEnvironmentName(PREFIX_REPO).equals(ed.getEnvironmentName()) &&
			(! config.getEnvironmentName(PREFIX_WORKERS).equals(ed.getEnvironmentName())) &&
			(! config.getEnvironmentName(PREFIX_PORTAL).equals(ed.getEnvironmentName()))) {
			throw new IllegalArgumentException("Invalid application name.");
		}
		if (config.getEnvironmentName(PREFIX_REPO).equals(ed.getEnvironmentName())) {
			resources.setEnvironment(StackEnvironmentType.REPO, ed);
		} else if (! config.getEnvironmentName(PREFIX_WORKERS).equals(ed.getEnvironmentName())) {
			resources.setEnvironment(StackEnvironmentType.WORKERS, ed);
		} else {
			resources.setEnvironment(StackEnvironmentType.PORTAL, ed);
		}
	}

	/**
	 * Setup the Role, policy and profile needed for automatic log rolling.
	 * Note the roleName is the same as the policy name.
	 * 
	 */
	private void configureInstanceProfileForLogRolingToS3() {
		// Need to grant the EC2 instances access to S3 so our logs can be rotated
		String roleName = config.getElasticBeanstalkS3RoleName();
		try{
			// Try to get the role, if it does not exist then an exception will be thrown.
			aimClient.getRole(new GetRoleRequest().withRoleName(roleName));
		}catch (NoSuchEntityException e){
			// This means the role does not exist so we must create it.
			aimClient.createRole(new CreateRoleRequest().withRoleName(roleName).withAssumeRolePolicyDocument(AssumeRolePolicyDocument));
		}
		// Set the role policy
		aimClient.putRolePolicy(new PutRolePolicyRequest().withRoleName(roleName).withPolicyDocument(ROLE_POLICY).withPolicyName("AdminAccessToS3"));
		// Create an instance profile with the same name as the role.
		try{
			// Check to see if it already exists
			aimClient.getInstanceProfile(new GetInstanceProfileRequest().withInstanceProfileName(roleName));
		}catch(NoSuchEntityException e){
			// this means it did not exist so we must create it.
			aimClient.createInstanceProfile(new CreateInstanceProfileRequest().withInstanceProfileName(roleName));
			// Add the policy to the role
			aimClient.addRoleToInstanceProfile(new AddRoleToInstanceProfileRequest().withRoleName(roleName).withInstanceProfileName(roleName));
		}
	}

	/*
	 * Terminate the environments
	 */
	public void terminateAllEnvironments() {
		this.terminateEnvironment(PREFIX_PORTAL);
		this.terminateEnvironment(PREFIX_REPO);
		this.terminateEnvironment(PREFIX_WORKERS);
//		this.deleteConfigurationTemplate();
	}
	/**
	 * Create or get the Configuration template
	 * @return
	 */
	public DescribeConfigurationOptionsResult createOrUpdateConfigurationTemplate(final String templateName, final List<ConfigurationOptionSetting> cfgOptSettings) {
		// Add SSL arn based on templateSuffix
		DescribeConfigurationOptionsResult desc = describeConfigurationTemplate(templateName);
		if(desc == null){
			logger.debug("Creating Elastic Beanstalk Template for the first time with name: "+templateName+"...");
			// We need to create it
			CreateConfigurationTemplateRequest request = new CreateConfigurationTemplateRequest();
			request.setApplicationName(config.getElasticBeanstalkApplicationName());
			request.setTemplateName(templateName);
			request.setSolutionStackName(Constants.SOLUTION_STACK_NAME_64BIT_TOMCAT7_JAVA7_2016_03_AMI);
			request.setOptionSettings(cfgOptSettings);
			beanstalkClient.createConfigurationTemplate(request);
		}else{

			logger.debug("Elastic Beanstalk Template already exists so updating it with name: "+templateName+"...");
			// If it exists then we want to update it
			UpdateConfigurationTemplateRequest request = new UpdateConfigurationTemplateRequest();
			request.setApplicationName(config.getElasticBeanstalkApplicationName());
			request.setTemplateName(templateName);
			request.setOptionSettings(cfgOptSettings);
			UpdateConfigurationTemplateResult updateResult = beanstalkClient.updateConfigurationTemplate(request);
			logger.debug(updateResult);
		}
		return describeConfigurationTemplate(templateName);
	}
	

	public void deleteConfigurationTemplate(final String templateName) {
		DescribeConfigurationOptionsResult desc = describeConfigurationTemplate(templateName);
		if (desc != null) {
			DeleteConfigurationTemplateRequest req = new DeleteConfigurationTemplateRequest();
			req.setApplicationName(config.getElasticBeanstalkApplicationName());
			req.setTemplateName(templateName);
			beanstalkClient.deleteConfigurationTemplate(req);
		}
	}

	/**
	 * Get the description if it exists.
	 * @return
	 */
	public DescribeConfigurationOptionsResult describeConfigurationTemplate(final String templateName){
		try{
			DescribeConfigurationOptionsResult results = beanstalkClient.describeConfigurationOptions(new DescribeConfigurationOptionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withTemplateName(templateName));
			return results;
		} catch (AmazonServiceException e){
			if("InvalidParameterValue".equals(e.getErrorCode())){
				return null;
			}else{
				throw e;
			}
		}
	}
	
	/**
	 * Get the description if it exists.
	 * @return
	 */
	public EnvironmentDescription describeEnvironment(String environmentName){
		try{
			DescribeEnvironmentsResult results = beanstalkClient.describeEnvironments(new DescribeEnvironmentsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withEnvironmentNames(environmentName));
			if(results.getEnvironments().size() < 1) return null;
			// Find a non-terminated environment
			for(EnvironmentDescription env: results.getEnvironments()){
//				logger.debug(String.format("Found environment with name: '%1$s' and status: '%2$s'", environmentName, env.getStatus()));
				if("Terminated".equals(env.getStatus()) || "Terminating".equals(env.getStatus())){
					// Cannot use a terminated environment
					continue;
				}else{
					return env;
				}
			}
			// No match found
			return null;
		}catch (AmazonServiceException e){
			if("InvalidParameterValue".equals(e.getErrorCode())){
				return null;
			}else{
				throw e;
			}
		}
	}
	
	/**
	 * Create a single environment
	 * @param version
	 * @return 
	 */
	public Future<EnvironmentDescription> createOrUpdateEnvironment(final String servicePrefix, final String cfgTemplateName, final ApplicationVersionDescription version){
		final String environmentName = config.getEnvironmentName(servicePrefix);
		final String environmentCNAME = config.getEnvironmentCNAMEPrefix(servicePrefix);
		// This work is done on a separate thread.
		Callable<EnvironmentDescription> worker = new Callable<EnvironmentDescription>() {
			public EnvironmentDescription call() throws Exception {
				EnvironmentDescription environment = describeEnvironment(environmentName);
				if(environment == null){
					// Create it since it does not exist
					logger.debug(String.format("Creating environment name: '%1$s' with CNAME: '%2$s' ",environmentName, environmentCNAME));
					CreateEnvironmentRequest cer = new CreateEnvironmentRequest(resources.getRepoApplicationVersion().getApplicationName(), environmentName);
					cer.setTemplateName(cfgTemplateName);
					cer.setVersionLabel(version.getVersionLabel());
					cer.setCNAMEPrefix(environmentCNAME);
					// Query for it again
					beanstalkClient.createEnvironment(cer);
					environment = describeEnvironment(environmentName);
					logger.debug(environment);
					return environment;
				}else{
					// Note: No support for upgrading the environment, should deploy new stack instead
					// Code deploys are OK
					
					logger.debug("Environment already exists: "+environmentName+" updating it...");

					// Should we update the version?
					if(!environment.getVersionLabel().equals(version.getVersionLabel())){
						logger.debug("Environment version need to be updated for: "+environmentName+"... updating it...");
						// Now update the version.
						updateEnvironmentVersionOnly(environmentName, version, environment);
					}else{
						// Force a template change
//						updateEnvironmentTemplateOnly(environmentName, version, environment);
					}
					
					// Return the new information.
					environment = describeEnvironment(environmentName);
					logger.debug(environment);
					return environment;
				}
			}
		};
		// Start the worker.
		return completionSvc.submit(worker);
	}
	
	/**
	 * Describe Configuration Settings
	 * @param applicationName
	 * @param environmentName
	 * @return
	 */
	public ConfigurationSettingsDescription describeConfigurationSettings(String applicationName, String environmentName){
		DescribeConfigurationSettingsResult results = beanstalkClient.describeConfigurationSettings(new DescribeConfigurationSettingsRequest().withApplicationName(applicationName).withEnvironmentName(environmentName));
		if(results != null && results.getConfigurationSettings() != null && results.getConfigurationSettings().size() == 1){
			return results.getConfigurationSettings().get(0);
		}
		return null;
	}


	/**
	 * @param environmentName
	 * @param version
	 * @param environment
	 */
	public void updateEnvironmentVersionOnly(String environmentName, ApplicationVersionDescription version,
			EnvironmentDescription environment) {
		// wait for environment to be ready after this change.
		waitForEnvironmentReady(environmentName);
		// The second pass we update the environment template.
		UpdateEnvironmentRequest uer = new UpdateEnvironmentRequest();
		uer.setEnvironmentId(environment.getEnvironmentId());
		uer.setEnvironmentName(environmentName);
		uer.setVersionLabel(version.getVersionLabel());
//		uer.setTemplateName(config.getElasticBeanstalkTemplateName());
		UpdateEnvironmentResult result =beanstalkClient.updateEnvironment(uer);

	}
	
	/**
	 * Update the template
	 * @param environmentName
	 * @param version
	 * @param environment
	 */
	public void updateEnvironmentTemplateOnly(String environmentName, ApplicationVersionDescription version,
			EnvironmentDescription environment) {
		// wait for environment to be ready after this change.
		waitForEnvironmentReady(environmentName);
		// The second pass we update the environment template.
		UpdateEnvironmentRequest uer = new UpdateEnvironmentRequest();
		uer.setEnvironmentId(environment.getEnvironmentId());
		uer.setEnvironmentName(environmentName);
//		uer.setVersionLabel(version.getVersionLabel());
		uer.setTemplateName(config.getElasticBeanstalkTemplateName());
		UpdateEnvironmentResult result =beanstalkClient.updateEnvironment(uer);
	}

	/**
	 * Wait for the Environment to be ready
	 * @throws InterruptedException 
	 */
	public void waitForEnvironmentReady(String environmentName){
		EnvironmentDescription environment = null;
		do{
			environment = describeEnvironment(environmentName);
			if(environment == null) throw new IllegalArgumentException("Environment :"+environmentName+" does not exist");
			logger.info(String.format("Waiting for Environment '%1$s' to be ready.  Status: '%2$s'", environmentName, environment.getStatus()));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}while(!"Ready".equals(environment.getStatus()));
	}
	
	/**
	 * Delete a single environment
	 */
	public void terminateEnvironment(String servicePrefix) {
		final String environmentName = config.getEnvironmentName(servicePrefix);
		final String environmentCName = config.getEnvironmentCNAMEPrefix(servicePrefix);
		EnvironmentDescription environment = describeEnvironment(environmentName);
		if (environment == null) {
			// Nothing to do except logger
			logger.debug(String.format("Environment name: '%1$s' does not exist!!!", environmentName, environmentCName));
		} else {
			// Delete environment
			logger.debug(String.format("Terminating environment name: '%1$s' with CNAME: '%2$s' ", environmentName, environmentCName));
			String environmentId = environment.getEnvironmentId();
			TerminateEnvironmentRequest ter = new TerminateEnvironmentRequest().withEnvironmentId(environmentId).withTerminateResources(Boolean.TRUE);
			TerminateEnvironmentResult terminateResult = beanstalkClient.terminateEnvironment(ter);
		}
	}
	/**
	 * Get all options
	 * @return
	 * @throws IOException 
	 */
	public List<ConfigurationOptionSetting> getAllElasticBeanstalkOptions(StackEnvironmentType env) {
		List<ConfigurationOptionSetting> list = new LinkedList<ConfigurationOptionSetting>();
		// Load the properties 
		Properties rawConfig = InputConfiguration.loadPropertyFile(Constants.ELASTIC_BEANSTALK_CONFIG_PROP_FILE_NAME);
		rawConfig = config.createFilteredProperties(rawConfig);
		// Process the keys
		logger.debug("Building the following ConfigurationOptionSetting list...");
		for(String key: rawConfig.stringPropertyNames()){
			String value = rawConfig.getProperty(key);
			// Process the key
			// The last '.' is the start of the name, everything before the last '.' is the namespaces.
			String[] split = key.split("\\.");
			StringBuilder builder = new StringBuilder();
			for(int i=0; i<split.length-1; i++){
				if(i != 0){
					builder.append(":");
				}
				builder.append(split[i]);
			}
			String nameSpace = builder.toString();
			// Name can contain spaces and colons both of which where replaced in the property file.
			// Replace all '-' with space and all '?" with colons.
			String nameRaw = split[split.length-1];
			nameRaw = nameRaw.replaceAll("-", " ");
			// What is left is the name
			String name = nameRaw.replaceAll("\\?", ":");
			// We override some of the auto-scaling values for production.
			if(config.isProductionStack()){
				if("aws:autoscaling:asg".equals(nameSpace)){
					// We need a minimum of two instances for production.
					if("MinSize".equals(name)){
						if(Long.parseLong(value) < 8){
							logger.debug("Overriding aws.autoscaling.asg.MinSize for production to be at least 4");
							value = "4";
							if ((env.equals(StackEnvironmentType.REPO)) || (env.equals(StackEnvironmentType.WORKERS))) {
								value = "8";
							}
						}
					}
					if("MaxSize".equals(name)){
						if(Long.parseLong(value) < 12){
							logger.debug("Overriding aws.autoscaling.asg.MaxSize for production to be at least 8");
							value = "8";
							if ((env.equals(StackEnvironmentType.REPO)) || (env.equals(StackEnvironmentType.WORKERS))) {
								value = "12";
							}
						}
					}
					// We want our two instances to be in any two zones. See PLFM-1560
					if("Availability Zones".equals(name)){
						if(!"Any 2".equals(value)){
							logger.debug("Overriding aws.autoscaling.asg.Availability-Zones for production to be at least 'Any 2'");
							value = "Any 2";
						}
					}
				}
			}
			// Override health check URL for plfm
			if ("aws.elasticbeanstalk.application.Application-Healthcheck-URL".equals(key)) {
				if (env.equals(StackEnvironmentType.REPO)) {
					logger.debug("Overriding aws.elasticbeanstalk.application.Application Healthcheck URL to '/repo/v1/version'");
					value = "/repo/v1/version";
				}
			}
			// The SNS topic now dependendent on the environment
			if ("aws.elasticbeanstalk.sns.topics.Notification-Topic-Name".equals(key)) {
				value = config.getEnvironmentInstanceNotificationTopicName(env);
			}

			ConfigurationOptionSetting config = new ConfigurationOptionSetting(nameSpace, name, value);
			list.add(config);
			logger.debug(config);
		}
		// For production we need one more configuration added. See PLFM-1571
		if(config.isProductionStack()){
			ConfigurationOptionSetting cfg;
			cfg = new ConfigurationOptionSetting("aws:autoscaling:asg", "Custom Availability Zones", "us-east-1c, us-east-1e");
			list.add(cfg);
		}
		// Add ACM cert ARNs based on templateSuffix
		String arn = resources.getACMCertificateArn(env);
		list.add(new ConfigurationOptionSetting("aws:elb:loadbalancer", "SSLCertificateId", arn));
		
		return list;
	}
	
	/**
	 * Are all of the expected cfgOptionSettings equals to the current cfgOptionSettings?
	 * @param one
	 * @param two
	 * @return
	 */
	public static boolean areExpectedSettingsEquals(List<ConfigurationOptionSetting> expected, List<ConfigurationOptionSetting> current){
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), current);
		
			if(found == null) {
				logger.debug("Null for "+expectedCon);
				return false;
			}
			if(!expectedCon.getValue().equals(found.getValue())){
				logger.debug("Expected: "+expectedCon+" but found "+found);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Helper to find a configuration with a given namepaces and option name.
	 * @param namespace
	 * @param name
	 * @param list
	 * @return
	 */
	public static ConfigurationOptionSetting find(String namespace, String name, List<ConfigurationOptionSetting> list){
		for(ConfigurationOptionSetting config: list){
			if(config.getNamespace().equals(namespace) && config.getOptionName().equals(name)) return config;
		}
		return null;
	}
	
	/**
	 * Get the MD5 of the configuration.
	 * 
	 * @return
	 */
	public static String createConfigMD5(List<ConfigurationOptionSetting> config){
		try {
			String string = config.toString();
			byte[] bytesOfMessage;
			bytesOfMessage = string.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(bytesOfMessage);
			return new String(Hex.encodeHex(thedigest));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
}
