package org.sagebionetworks.stack;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentResourceDescription;
import com.amazonaws.services.elasticbeanstalk.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.rds.model.DBInstance;
import java.util.ArrayList;
import java.util.List;
import static org.sagebionetworks.stack.Constants.DB_INSTANCE_IDENTIFIER;
import static org.sagebionetworks.stack.Constants.ELB_INSTANCE_NAME;
import static org.sagebionetworks.stack.Constants.FIVE_MINUTES_IN_SECONDS;
import static org.sagebionetworks.stack.Constants.METRIC_UNHEALTHY_COUNT;
import static org.sagebionetworks.stack.Constants.NAME_SPACES_AWS_RDS;
import static org.sagebionetworks.stack.Constants.STATISTIC_MAX;
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
            this.createAlarms(this.repoEd);
            this.createAlarms(this.workersEd);
            this.createAlarms(this.portalEd);
	}

	@Override
	public void teardownResources() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
        
        public void createAlarms(EnvironmentDescription ed) {
            String topicArn = resources.getRdsAlertTopicArn();
            LoadBalancer loadBalancer = getLoadBalancerFromEnvironmentName(ed.getEnvironmentName());
            List<PutMetricAlarmRequest> reqs = createAllAlarmRequestsForLoadBalancer(loadBalancer, topicArn);
            for (PutMetricAlarmRequest req: reqs) {
                this.cloudWatchClient.putMetricAlarm(req);
            }
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
	
	static PutMetricAlarmRequest createDefaultPutMetricRequest(LoadBalancer loadBalancer, String topicArn) {
		PutMetricAlarmRequest alarmRequest = new PutMetricAlarmRequest();
		alarmRequest.setAlarmDescription("Setup by: "+ElbAlarmSetup.class.getName());
		alarmRequest.setActionsEnabled(true);
		alarmRequest.withAlarmActions(topicArn);
		alarmRequest.setNamespace(NAME_SPACES_AWS_RDS);
		alarmRequest.withDimensions(new Dimension().withName(ELB_INSTANCE_NAME).withValue(loadBalancer.getName()));
		return alarmRequest;
	}
	
	public PutMetricAlarmRequest createUnhealthyInstancesAlarmForLoadBalancer(LoadBalancer loadBalancer, String topicArn) {
		PutMetricAlarmRequest req = createDefaultPutMetricRequest(loadBalancer, topicArn);
		req.setAlarmName(loadBalancer.getName() + "-unlhealthy-instance-count-alarm");
		req.setStatistic(STATISTIC_MAX);
                req.setMetricName(METRIC_UNHEALTHY_COUNT);
                req.setComparisonOperator(ComparisonOperator.GreaterThanThreshold);
                req.setThreshold(new Double(0));
                req.setEvaluationPeriods(2);
                req.setPeriod(FIVE_MINUTES_IN_SECONDS);
		return req;
	}
}
