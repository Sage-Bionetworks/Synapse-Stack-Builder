package org.sagebionetworks.stack.alarms;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentResourceDescription;
import com.amazonaws.services.elasticbeanstalk.model.LoadBalancer;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Matchers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.StackEnvironmentType;
import org.sagebionetworks.stack.TestHelper;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.util.Sleeper;

public class ElbAlarmSetupTest {
	
	private InputConfiguration config;
	private GeneratedResources resources;
	private final MockAmazonClientFactory mockFactory  = new MockAmazonClientFactory();
	private ElbAlarmSetup setup;
	private AWSElasticBeanstalkClient beanstalkClient;
	private AmazonCloudWatchClient mockCwClient;
	private Sleeper mockSleeper;
	
	public ElbAlarmSetupTest() {
	}
	
	@Before
	public void setUp() throws Exception{
		//	Config
		config = TestHelper.createTestConfig("dev");
		//	SNS topic
		resources = new GeneratedResources();
		resources.setStackInstanceNotificationTopicArn("topicArn");
		//	Beanstalk environments
		EnvironmentDescription repoEnvDesc = new EnvironmentDescription().withEnvironmentName("repoEnvName");
		resources.setEnvironment(StackEnvironmentType.REPO, repoEnvDesc);
		EnvironmentDescription workersEnvDesc = new EnvironmentDescription().withEnvironmentName("workersEnvName");
		resources.setEnvironment(StackEnvironmentType.WORKERS, workersEnvDesc);
		EnvironmentDescription portalEnvDesc = new EnvironmentDescription().withEnvironmentName("portalEnvName");
		resources.setEnvironment(StackEnvironmentType.PORTAL, portalEnvDesc);
		//	Clients
		beanstalkClient = mockFactory.createBeanstalkClient();
		mockCwClient = mockFactory.createCloudWatchClient();
		mockSleeper = Mockito.mock(Sleeper.class);

		setup = new ElbAlarmSetup(mockFactory, config, resources, mockSleeper);
	}
	
	@After
	public void tearDown() {
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetLoadBalancerFromEnvironmentNameNullName() throws Exception {
		setup.getLoadBalancerFromEnvironmentName(null);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testGetLoadBalancerFromEnvironmentNameNullLoadBalancers() throws Exception {
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription();
		DescribeEnvironmentResourcesResult expectedRes = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedRes);
		doNothing().when(mockSleeper).sleep(anyLong());
		setup.getLoadBalancerFromEnvironmentName("repoEnvName");
	}
	
	@Test(expected=IllegalStateException.class)
	public void testGetLoadBalancerFromEnvironmentNameNoLoadBalancer() throws Exception {
		//	Return empty list
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription().withLoadBalancers(new ArrayList<LoadBalancer>());
		DescribeEnvironmentResourcesResult expectedRes = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedRes);
		doNothing().when(mockSleeper).sleep(anyLong());
		setup.getLoadBalancerFromEnvironmentName("repoEnvName");
	}
	
	@Test(expected=IllegalStateException.class)
	public void testGetLoadBalancerFromEnvironmentNameTwoLoadBalancers() throws Exception {
		//	Return 2 load balancers
		List<LoadBalancer> loadBalancers = new ArrayList<>();
		LoadBalancer lb = new LoadBalancer();
		loadBalancers.add(lb);
		loadBalancers.add(lb);
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription().withLoadBalancers(loadBalancers);
		DescribeEnvironmentResourcesResult expectedRes = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedRes);
		doNothing().when(mockSleeper).sleep(anyLong());
		setup.getLoadBalancerFromEnvironmentName("repoEnvName");
	}
	
	@Test
	public void testGetLoadBalancerFromEnvironmentNameOneLoadBalancer() throws Exception {
		//	Return 1 load balancers
		List<LoadBalancer> loadBalancers = new ArrayList<>();
		LoadBalancer lb = new LoadBalancer().withName("loadBalancer");
		loadBalancers.add(lb);
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription().withLoadBalancers(loadBalancers);
		DescribeEnvironmentResourcesResult expectedRes = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedRes);
		doNothing().when(mockSleeper).sleep(anyLong());
		LoadBalancer b = setup.getLoadBalancerFromEnvironmentName("repoEnvName");
		//	Should have same name
		assertEquals(lb.getName(), b.getName());
	}
	
	//	TODO: Add tests for error cases
	
	@Test
	public void testCreateDefaultPutMetricAlarmRequest() {
		PutMetricAlarmRequest expectedReq = ElbAlarmTestHelper.getExpectedBasePutMetricAlarmRequest();
		
		String loadBalancerName = "loadBalancer";

		PutMetricAlarmRequest req = ElbAlarmSetup.createDefaultPutMetricAlarmRequest(loadBalancerName, resources.getStackInstanceNotificationTopicArn());
		
		assertEquals(expectedReq, req);
	}
	
	@Test
	public void testCreateUnhealthyInstancesPutMetricAlarmRequest() {
		PutMetricAlarmRequest expectedReq = ElbAlarmTestHelper.getExpectedPutMetricAlarmRequest();
		
		String loadBalancerName = "loadBalancer";

		PutMetricAlarmRequest req = ElbAlarmSetup.createUnhealthyInstancesPutMetricAlarmRequest("prefix", loadBalancerName, resources.getStackInstanceNotificationTopicArn());
		
		assertEquals(expectedReq, req);
	}

	@Test
	public void testCreateAllPutMetricAlarmRequests() {
		PutMetricAlarmRequest expectedReq = ElbAlarmTestHelper.getExpectedPutMetricAlarmRequest();
		List<PutMetricAlarmRequest> expectedReqs = new ArrayList<>();
		expectedReqs.add(expectedReq);
		
		String loadBalancerName = "loadBalancer";

		List<PutMetricAlarmRequest> reqs = ElbAlarmSetup.createAllPutMetricAlarmRequests("prefix", loadBalancerName, resources.getStackInstanceNotificationTopicArn());
		
		assertEquals(expectedReqs, reqs);
	}
	
	@Test
	public void testCreateDescribeAlarmsRequest() {
		DescribeAlarmsRequest expectedReq = ElbAlarmTestHelper.getExpectedDescribeAlarmsRequest();
		String loadBalancerName = "loadBalancer";
		DescribeAlarmsRequest req = ElbAlarmSetup.createDescribeAlarmsRequest("prefix", loadBalancerName, resources.getStackInstanceNotificationTopicArn());
		assertEquals(expectedReq, req);
	}
	
	@Test
	public void testCreateAlarms() throws Exception {
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription().withLoadBalancers(new LoadBalancer().withName("loadBalancer"));
		DescribeEnvironmentResourcesResult expectedErr = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedErr);
		setup.createAlarms(resources.getEnvironment(StackEnvironmentType.REPO));
		verify(mockCwClient).putMetricAlarm(any(PutMetricAlarmRequest.class));
	}
	
	
}
