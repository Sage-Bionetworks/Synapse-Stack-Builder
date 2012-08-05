package org.sagebionetworks.stack;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentResourceDescription;
import com.amazonaws.services.elasticbeanstalk.model.ListAvailableSolutionStacksResult;
import com.amazonaws.services.elasticbeanstalk.model.SolutionStackDescription;

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
		if(resources.getReopApplicationVersion() == null) throw new IllegalArgumentException("GeneratedResources.getReopApplicationVersion() cannot be null");
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
		String testing = "testing123";
		DescribeConfigurationOptionsResult r = beanstalkClient.describeConfigurationOptions(new DescribeConfigurationOptionsRequest().withApplicationName(testing).withEnvironmentName(testing));
		for(ConfigurationOptionDescription cos :r.getOptions()){
			log.debug(cos);
		}
		
		DescribeConfigurationSettingsResult r2 = beanstalkClient.describeConfigurationSettings(new DescribeConfigurationSettingsRequest().withApplicationName(testing).withEnvironmentName(testing));
		for(ConfigurationSettingsDescription cos :r2.getConfigurationSettings()){
			log.debug(cos);
		}
		
//		CreateConfigurationTemplateRequest request = new CreateConfigurationTemplateRequest(resources.getElasticBeanstalkApplication().getApplicationName(), config.getElasticBeanstalkTemplateName());
//		request.withSolutionStackName(Constants.SOLUTION_STACK_NAME_64BIT_TOMCAT_7);
//		request.withOptionSettings(new ConfigurationOptionSetting(namespace, optionName, value))
//		beanstalkClient.createConfigurationTemplate(request);
//		// Create each environment
//		// auth
//		createEnvironments(config.getAuthEnvironmentName(), resources.getAuthApplicationVersion());
//		// repo
//		createEnvironments(config.getRepoEnvironmentName(), resources.getReopApplicationVersion());
//		// portal
//		createEnvironments(config.getPortalEnvironmentName(), resources.getPortalApplicationVersion());
	}
	
	/**
	 * Create a single environment
	 * @param version
	 * @return 
	 */
	public EnvironmentResourceDescription createEnvironments(String environmentName, ApplicationVersionDescription version){
		
		DescribeEnvironmentResourcesResult result = beanstalkClient.describeEnvironmentResources(new DescribeEnvironmentResourcesRequest().withEnvironmentName(environmentName));
		if(result.getEnvironmentResources() == null){
			// Create it since it does not exist
			log.debug("Creating environment: "+environmentName);
			CreateEnvironmentRequest cer = new CreateEnvironmentRequest(resources.getAuthApplicationVersion().getApplicationName(), environmentName);
			//cer.withSolutionStackName(solutionStackName);
			// Query for it again
			beanstalkClient.createEnvironment(cer);
			
			result = beanstalkClient.describeEnvironmentResources(new DescribeEnvironmentResourcesRequest().withEnvironmentName(environmentName));
		}else{
			log.debug("Environment already exists: "+environmentName);
		}
		return result.getEnvironmentResources();
	}
	
	/**
	 * Get all options
	 * @return
	 * @throws IOException 
	 */
	public static List<ConfigurationOptionSetting> getAllOptions() throws IOException{
		List<ConfigurationOptionSetting> list = new LinkedList<ConfigurationOptionSetting>();
		// Load the properties 
		Properties rawConfig = InputConfiguration.loadPropertyFile(Constants.ELASTIC_BEANSTALK_CONFIG_PROP_FILE_NAME);
		// Process the keys
		for(String key: rawConfig.stringPropertyNames()){
			String value = rawConfig.getProperty(key);
			// Process the key
			// The last '.' is the start of the name, everything before the last '.' is the namespaces.
			String[] split = key.split("\\.");
			StringBuilder builder = new StringBuilder();
			for(int i=0; i<split.length-2; i++){
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
			System.out.println(config);
		}
		return list;
	}
	
	/**
	 * Test load properties 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException{
		getAllOptions();
	}
	
	
	
	
	

}
