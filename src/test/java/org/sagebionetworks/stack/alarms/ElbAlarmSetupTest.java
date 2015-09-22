package org.sagebionetworks.stack.alarms;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import static com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionValueType.List;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentResourceDescription;
import com.amazonaws.services.elasticbeanstalk.model.LoadBalancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.sagebionetworks.factory.MockAmazonClientFactory;
import static org.sagebionetworks.stack.Constants.DIMENSION_NAME_LOAD_BALANCER;
import static org.sagebionetworks.stack.Constants.FIVE_MINUTES_IN_SECONDS;
import static org.sagebionetworks.stack.Constants.METRIC_UNHEALTHY_COUNT;
import static org.sagebionetworks.stack.Constants.NAMESPACE_ELB;
import static org.sagebionetworks.stack.Constants.STATISTIC_MAX;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.TestHelper;
import org.sagebionetworks.stack.config.InputConfiguration;

public class ElbAlarmSetupTest {
	
	private InputConfiguration config;
	private GeneratedResources resources;
	private final MockAmazonClientFactory mockFactory  = new MockAmazonClientFactory();
	private ElbAlarmSetup setup;
	private AWSElasticBeanstalkClient beanstalkClient;
	private AmazonCloudWatchClient mockCwClient;
	private PutMetricAlarmRequest expectedPutMetricAlarmRequest;
	
	public ElbAlarmSetupTest() {
	}
	
	@Before
	public void setUp() throws Exception{
		//	Config
		config = TestHelper.createTestConfig("dev");
		//	SNS topic
		resources = new GeneratedResources();
		resources.setRdsAlertTopicArn("topicArn");
		//	Beanstalk environments
		EnvironmentDescription repoEnvDesc = new EnvironmentDescription().withEnvironmentName("repoEnvName");
		resources.setRepositoryEnvironment(repoEnvDesc);
		EnvironmentDescription workersEnvDesc = new EnvironmentDescription().withEnvironmentName("workersEnvName");
		resources.setWorkersEnvironment(workersEnvDesc);
		EnvironmentDescription portalEnvDesc = new EnvironmentDescription().withEnvironmentName("portalEnvName");
		resources.setPortalEnvironment(portalEnvDesc);
		//	Clients
		beanstalkClient = mockFactory.createBeanstalkClient();
		mockCwClient = mockFactory.createCloudWatchClient();

		setup = new ElbAlarmSetup(mockFactory, config, resources);
	}
	
	@After
	public void tearDown() {
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetLoadBalancerFromEnvironmentNameNullName() {
		setup.getLoadBalancerFromEnvironmentName(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetLoadBalancerFromEnvironmentNameNullLoadBalancers() {
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription();
		DescribeEnvironmentResourcesResult expectedRes = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedRes);
		setup.getLoadBalancerFromEnvironmentName("repoEnvName");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetLoadBalancerFromEnvironmentNameNoLoadBalancer() {
		//	Return empty list
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription().withLoadBalancers(new ArrayList<LoadBalancer>());
		DescribeEnvironmentResourcesResult expectedRes = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedRes);
		setup.getLoadBalancerFromEnvironmentName("repoEnvName");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetLoadBalancerFromEnvironmentNameTwoLoadBalancers() {
		//	Return 2 load balancers
		List<LoadBalancer> loadBalancers = new ArrayList<>();
		LoadBalancer lb = new LoadBalancer();
		loadBalancers.add(lb);
		loadBalancers.add(lb);
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription().withLoadBalancers(loadBalancers);
		DescribeEnvironmentResourcesResult expectedRes = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedRes);
		setup.getLoadBalancerFromEnvironmentName("repoEnvName");
	}
	
	@Test
	public void testGetLoadBalancerFromEnvironmentNameOneLoadBalancer() {
		//	Return 1 load balancers
		List<LoadBalancer> loadBalancers = new ArrayList<>();
		LoadBalancer lb = new LoadBalancer().withName("loadBalancer");
		loadBalancers.add(lb);
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription().withLoadBalancers(loadBalancers);
		DescribeEnvironmentResourcesResult expectedRes = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedRes);
		LoadBalancer b = setup.getLoadBalancerFromEnvironmentName("repoEnvName");
		//	Should have same name
		assertEquals(lb.getName(), b.getName());
	}
	
	//	TODO: Add tests for error cases
	
	@Test
	public void testCreateDefaultPutMetricAlarmRequest() {
		PutMetricAlarmRequest expectedReq = ElbAlarmTestHelper.getExpectedBasePutMetricAlarmRequest();
		
		LoadBalancer loadBalancer = new LoadBalancer().withName("loadBalancer");

		PutMetricAlarmRequest req = ElbAlarmSetup.createDefaultPutMetricAlarmRequest(loadBalancer, resources.getRdsAlertTopicArn());
		
		assertEquals(expectedReq, req);
	}
	
	@Test
	public void testCreateUnhealthyInstancesPutMetricAlarmRequest() {
		PutMetricAlarmRequest expectedReq = ElbAlarmTestHelper.getExpectedPutMetricAlarmRequest();
		
		LoadBalancer loadBalancer = new LoadBalancer().withName("loadBalancer");

		PutMetricAlarmRequest req = ElbAlarmSetup.createUnhealthyInstancesPutMetricAlarmRequest("prefix", loadBalancer, resources.getRdsAlertTopicArn());
		
		assertEquals(expectedReq, req);
	}

	@Test
	public void testCreateAllPutMetricAlarmRequests() {
		PutMetricAlarmRequest expectedReq = ElbAlarmTestHelper.getExpectedPutMetricAlarmRequest();
		List<PutMetricAlarmRequest> expectedReqs = new ArrayList<>();
		expectedReqs.add(expectedReq);
		
		LoadBalancer loadBalancer = new LoadBalancer().withName("loadBalancer");

		List<PutMetricAlarmRequest> reqs = ElbAlarmSetup.createAllPutMetricAlarmRequests("prefix", loadBalancer, resources.getRdsAlertTopicArn());
		
		assertEquals(expectedReqs, reqs);
	}
	
	@Test
	public void testCreateDescribeAlarmsRequest() {
		DescribeAlarmsRequest expectedReq = ElbAlarmTestHelper.getExpectedDescribeAlarmsRequest();
		LoadBalancer loadBalancer = new LoadBalancer().withName("loadBalancer");
		DescribeAlarmsRequest req = ElbAlarmSetup.createDescribeAlarmsRequest("prefix", loadBalancer, resources.getRdsAlertTopicArn());
		assertEquals(expectedReq, req);
	}
	
	@Test
	public void testCreateAlarms() {
		EnvironmentResourceDescription erd = new EnvironmentResourceDescription().withLoadBalancers(new LoadBalancer().withName("loadBalancer"));
		DescribeEnvironmentResourcesResult expectedErr = new DescribeEnvironmentResourcesResult().withEnvironmentResources(erd);
		when(beanstalkClient.describeEnvironmentResources(any(DescribeEnvironmentResourcesRequest.class))).thenReturn(expectedErr);
		setup.createAlarms(resources.getRepositoryEnvironment());
		verify(mockCwClient).putMetricAlarm(any(PutMetricAlarmRequest.class));
	}
	
	
}
