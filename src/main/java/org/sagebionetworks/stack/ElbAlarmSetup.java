package org.sagebionetworks.stack;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentResourceDescription;
import com.amazonaws.services.elasticbeanstalk.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import java.util.ArrayList;
import java.util.List;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

public class ElbAlarmSetup implements ResourceProcessor {
	
	GeneratedResources resources;
	InputConfiguration config;
	EnvironmentDescription repoEd;
	EnvironmentDescription workersEd;
	EnvironmentDescription portalEd;
	AmazonCloudWatchClient cloudWatchClient;
	AmazonElasticLoadBalancingClient loadBalancingClient;
	AWSElasticBeanstalkClient beanstalkClient;
	
	@Override
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		if(factory == null) throw new IllegalArgumentException("AmazonClientFactory cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		if (resources.getRepositoryEnvironment() == null) throw new IllegalArgumentException("resources.getRepositoryEnvironment() cannot be null");
		if (resources.getWorkersEnvironment() == null) throw new IllegalArgumentException("resources.getWorkersEnvironment() cannot be null");
		if (resources.getPortalEnvironment() == null) throw new IllegalArgumentException("resources.getPortalEnvironment() cannot be null");
		this.resources = resources;
		this.config = config;
		repoEd = resources.getRepositoryEnvironment();
		workersEd = resources.getWorkersEnvironment();
		portalEd = resources.getPortalEnvironment();
		cloudWatchClient = factory.createCloudWatchClient();
		loadBalancingClient = factory.createElasticLoadBalancingClient();
		beanstalkClient = factory.createBeanstalkClient();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setupResources() throws InterruptedException {
		String topicArn = resources.getRdsAlertTopicArn();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void teardownResources() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	public LoadBalancer getLoadBalancerFromEnvironmentName(String envName) {
		DescribeEnvironmentResourcesRequest req = new DescribeEnvironmentResourcesRequest();
		req.setEnvironmentName(envName);
		DescribeEnvironmentResourcesResult res = beanstalkClient.describeEnvironmentResources(req);
		EnvironmentResourceDescription erd = res.getEnvironmentResources();
		List<LoadBalancer> loadBalancers = erd.getLoadBalancers();
		if (loadBalancers.size() > 1) {
			throw new IllegalArgumentException("Found more than one load balancer for environment " + envName);
		}
		return loadBalancers.get(0);
	}
	
	public List<PutMetricAlarmRequest> createAllAlarmRequestsForLoadBalancer(LoadBalancer loadBalancer, String topicArn) {
		List<PutMetricAlarmRequest> l = new ArrayList<PutMetricAlarmRequest>();
		l.add(createUnhealthyInstancesAlarmForLoadBalancer(loadBalancer, topicArn));
		return l;
	}
	
	public PutMetricAlarmRequest createUnhealthyInstancesAlarmForLoadBalancer(LoadBalancer loadBalancer, String topicArn) {
		PutMetricAlarmRequest req = new PutMetricAlarmRequest();
		req.setAlarmName(loadBalancer.getName() + "-unlhealthy-instance-count-alarm");
		req.setAlarmDescription("Setup by " + ElbAlarmSetup.class);
		
		return req;
	}
}
