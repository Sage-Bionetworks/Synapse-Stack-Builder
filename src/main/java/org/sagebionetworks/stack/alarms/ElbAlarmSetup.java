package org.sagebionetworks.stack.alarms;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentResourceDescription;
import com.amazonaws.services.elasticbeanstalk.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static org.sagebionetworks.stack.Constants.DIMENSION_NAME_LOAD_BALANCER;
import static org.sagebionetworks.stack.Constants.FIVE_MINUTES_IN_SECONDS;
import static org.sagebionetworks.stack.Constants.METRIC_UNHEALTHY_COUNT;
import static org.sagebionetworks.stack.Constants.NAMESPACE_ELB;
import static org.sagebionetworks.stack.Constants.STATISTIC_MAX;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.ResourceProcessor;
import org.sagebionetworks.stack.StackEnvironmentType;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;
import org.sagebionetworks.stack.util.Sleeper;
import org.sagebionetworks.stack.util.SleeperImpl;

public class ElbAlarmSetup implements ResourceProcessor {
	
	GeneratedResources resources;
	InputConfiguration config;
	EnvironmentDescription repoEd;
	EnvironmentDescription workersEd;
	EnvironmentDescription portalEd;
	AmazonCloudWatchClient cloudWatchClient;
	AWSElasticBeanstalkClient beanstalkClient;
	Sleeper sleeper;
	
	public ElbAlarmSetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources, Sleeper sleeper) {
		this.initialize(factory, config, resources);
		this.sleeper = sleeper;
	}
	
	@Override
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		if(factory == null) throw new IllegalArgumentException("AmazonClientFactory cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		for (StackEnvironmentType env: StackEnvironmentType.values()) {
			if (resources.getEnvironment(env) == null) {
				throw new IllegalStateException("All environments must be created before setting alarms up.");
			}
		}
		this.resources = resources;
		this.config = config;
		repoEd = resources.getEnvironment(StackEnvironmentType.REPO);
		workersEd = resources.getEnvironment(StackEnvironmentType.WORKERS);
		portalEd = resources.getEnvironment(StackEnvironmentType.PORTAL);
		cloudWatchClient = factory.createCloudWatchClient();
		beanstalkClient = factory.createBeanstalkClient();
	}

	@Override
	public void setupResources() throws InterruptedException {
		this.createAlarms(this.repoEd);
		this.createAlarms(this.workersEd);
		this.createAlarms(this.portalEd);
	}
	
	public void describeResources() throws InterruptedException {
		resources.setEnvironmentELBAlarms(StackEnvironmentType.REPO, this.describeAlarms(repoEd));
		resources.setEnvironmentELBAlarms(StackEnvironmentType.WORKERS, this.describeAlarms(workersEd));
		resources.setEnvironmentELBAlarms(StackEnvironmentType.PORTAL, this.describeAlarms(portalEd));
	}

	@Override
	public void teardownResources() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
		
	public void createAlarms(EnvironmentDescription ed) throws InterruptedException {
		String topicArn = resources.getStackInstanceNotificationTopicArn();
		LoadBalancer loadBalancer = getLoadBalancerFromEnvironmentName(ed.getEnvironmentName());
		List<PutMetricAlarmRequest> reqs = createAllPutMetricAlarmRequests(ed.getEnvironmentName(), loadBalancer.getName(), topicArn);
		for (PutMetricAlarmRequest req: reqs) {
			this.cloudWatchClient.putMetricAlarm(req);
		}
	}
	
	public DescribeAlarmsResult describeAlarms(EnvironmentDescription ed) throws InterruptedException {
		String topicArn = resources.getStackInstanceNotificationTopicArn();
		LoadBalancer loadBalancer = getLoadBalancerFromEnvironmentName(ed.getEnvironmentName());
		DescribeAlarmsRequest req = createDescribeAlarmsRequest(ed.getEnvironmentName(), loadBalancer.getName(), topicArn);
		DescribeAlarmsResult res = this.cloudWatchClient.describeAlarms(req);
		return res;
	}
	
	public LoadBalancer getLoadBalancerFromEnvironmentName(String envName) throws InterruptedException {
		if (envName == null) throw new IllegalArgumentException("Environment name cannot be null.");
		
		DescribeEnvironmentResourcesRequest req = new DescribeEnvironmentResourcesRequest();
		req.setEnvironmentName(envName);
		DescribeEnvironmentResourcesResult res = beanstalkClient.describeEnvironmentResources(req);
		EnvironmentResourceDescription erd = res.getEnvironmentResources();
		List<LoadBalancer> loadBalancers = erd.getLoadBalancers();
		int count = 0;
		while ((loadBalancers != null) && (loadBalancers.size() == 0)) {
			count += 1;
			if (count == 10) {
				throw new IllegalStateException("Load balancer for environment " + envName + " did not come up within 5 minutes");
			}
			sleeper.sleep(30000L);
			loadBalancers = erd.getLoadBalancers();
		}
		// In case loadBalancers was null to start with, should not happen
		if ((loadBalancers != null) && (loadBalancers.size() != 1)) {
			throw new IllegalStateException("Should only be one load balancer for environment " + envName);
		}
		return loadBalancers.get(0);
	}
	
	public static List<PutMetricAlarmRequest> createAllPutMetricAlarmRequests(String alarmNamePrefix, String loadBalancerName, String topicArn) {
		List<PutMetricAlarmRequest> l = new ArrayList<>();
		l.add(createUnhealthyInstancesPutMetricAlarmRequest(alarmNamePrefix, loadBalancerName, topicArn));
		return l;
	}
	
	public static PutMetricAlarmRequest createDefaultPutMetricAlarmRequest(String loadBalancerName, String topicArn) {
		if (loadBalancerName == null) throw new IllegalArgumentException("Load balancer name cannot be null");
		if (topicArn == null) throw new IllegalArgumentException("Topic ARN cannot be null");
		
		PutMetricAlarmRequest alarmRequest = new PutMetricAlarmRequest();
		alarmRequest.setAlarmDescription("Setup by Stack Builder: " + ElbAlarmSetup.class.getName());
		alarmRequest.setActionsEnabled(true);
		alarmRequest.withAlarmActions(topicArn);
		alarmRequest.setNamespace(NAMESPACE_ELB);
		Collection<Dimension> dimensions = new ArrayList<Dimension>();
		dimensions.add(new Dimension().withName(DIMENSION_NAME_LOAD_BALANCER).withValue(loadBalancerName));
		alarmRequest.setDimensions(dimensions);
		alarmRequest.withDimensions();
		return alarmRequest;
	}
	
	public static PutMetricAlarmRequest createUnhealthyInstancesPutMetricAlarmRequest(String prefix, String loadBalancerName, String topicArn) {
		PutMetricAlarmRequest req = createDefaultPutMetricAlarmRequest(loadBalancerName, topicArn);
		req.setAlarmName(prefix + "-unlhealthy-instance-count-alarm");
		req.setStatistic(STATISTIC_MAX);
		req.setMetricName(METRIC_UNHEALTHY_COUNT);
		req.setComparisonOperator(ComparisonOperator.GreaterThanThreshold);
		req.setThreshold(new Double(0));
		req.setEvaluationPeriods(2);
		req.setPeriod(FIVE_MINUTES_IN_SECONDS);
		return req;
	}

	public static DescribeAlarmsRequest createDescribeAlarmsRequest(String prefix, String loadBalancerName, String topicArn) {
		DescribeAlarmsRequest req = new DescribeAlarmsRequest();
		req.setAlarmNamePrefix(prefix);
		req.setActionPrefix(topicArn);
		req.setMaxRecords(100);
		return req;
	}
}
