package org.sagebionetworks.stack;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import org.sagebionetworks.factory.MockAmazonClientFactory;

public class ElasticBeanstalkSetupTest {
	
	InputConfiguration config;	

	AWSElasticBeanstalkClient mockClient;
	ElasticBeanstalkSetup setup;
	GeneratedResources resources;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();

	@Before
	public void before() throws IOException {
		mockClient = factory.createBeanstalkClient();
		config = TestHelper.createTestConfig("dev");
		resources = TestHelper.createTestResources(config);
		setup = new ElasticBeanstalkSetup(factory, config, resources);
	}
	
	@Test
	public void testGetAllElasticBeanstalkOptions(){
		List<ConfigurationOptionSetting> expected = new LinkedList<ConfigurationOptionSetting>(); 
		// From the server tab
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:launchconfiguration").withOptionName("InstanceType").withValue("m1.small"));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:launchconfiguration").withOptionName("SecurityGroups").withValue(config.getElasticSecurityGroupName()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:launchconfiguration").withOptionName("EC2KeyName").withValue(config.getStackKeyPairName()));
		
		// From the load balancer tab
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elb:loadbalancer").withOptionName("LoadBalancerHTTPPort").withValue("80"));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elb:loadbalancer").withOptionName("LoadBalancerHTTPSPort").withValue("443"));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elb:loadbalancer").withOptionName("SSLCertificateId").withValue(config.geSSLCertificateARN()));
		
		// From the container tab.
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:container:tomcat:jvmoptions").withOptionName("Xmx").withValue("1536m"));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("AWS_ACCESS_KEY_ID").withValue(config.getAWSAccessKey()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("AWS_SECRET_KEY").withValue(config.getAWSSecretKey()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("PARAM1").withValue(config.getStackConfigurationFileURL()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("PARAM2").withValue(config.getEncryptionKey()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("PARAM3").withValue(config.getStack()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("PARAM4").withValue(config.getStackInstance()));
		
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions();
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
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

}
