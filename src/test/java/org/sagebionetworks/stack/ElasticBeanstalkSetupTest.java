package org.sagebionetworks.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;

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
	
	@Test
	public void testMinAutoScaleSizeDev() throws IOException{
		List<ConfigurationOptionSetting> expected = new LinkedList<ConfigurationOptionSetting>();
		// For dev the min should be 1
		config = TestHelper.createTestConfig("dev");
		resources = TestHelper.createTestResources(config);
		setup = new ElasticBeanstalkSetup(factory, config, resources);
		// From the server tab
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:asg").withOptionName("MinSize").withValue("1"));
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions();
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
	}
	
	@Test
	public void testMinAutoScaleSizeProduction() throws IOException{
		List<ConfigurationOptionSetting> expected = new LinkedList<ConfigurationOptionSetting>(); 
		// For prod the min should be 2
		config = TestHelper.createTestConfig("prod");
		resources = TestHelper.createTestResources(config);
		setup = new ElasticBeanstalkSetup(factory, config, resources);
		// From the server tab
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:asg").withOptionName("MinSize").withValue("2"));
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions();
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
	}
	
	@Test
	public void testMD5(){
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions();
		String md5 = ElasticBeanstalkSetup.createConfigMD5(result);
		System.out.println(md5);
		assertNotNull(md5);
		// get it again should be the same
		String md5Second = ElasticBeanstalkSetup.createConfigMD5(result);
		assertEquals(md5, md5Second);
		// Now a change should be different
		result.get(0).setValue("abdefg");
		String md5Thrid = ElasticBeanstalkSetup.createConfigMD5(result);
		System.out.println(md5Thrid);
		assertFalse(md5.equals(md5Thrid));
	}
	
	@Test
	public void testAreSettingsEquals(){
		List<ConfigurationOptionSetting> one = setup.getAllElasticBeanstalkOptions();
		List<ConfigurationOptionSetting> two = setup.getAllElasticBeanstalkOptions();
		// Add some setting to the second that are not in the first.
		two.add(new ConfigurationOptionSetting("ns", "os", "123"));
		Collections.shuffle(one);
		Collections.shuffle(two);
		assertTrue(ElasticBeanstalkSetup.areExpectedSettingsEquals(one, two));
		// Now make a change
		two.get(0).setValue("some crazy value");
		Collections.shuffle(two);
		assertFalse(ElasticBeanstalkSetup.areExpectedSettingsEquals(one, two));
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
