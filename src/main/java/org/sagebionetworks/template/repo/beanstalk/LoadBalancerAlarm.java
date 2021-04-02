package org.sagebionetworks.template.repo.beanstalk;

import java.util.Objects;

import org.sagebionetworks.template.Constants;

import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.Statistic;

/**
 * Simple DTO that describe an alarm on the load balancer for the EB environment
 */
public class LoadBalancerAlarm {

	private String name;
	private String description;
	private String metric;
	private Statistic statistic;
	private Integer period;
	private Integer evaluationPeriods;
	private Double threshold;
	private ComparisonOperator comparisonOperator;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getResourceName() {
		return Constants.createCamelCaseName(name, "-");
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public Statistic getStatistic() {
		return statistic;
	}

	public void setStatistic(Statistic statistic) {
		this.statistic = statistic;
	}

	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public Integer getEvaluationPeriods() {
		return evaluationPeriods;
	}

	public void setEvaluationPeriods(Integer evaluationPeriods) {
		this.evaluationPeriods = evaluationPeriods;
	}

	public Double getThreshold() {
		return threshold;
	}

	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}

	public ComparisonOperator getComparisonOperator() {
		return comparisonOperator;
	}

	public void setComparisonOperator(ComparisonOperator comparisonOperator) {
		this.comparisonOperator = comparisonOperator;
	}

	@Override
	public int hashCode() {
		return Objects.hash(comparisonOperator, description, evaluationPeriods, metric, name, period, statistic, threshold);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LoadBalancerAlarm other = (LoadBalancerAlarm) obj;
		return comparisonOperator == other.comparisonOperator && Objects.equals(description, other.description)
				&& Objects.equals(evaluationPeriods, other.evaluationPeriods) && Objects.equals(metric, other.metric)
				&& Objects.equals(name, other.name) && Objects.equals(period, other.period) && statistic == other.statistic
				&& Objects.equals(threshold, other.threshold);
	}

}
