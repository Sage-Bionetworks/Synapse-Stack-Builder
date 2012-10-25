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
import com.amazonaws.services.elasticbeanstalk.model.RestartAppServerRequest;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.UpdateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.UpdateConfigurationTemplateResult;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentResult;

/**
 * Setup the elastic beanstalk environments.
 * 
 * @author John
 *
 */
public class ElasticBeanstalkSetup implements ResourceProcessor {
	
	private static Logger log = Logger.getLogger(ElasticBeanstalkSetup.class);
	
	private AWSElasticBeanstalkClient beanstalkClient;
	private InputConfiguration config;
	private GeneratedResources resources;
	private ExecutorService executor = Executors.newFixedThreadPool(4);
	/**
	 * The IoC constructor.
	 * 
	 * @param beanstalkClient
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
		// There are many dependencies for this setup.
		if(resources.getSslCertificate() == null) throw new IllegalArgumentException("GeneratedResources.getSslCertificate() cannot be null");
		if(resources.getAuthApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getAuthApplicationVersion() cannot be null");
		if(resources.getPortalApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getPortalApplicationVersion() cannot be null");
		if(resources.getRepoApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getReopApplicationVersion() cannot be null");
		if(resources.getSearchApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getSearchApplicationVersion() cannot be null");
		if(resources.getRdsAsynchApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getRdsAsynchApplicationVersion() cannot be null");
		if(resources.getStackKeyPair() == null) throw new IllegalArgumentException("GeneratedResources.getStackKeyPair() cannot be null");
		this.beanstalkClient = factory.createBeanstalkClient();
		this.config = config;
		this.resources = resources;
	}
	
	public void setupResources() {
		this.createAllEnvironments();
	}
	
	public void teardownResources() {
		this.terminateAllEnvironments();
	}
	
	public void describeResources() {
		resources.setAuthenticationEnvironment(describeEnvironment(config.getAuthEnvironmentName()));
		resources.setPortalEnvironment(describeEnvironment(config.getPortalEnvironmentName()));
		resources.setRepositoryEnvironment(describeEnvironment(config.getRepoEnvironmentName()));
		resources.setSearchEnvironment(describeEnvironment(config.getSearchEnvironmentName()));
		resources.setRdsAsynchEnvironment(describeEnvironment(config.getRdsAsynchEnvironmentName()));
	}

	/**
	 * Create the environments
	 */
	public void createAllEnvironments(){
		// First create or update the template using the current data.
		resources.setElasticBeanstalkConfigurationTemplate(createOrUpdateConfigurationTemplate());
		// Create the environments
		// Auth
		Future<EnvironmentDescription> authFuture = createEnvironment(config.getAuthEnvironmentName(), config.getAuthEnvironmentCNAMEPrefix(), resources.getAuthApplicationVersion());
		// repo
		Future<EnvironmentDescription> repoFuture = createEnvironment(config.getRepoEnvironmentName(), config.getRepoEnvironmentCNAMEPrefix(), resources.getRepoApplicationVersion());
		// search
		Future<EnvironmentDescription> searchFuture = createEnvironment(config.getSearchEnvironmentName(), config.getSearchEnvironmentCNAMEPrefix(), resources.getSearchApplicationVersion());
		// portal
		Future<EnvironmentDescription> portalFuture = createEnvironment(config.getPortalEnvironmentName(), config.getPortalEnvironmentCNAMEPrefix(), resources.getPortalApplicationVersion());
		// The rds asynch
		Future<EnvironmentDescription> rdsFuture = createEnvironment(config.getRdsAsynchEnvironmentName(), config.getRdsAsynchEnvironmentCNAMEPrefix(), resources.getRdsAsynchApplicationVersion());
		// Fetch all of the results
		try {
			resources.setAuthenticationEnvironment(authFuture.get());
			resources.setRepositoryEnvironment(repoFuture.get());
			resources.setSearchEnvironment(searchFuture.get());
			resources.setPortalEnvironment(portalFuture.get());
			resources.setRdsAsynchEnvironment(rdsFuture.get());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * Terminate the environments
	 */
	public void terminateAllEnvironments() {
		this.terminateEnvironment(config.getAuthEnvironmentName(), config.getAuthEnvironmentCNAMEPrefix());
		this.terminateEnvironment(config.getPortalEnvironmentName(), config.getPortalEnvironmentCNAMEPrefix());
		this.terminateEnvironment(config.getRepoEnvironmentName(), config.getRepoEnvironmentCNAMEPrefix());
		this.terminateEnvironment(config.getSearchEnvironmentName(), config.getSearchEnvironmentCNAMEPrefix());
//		this.deleteConfigurationTemplate();
	}
	/**
	 * Create or get the Configuration template
	 * @return
	 */
	public DescribeConfigurationOptionsResult createOrUpdateConfigurationTemplate(){
		DescribeConfigurationOptionsResult desc = describeTemplateConfiguration();
		if(desc == null){
			log.debug("Creating Elastic Beanstalk Template for the first time with name: "+config.getElasticBeanstalkTemplateName()+"...");
			// We need to create it
			CreateConfigurationTemplateRequest request = new CreateConfigurationTemplateRequest();
			request.setApplicationName(config.getElasticBeanstalkApplicationName());
			request.setTemplateName(config.getElasticBeanstalkTemplateName());
			request.setSolutionStackName(Constants.SOLUTION_STACK_NAME_64BIT_TOMCAT_7);
			request.setOptionSettings(getAllElasticBeanstalkOptions());
			beanstalkClient.createConfigurationTemplate(request);
		}else{
			log.debug("Elastic Beanstalk Template already exists so updating it with name: "+config.getElasticBeanstalkTemplateName()+"...");
			// If it exists then we want to update it
			UpdateConfigurationTemplateRequest request = new UpdateConfigurationTemplateRequest();
			request.setApplicationName(config.getElasticBeanstalkApplicationName());
			request.setTemplateName(config.getElasticBeanstalkTemplateName());
			request.setOptionSettings(getAllElasticBeanstalkOptions());
			UpdateConfigurationTemplateResult updateResult = beanstalkClient.updateConfigurationTemplate(request);
			log.debug(updateResult);
		}
		return describeTemplateConfiguration();
	}
	

	public void deleteConfigurationTemplate() {
		DescribeConfigurationOptionsResult desc = describeTemplateConfiguration();
		if (desc != null) {
			DeleteConfigurationTemplateRequest req = new DeleteConfigurationTemplateRequest();
			req.setApplicationName(config.getElasticBeanstalkApplicationName());
			req.setTemplateName(config.getElasticBeanstalkTemplateName());
			beanstalkClient.deleteConfigurationTemplate(req);
		}
	}

	/**
	 * Get the description if it exists.
	 * @return
	 */
	public DescribeConfigurationOptionsResult describeTemplateConfiguration(){
		try{
			DescribeConfigurationOptionsResult results = beanstalkClient.describeConfigurationOptions(new DescribeConfigurationOptionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withTemplateName(config.getElasticBeanstalkTemplateName()));
			return results;
		}catch (AmazonServiceException e){
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
//				log.debug(String.format("Found environment with name: '%1$s' and status: '%2$s'", environmentName, env.getStatus()));
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
	public Future<EnvironmentDescription> createEnvironment(final String environmentName, final String environmentCNAME,  final ApplicationVersionDescription version){
		// This work is done on a separate thread.
		Callable<EnvironmentDescription> worker = new Callable<EnvironmentDescription>() {
			public EnvironmentDescription call() throws Exception {
				EnvironmentDescription environment = describeEnvironment(environmentName);
				if(environment == null){
					// Create it since it does not exist
					log.debug(String.format("Creating environment name: '%1$s' with CNAME: '%2$s' ",environmentName, environmentCNAME));
					CreateEnvironmentRequest cer = new CreateEnvironmentRequest(resources.getAuthApplicationVersion().getApplicationName(), environmentName);
					cer.setTemplateName(config.getElasticBeanstalkTemplateName());
					cer.setVersionLabel(version.getVersionLabel());
					cer.setCNAMEPrefix(environmentCNAME);
					// Query for it again
					beanstalkClient.createEnvironment(cer);
					environment = describeEnvironment(environmentName);
					log.debug(environment);
					return environment;
				}else{
					log.debug("Environment already exists: "+environmentName+" updating it...");
					// Lookup the current environment
					ConfigurationSettingsDescription csd = describeConfigurationSettings(version.getApplicationName(), environmentName);
					// do the configurations already match?
					List<ConfigurationOptionSetting> settings = getAllElasticBeanstalkOptions();
					boolean updated = false;
					// Should we update the configuration?
//					if(csd == null || !areExpectedSettingsEquals(settings, csd.getOptionSettings())){
						// First update the configuration
						log.debug("Environment configurations need to be updated for: "+environmentName+"... updating it...");
						updateConfigurationOnly(environmentName, environment);
						// An update was made.
						updated = true;
//					}
					// Should we update the version?
					if(!environment.getVersionLabel().equals(version.getVersionLabel())){
						log.debug("Environment version need to be updated for: "+environmentName+"... updating it...");
						// Now update the version.
						updateEnvironmentVersionOnly(environmentName, version, environment);
						updated = true;
					}
					// If we did not update the environment then we need to restart it.
					if(!updated){
						beanstalkClient.restartAppServer(new RestartAppServerRequest().withEnvironmentId(environment.getEnvironmentId()));
					}
					// Return the new information.
					environment = describeEnvironment(environmentName);
					log.debug(environment);
					return environment;
				}
			}
		};
		// Start the worker.
		return executor.submit(worker);
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
	 * @param environmentName
	 * @param environment
	 */
	public void updateConfigurationOnly(String environmentName,	EnvironmentDescription environment) {
		// We can only update a ready environment.
		waitForEnvironmentReady(environmentName);
		// This first pass is just to update the configuration, we do not change the version here.
		UpdateEnvironmentRequest uer = new UpdateEnvironmentRequest();
		uer.setEnvironmentId(environment.getEnvironmentId());
		uer.setEnvironmentName(environmentName);
		// We re-use the existing version for now.
		uer.setVersionLabel(environment.getVersionLabel());
		uer.setTemplateName(config.getElasticBeanstalkTemplateName());
		UpdateEnvironmentResult result = beanstalkClient.updateEnvironment(uer);
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
			log.info(String.format("Waiting for Environment '%1$s' to be ready.  Status: '%2$s'", environmentName, environment.getStatus()));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}while(!"Ready".equals(environment.getStatus()));
	}
	
	/**
	 * Delete a single environment
	 */
	public void terminateEnvironment(String environmentName, String environmentCName) {
		EnvironmentDescription environment = describeEnvironment(environmentName);
		if (environment == null) {
			// Nothing to do except log
			log.debug(String.format("Environment name: '%1$s' does not exist!!!", environmentName, environmentCName));
		} else {
			// Delete environment
			log.debug(String.format("Terminating environment name: '%1$s' with CNAME: '%2$s' ", environmentName, environmentCName));
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
	public List<ConfigurationOptionSetting> getAllElasticBeanstalkOptions() {
		List<ConfigurationOptionSetting> list = new LinkedList<ConfigurationOptionSetting>();
		// Load the properties 
		Properties rawConfig = InputConfiguration.loadPropertyFile(Constants.ELASTIC_BEANSTALK_CONFIG_PROP_FILE_NAME);
		rawConfig = config.createFilteredProperties(rawConfig);
		// Process the keys
		log.debug("Building the following ConfigurationOptionSetting list...");
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
			if("aws:autoscaling:asg".equals(nameSpace) && "MinSize".equals(name)){
				// For production we want a  value of 2
				if(config.isProductionStack()){
					if(Long.parseLong(value) < 2){
						log.debug("Overriding aws.autoscaling.asg.MinSize for production to be at least 2");
						value = "2";
					}
				}
			}
			ConfigurationOptionSetting config = new ConfigurationOptionSetting(nameSpace, name, value);
			list.add(config);
			log.debug(config);
		}
		return list;
	}
	
	/**
	 * Are all of the expected settings equals to the current settings?
	 * @param one
	 * @param two
	 * @return
	 */
	public static boolean areExpectedSettingsEquals(List<ConfigurationOptionSetting> expected, List<ConfigurationOptionSetting> current){
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), current);
		
			if(found == null) {
				log.debug("Null for "+expectedCon);
				return false;
			}
			if(!expectedCon.getValue().equals(found.getValue())){
				log.debug("Expected: "+expectedCon+" but found "+found);
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
