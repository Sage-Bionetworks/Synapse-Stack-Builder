package org.sagebionetworks.stack;

import com.amazonaws.AmazonServiceException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.UpdateConfigurationTemplateRequest;
import java.util.ArrayList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	public void testDescribeConfigurationTemplateNonExistingtTemplate() {
		DescribeConfigurationOptionsRequest dcoReq = new DescribeConfigurationOptionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withTemplateName("nonExist");
		AmazonServiceException expectedAmznException = new AmazonServiceException("Invalid template name");
		expectedAmznException.setErrorCode("InvalidParameterValue");
		when(mockClient.describeConfigurationOptions(dcoReq)).thenThrow(expectedAmznException);
		DescribeConfigurationOptionsResult dcorExpectedRes = setup.describeConfigurationTemplate("nonExist");
		assertNull(dcorExpectedRes);
	}
	
	@Test
	public void testDescribeConfigurationTemplateExistingTemplate() {
		DescribeConfigurationOptionsRequest dcoReq = new DescribeConfigurationOptionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withTemplateName("tempExist");
		DescribeConfigurationOptionsResult expectedDcoRes = new DescribeConfigurationOptionsResult();
		when(mockClient.describeConfigurationOptions(dcoReq)).thenReturn(expectedDcoRes);
		DescribeConfigurationOptionsResult dcoRes = setup.describeConfigurationTemplate("tempExist");
		assertNotNull(dcoRes);
	}
	
	@Test
	public void testCreateConfigurationTemplate() {
		List<ConfigurationOptionSetting> cfgOptSettings = new ArrayList<ConfigurationOptionSetting> ();
		String templateName = "newTemplate";
		DescribeConfigurationOptionsRequest dcoReq = new DescribeConfigurationOptionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withTemplateName(templateName);
		AmazonServiceException expectedAmznException = new AmazonServiceException("Invalid template name");
		expectedAmznException.setErrorCode("InvalidParameterValue");
		when(mockClient.describeConfigurationOptions(dcoReq)).thenThrow(expectedAmznException);
		CreateConfigurationTemplateRequest expectedCctReq = new CreateConfigurationTemplateRequest();
		expectedCctReq.setApplicationName(config.getElasticBeanstalkApplicationName());
		expectedCctReq.setOptionSettings(cfgOptSettings);
		expectedCctReq.setSolutionStackName(Constants.SOLUTION_STACK_NAME_64BIT_TOMCAT_7);
		expectedCctReq.setTemplateName(templateName);
		setup.createOrUpdateConfigurationTemplate(templateName, cfgOptSettings);
		verify(mockClient).createConfigurationTemplate(expectedCctReq);
	}
	
	@Test
	public void testUpdateConfigurationTemplate() {
		List<ConfigurationOptionSetting> cfgOptSettings = new ArrayList<ConfigurationOptionSetting> ();
		String templateName = "existingTemplate";
		DescribeConfigurationOptionsRequest dcoReq = new DescribeConfigurationOptionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withTemplateName(templateName);
		DescribeConfigurationOptionsResult expectedDcoRes = new DescribeConfigurationOptionsResult();
		when(mockClient.describeConfigurationOptions(dcoReq)).thenReturn(expectedDcoRes);
		UpdateConfigurationTemplateRequest expectedUctReq = new UpdateConfigurationTemplateRequest();
		expectedUctReq.setApplicationName(config.getElasticBeanstalkApplicationName());
		expectedUctReq.setOptionSettings(cfgOptSettings);
		expectedUctReq.setTemplateName(templateName);
		setup.createOrUpdateConfigurationTemplate(templateName, cfgOptSettings);
		verify(mockClient).updateConfigurationTemplate(expectedUctReq);
	}
	
