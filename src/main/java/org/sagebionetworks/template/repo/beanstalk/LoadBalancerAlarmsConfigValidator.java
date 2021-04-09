package org.sagebionetworks.template.repo.beanstalk;

import java.util.List;

import org.sagebionetworks.util.ValidateArgument;

public class LoadBalancerAlarmsConfigValidator {
	
	private LoadBalancerAlarmsConfig config;

	public LoadBalancerAlarmsConfigValidator(LoadBalancerAlarmsConfig config) {
		this.config = config;
	}
	
	public LoadBalancerAlarmsConfig validate() {
		config.values().stream().flatMap(List::stream).forEach(this::validate);
		return config;
	}
	
	private void validate(LoadBalancerAlarm alarm) {
		ValidateArgument.required(alarm, "The alarm");
		ValidateArgument.required(alarm.getName(), "The name");
		ValidateArgument.required(alarm.getDescription(), "The description");
		ValidateArgument.required(alarm.getMetric(), "The metric");
		ValidateArgument.required(alarm.getPeriod(), "The period");
		ValidateArgument.required(alarm.getEvaluationPeriods(), "The evaluationPeriods");
		ValidateArgument.required(alarm.getStatistic(), "The statistic");
		ValidateArgument.required(alarm.getThreshold(), "The threshold");
		ValidateArgument.required(alarm.getComparisonOperator(), "The comparisonOperator");
	}
	
}
