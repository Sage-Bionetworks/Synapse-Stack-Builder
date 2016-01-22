package org.sagebionetworks.stack;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.LoadBalancer;
import com.amazonaws.services.sns.AmazonSNSClient;
import java.util.List;

public class TestCloudWatch {
	public static void main(String[] args) {
		//	Clients
		ProfileCredentialsProvider credsProvider = new ProfileCredentialsProvider();
		AWSElasticBeanstalkClient beanstalkClient = new AWSElasticBeanstalkClient(credsProvider.getCredentials());
		AmazonCloudWatchClient cWclient = new AmazonCloudWatchClient(credsProvider.getCredentials());
		AmazonSNSClient snsClient = new AmazonSNSClient(credsProvider.getCredentials());
		//	
		final String envName = "repo-prod-106-0";
		DescribeEnvironmentsRequest deReq = new DescribeEnvironmentsRequest().withEnvironmentNames(envName);
		DescribeEnvironmentsResult deRes = beanstalkClient.describeEnvironments(deReq);
		DescribeEnvironmentResourcesRequest derReq = new DescribeEnvironmentResourcesRequest().withEnvironmentName(envName);
		DescribeEnvironmentResourcesResult derRes = beanstalkClient.describeEnvironmentResources(derReq);
		LoadBalancer loadBalancer = derRes.getEnvironmentResources().getLoadBalancers().get(0);
		String loadBalancerName = loadBalancer.getName();

		DescribeAlarmsRequest daReq = new DescribeAlarmsRequest();
		DescribeAlarmsResult daRes = cWclient.describeAlarms(daReq);
		List<MetricAlarm> alarms = daRes.getMetricAlarms();
		for (MetricAlarm a: alarms) { 
			System.out.println(a.getAlarmName() + "/" + a.getAlarmActions());
		}
	}
}