//	@Test
//	public void testCreateEnvironment() {
//		//TODO: All services
//		String svcPrefix = Constants.PREFIX_AUTH;
//		ApplicationVersionDescription appVersionDesc = resources.getAuthApplicationVersion();
//		String genericElbTemplateName = config.getElasticBeanstalkTemplateName() + "-generic";
//		List<ConfigurationOptionSetting> cfgOptSettings = setup.getAllElasticBeanstalkOptions("generic");
//		resources.setElasticBeanstalkConfigurationTemplate("generic", setup.createOrUpdateConfigurationTemplate(genericElbTemplateName, cfgOptSettings));
//		setup.createOrUpdateEnvironment(svcPrefix, genericElbTemplateName, appVersionDesc);
//	}
//	
	@Test(expected = IllegalArgumentException.class)
	public void testGetAllElasticBeanstalkOptionsInvalidSuffix() {
		setup.getAllElasticBeanstalkOptions("badSuffix");
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
		
		// From the container tab.
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:container:tomcat:jvmoptions").withOptionName("Xmx").withValue("1536m"));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("AWS_ACCESS_KEY_ID").withValue(config.getAWSAccessKey()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("AWS_SECRET_KEY").withValue(config.getAWSSecretKey()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("PARAM1").withValue(config.getStackConfigurationFileURL()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("PARAM2").withValue(config.getEncryptionKey()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("PARAM3").withValue(config.getStack()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application:environment").withOptionName("PARAM4").withValue(config.getStackInstance()));
		
		// Check if the SSLCertificateID is correctly added for "plfm' and "portal" cases
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elb:loadbalancer").withOptionName("SSLCertificateId").withValue(resources.getSslCertificate("plfm").getArn()));
		// Also check if healthcheck url has been overriden
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application").withOptionName("Application Healthcheck URL").withValue("/repo/v1/version"));
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions("plfm");
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
		// Change the expected values for portal
		expected.remove(expected.size()-1);
		expected.remove(expected.size()-1);
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elb:loadbalancer").withOptionName("SSLCertificateId").withValue(resources.getSslCertificate("portal").getArn()));
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:elasticbeanstalk:application").withOptionName("Application Healthcheck URL").withValue("/"));
		result = setup.getAllElasticBeanstalkOptions("portal");
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
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions("plfm");
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
		// Now try for bridge, should behave the same
		result = setup.getAllElasticBeanstalkOptions("bridge");
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
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:asg").withOptionName("MinSize").withValue("4"));
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions("plfm");
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
	}
	
	@Test
	public void testMinAutoScaleSizeProductionBridge() throws IOException{
		List<ConfigurationOptionSetting> expected = new LinkedList<ConfigurationOptionSetting>(); 
		// For prod the min should be 2
		config = TestHelper.createTestConfig("prod");
		resources = TestHelper.createTestResources(config);
		setup = new ElasticBeanstalkSetup(factory, config, resources);
		// From the server tab
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:asg").withOptionName("MinSize").withValue("1"));
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions("bridge");
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
	}
	
	/**
	 * This is a test for PLFM-1560.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAutoScaleMultiZoneProduction() throws IOException{
		List<ConfigurationOptionSetting> expected = new LinkedList<ConfigurationOptionSetting>(); 
		// For prod the min should be 2
		config = TestHelper.createTestConfig("prod");
		resources = TestHelper.createTestResources(config);
		setup = new ElasticBeanstalkSetup(factory, config, resources);
		// From the server tab
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:asg").withOptionName("Availability Zones").withValue("Any 2"));
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions("plfm");
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
	}
	
	@Test
	public void testAutoScaleMultiZoneProductionBridge() throws IOException{
		List<ConfigurationOptionSetting> expected = new LinkedList<ConfigurationOptionSetting>(); 
		// For prod the min should be 2
		config = TestHelper.createTestConfig("prod");
		resources = TestHelper.createTestResources(config);
		setup = new ElasticBeanstalkSetup(factory, config, resources);
		// From the server tab
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:asg").withOptionName("Availability Zones").withValue("Any 1"));
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions("bridge");
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
	}
	
	/**
	 * This is a test for PLFM-1571.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCustomAvailabilityZonesProduction() throws IOException{
		List<ConfigurationOptionSetting> expected = new LinkedList<ConfigurationOptionSetting>(); 
		// For prod the min should be 2
		config = TestHelper.createTestConfig("prod");
		resources = TestHelper.createTestResources(config);
		setup = new ElasticBeanstalkSetup(factory, config, resources);
		// From the server tab
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:asg").withOptionName("Custom Availability Zones").withValue("us-east-1a, us-east-1e"));
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions("plfm");
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
	}
	
	@Test
	public void testCustomAvailabilityZonesProductionBridge() throws IOException{
		List<ConfigurationOptionSetting> expected = new LinkedList<ConfigurationOptionSetting>(); 
		// For prod the min should be 2
		config = TestHelper.createTestConfig("prod");
		resources = TestHelper.createTestResources(config);
		setup = new ElasticBeanstalkSetup(factory, config, resources);
		// From the server tab
		expected.add(new ConfigurationOptionSetting().withNamespace("aws:autoscaling:asg").withOptionName("Custom Availability Zones").withValue("us-east-1d"));
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions("bridge");
		// Make sure we can find all of the expected values
		for(ConfigurationOptionSetting expectedCon: expected){
			ConfigurationOptionSetting found = find(expectedCon.getNamespace(), expectedCon.getOptionName(), result);
			assertNotNull("Failed to find expected configuration: "+expectedCon,found);
			assertEquals("Values did not match for namespace: "+expectedCon.getNamespace()+" and option name: "+expectedCon.getOptionName(),expectedCon.getValue(), found.getValue());
		}
	}
	
	@Test
	public void testMD5(){
		List<ConfigurationOptionSetting> result = setup.getAllElasticBeanstalkOptions("plfm");
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
		List<ConfigurationOptionSetting> one = setup.getAllElasticBeanstalkOptions("plfm");
		List<ConfigurationOptionSetting> two = setup.getAllElasticBeanstalkOptions("plfm");
		// Add some setting to the second that are not in the first.
		two.add(new ConfigurationOptionSetting("ns", "os", "123"));
		Collections.shuffle(one);
		Collections.shuffle(two);
		assertTrue(ElasticBeanstalkSetup.areExpectedSettingsEquals(one, two));
		// Now make a change
		one = setup.getAllElasticBeanstalkOptions("plfm");
		two = setup.getAllElasticBeanstalkOptions("plfm");
		two.get(0).setValue("some crazy value");
		Collections.shuffle(one);
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
