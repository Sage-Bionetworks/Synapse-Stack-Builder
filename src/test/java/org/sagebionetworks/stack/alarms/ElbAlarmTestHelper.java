package org.sagebionetworks.stack.alarms;

import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static org.sagebionetworks.stack.Constants.DIMENSION_NAME_LOAD_BALANCER;
import static org.sagebionetworks.stack.Constants.FIVE_MINUTES_IN_SECONDS;
import static org.sagebionetworks.stack.Constants.METRIC_UNHEALTHY_COUNT;
import static org.sagebionetworks.stack.Constants.NAMESPACE_ELB;
import static org.sagebionetworks.stack.Constants.STATISTIC_MAX;

public class ElbAlarmTestHelper {
	
	public static PutMetricAlarmRequest getExpectedBasePutMetricAlarmRequest() {
		final String expectedDesc = "Setup by Stack Builder: "+ElbAlarmSetup.class.getName();
		final boolean expectedActionsEnabled = true;
		final Collection<String> expectedAlarmActions = new ArrayList<String>();
		expectedAlarmActions.add("topicArn");
		final String expectedNameSpace = NAMESPACE_ELB;
		Dimension expectedDimension = new Dimension().withName(DIMENSION_NAME_LOAD_BALANCER).withValue("loadBalancer");
		Collection<Dimension> expectedDimensions = new ArrayList<Dimension>();
		expectedDimensions.add(expectedDimension);

		PutMetricAlarmRequest expectedReq = new PutMetricAlarmRequest();
		expectedReq.setAlarmDescription(expectedDesc);
		expectedReq.setActionsEnabled(expectedActionsEnabled);
		expectedReq.setAlarmActions(expectedAlarmActions);
		expectedReq.setNamespace(expectedNameSpace);
		expectedReq.setDimensions(expectedDimensions);
		
		return expectedReq;
	}
	
	public static PutMetricAlarmRequest getExpectedPutMetricAlarmRequest() {
		final String prefix = "prefix";
		final String expectedAlarmName = prefix + "-unlhealthy-instance-count-alarm";
		PutMetricAlarmRequest expectedReq = ElbAlarmTestHelper.getExpectedBasePutMetricAlarmRequest();
		expectedReq.setAlarmName(expectedAlarmName);
		expectedReq.setStatistic(STATISTIC_MAX);
		expectedReq.setMetricName(METRIC_UNHEALTHY_COUNT);
		expectedReq.setComparisonOperator(ComparisonOperator.GreaterThanThreshold);
		expectedReq.setThreshold(new Double(0));
		expectedReq.setEvaluationPeriods(2);
		expectedReq.setPeriod(FIVE_MINUTES_IN_SECONDS);
		
		return expectedReq;
	
	}
	
	public static DescribeAlarmsRequest getExpectedDescribeAlarmsRequest() {
		DescribeAlarmsRequest req = new DescribeAlarmsRequest();
		req.setAlarmNamePrefix("prefix");
		req.setMaxRecords(100);
		req.setActionPrefix("topicArn");
		return req;
	}
	
	public static List<MetricAlarm> getExpectedMetricAlarms(String envName, String lbName, String topicArn) {
		List<MetricAlarm> l = new ArrayList<>();
		MetricAlarm a = new MetricAlarm().withAlarmActions(topicArn).withAlarmName(envName + "-unlhealthy-instance-count-alarm");
		l.add(a);
		return l;
	}
}
