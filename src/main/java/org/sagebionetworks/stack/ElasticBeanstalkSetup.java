package org.sagebionetworks.stack;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentResourceDescription;
import com.amazonaws.services.elasticbeanstalk.model.RestartAppServerRequest;
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
public class ElasticBeanstalkSetup {
	
	private static Logger log = Logger.getLogger(ElasticBeanstalkSetup.class);
	
	private AWSElasticBeanstalkClient beanstalkClient;
	private InputConfiguration config;
	private GeneratedResources resources;
	
	/**
	 * The IoC constructor.
	 * 
	 * @param beanstalkClient
	 * @param config
	 * @param resources
	 */
	public ElasticBeanstalkSetup(AWSElasticBeanstalkClient client,
			InputConfiguration config, GeneratedResources resources) {
		if(client == null) throw new IllegalArgumentException("AWSElasticBeanstalkClient cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		// There are many dependencies for this setup.
		if(resources.getSslCertificate() == null) throw new IllegalArgumentException("GeneratedResources.getSslCertificate() cannot be null");
		if(resources.getAuthApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getAuthApplicationVersion() cannot be null");
		if(resources.getPortalApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getPortalApplicationVersion() cannot be null");
		if(resources.getRepoApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getReopApplicationVersion() cannot be null");
		if(resources.getAuthApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getAuthApplicationVersion() cannot be null");
		if(resources.getStackKeyPair() == null) throw new IllegalArgumentException("GeneratedResources.getStackKeyPair() cannot be null");
		this.beanstalkClient = client;
		this.config = config;
		this.resources = resources;
	}
	
	/**
	 * Create the environments
	 */
	public void createAllEnvironments(){
		// First create or update the template using the current data.
		resources.setElasticBeanstalkConfigurationTemplate(createOrUpdateConfigurationTemplate());
		// Create the environments
		// Auth
		resources.setAuthenticationEnvironment(createEnvironment(config.getAuthEnvironmentName(), config.getAuthEnvironmentCNAMEPrefix(), resources.getAuthApplicationVersion()));
		// repo
		resources.setRepositoryEnvironment(createEnvironment(config.getRepoEnvironmentName(), config.getRepoEnvironmentCNAMEPrefix(), resources.getRepoApplicationVersion()));
		// portal
		resources.setPortalEnvironment(createEnvironment(config.getPortalEnvironmentName(), config.getPortalEnvironmentCNAMEPrefix(), resources.getPortalApplicationVersion()));
	}
	
	/**
	 * Create or get the Configuration template
	 * @return
	 */
	public DescribeConfigurationOptionsResult createOrUpdateConfigurationTemplate(){
		DescribeConfigurationOptionsResult desc = describTempalteConfiguration();
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

		}
		return describTempalteConfiguration();
	}
	
	
	/**
	 * Get the description if it exists.
	 * @return
	 */
	public DescribeConfigurationOptionsResult describTempalteConfiguration(){
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
				log.debug(String.format("Found environment with name: '%1$s' and status: '%2$s'", environmentName, env.getStatus()));
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
	public EnvironmentDescription createEnvironment(String environmentName, String environmentCNAME, ApplicationVersionDescription version){
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
			UpdateEnvironmentRequest uer = new UpdateEnvironmentRequest();
			uer.setEnvironmentId(environment.getEnvironmentId());
			uer.setEnvironmentName(environmentName);
			uer.setTemplateName(config.getElasticBeanstalkTemplateName());
			uer.setVersionLabel(version.getVersionLabel());
			UpdateEnvironmentResult updateResult = beanstalkClient.updateEnvironment(uer);
			// Restart the application
//			beanstalkClient.restartAppServer(new RestartAppServerRequest().withEnvironmentId(environment.getEnvironmentId()));
			// Return the new information.
			environment = describeEnvironment(environmentName);
			log.debug(environment);
			return environment;
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
			ConfigurationOptionSetting config = new ConfigurationOptionSetting(nameSpace, name, value);
			list.add(config);
			log.debug(config);
		}
		return list;
	}	

}
