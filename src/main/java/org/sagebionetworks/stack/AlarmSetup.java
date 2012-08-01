package org.sagebionetworks.stack;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;
import static org.sagebionetworks.stack.Constants.*;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.rds.model.DBInstance;

/**
 * Setups the various cloud watch alarms.
 * 
 * @author John
 *
 */
public class AlarmSetup {
	

	private static Logger log = Logger.getLogger(AlarmSetup.class.getName());
	
	AmazonCloudWatchClient client;
	InputConfiguration config;
	GeneratedResources resources;
	
	/**
	 * 
	 * @param client
	 * @param config
	 */
	public AlarmSetup(AmazonCloudWatchClient client, InputConfiguration config, GeneratedResources resources) {
		if(client == null) throw new IllegalArgumentException("AmazonCloudWatchClient cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		if(resources.getRdsAlertTopic() == null) throw new IllegalArgumentException("GeneratedResources.getRdsAlertTopic() cannot be null");
		if(resources.getIdGeneratorDatabase() == null) throw new IllegalArgumentException("GeneratedResources.getIdGeneratorDatabase() cannot be null");
		if(resources.getStackInstancesDatabase() == null) throw new IllegalArgumentException("GeneratedResources.getStackInstancesDatabase() cannot be null");
		this.client = client;
		this.config = config;
		this.resources = resources;
	}
	
	/**
	 * Setup all alarms.
	 */
	public void setupAllAlarms(){
		// This is the topic where all alarm notification are sent
		String topicArn = resources.getRdsAlertTopic().getTopicArn();
		// setup the alarms for the id generator
		DBInstance instance = resources.getIdGeneratorDatabase();
		resources.setIdGeneratorDatabaseAlarms(createAllAlarmsForDatabase(instance, topicArn));
		// setup the alarms for the stack instances database.
		instance = resources.getStackInstancesDatabase();
		resources.setStackInstancesDatabaseAlarms(createAllAlarmsForDatabase(instance, topicArn));
	}
	
	/**
	 * Create all of the alarms for a given database.
	 * @param databaseInstancesName
	 * @return
	 */
	public List<PutMetricAlarmRequest> createAllAlarmsForDatabase(DBInstance instances, String topicArn){
		if(instances == null) throw new IllegalArgumentException("DBInstance cannot be null");
		if(topicArn == null) throw new IllegalArgumentException("Topic ARN cannot be null");
		List<PutMetricAlarmRequest> alarms = new LinkedList<PutMetricAlarmRequest>();
		// Low-Freeable-Memory alarm
		alarms.add(createLowFreeableMemory(instances, topicArn));
		// High-Write-Latency
		alarms.add(createHighWriteLatency(instances, topicArn));
		// High CPU
		alarms.add(createHighCPUUtilization(instances, topicArn));
		// Low free storage
		alarms.add(createLowFreeStorage(instances, topicArn));
		// Add all alarms from the lsit
		for(PutMetricAlarmRequest alarm: alarms){
			log.info("Creating or updateing alarm: "+alarm);
			client.putMetricAlarm(alarm);
		}
		return alarms;
	}
	
	/**
	 * @param instances
	 * @param topicArn
	 * @param alarmRequest
	 */
	static PutMetricAlarmRequest createDefaultPutMetricRequest(DBInstance instances, String topicArn) {
		PutMetricAlarmRequest alarmRequest = new PutMetricAlarmRequest();
		alarmRequest.setAlarmDescription("Setup by: "+AlarmSetup.class.getName());
		alarmRequest.setActionsEnabled(true);
		alarmRequest.withAlarmActions(topicArn);
		alarmRequest.setNamespace(NAME_SPACES_AWS_RDS);
		alarmRequest.withDimensions(new Dimension().withName(DB_INSTANCE_IDENTIFIER).withValue(instances.getDBInstanceIdentifier()));
		return alarmRequest;
	}
	
	/**
	 * Create a Low-Freeable-Memory alarm.
	 * 
	 * @param databaseInstancesName
	 * @return
	 */
	public static PutMetricAlarmRequest createLowFreeableMemory(DBInstance instances, String topicArn){
		if(instances == null) throw new IllegalArgumentException("DBInstance cannot be null");
		if(topicArn == null) throw new IllegalArgumentException("Topic ARN cannot be null");
		PutMetricAlarmRequest alarmRequest = createDefaultPutMetricRequest(instances, topicArn);
		alarmRequest.setAlarmName(instances.getDBInstanceIdentifier()+LOW_FREEABLE_MEMORY_NAME);
		Double totalMemory = Constants.getDatabaseClassMemrorySizeBytes(instances.getDBInstanceClass());
		Double eightPercent = totalMemory - (totalMemory*0.8);
		// Average FreeableMemory < 80% for 1 consecutive period of 5 minutes
		alarmRequest.withStatistic(STATISTIC_AVERAGE).withMetricName(METRIC_FREEABLE_MEMORY).withComparisonOperator(ComparisonOperator.LessThanThreshold).withThreshold(eightPercent).withEvaluationPeriods(1).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		return alarmRequest;
	}
	
	
	/**
	 * High-Write-Latency
	 * 
	 * @param databaseInstancesName
	 * @return
	 */
	public static PutMetricAlarmRequest createHighWriteLatency(DBInstance instances, String topicArn){
		if(instances == null) throw new IllegalArgumentException("DBInstance cannot be null");
		if(topicArn == null) throw new IllegalArgumentException("Topic ARN cannot be null");
		PutMetricAlarmRequest alarmRequest = createDefaultPutMetricRequest(instances, topicArn);
		alarmRequest.setAlarmName(instances.getDBInstanceIdentifier()+HIGH_WRITE_LATENCY);
		Double hundredMS = 0.1;
		// WriteLatency >= 0.1 for 1 consecutive period of 5 minutes
		alarmRequest.withStatistic(STATISTIC_AVERAGE).withMetricName(METRIC_WRITE_LATENCY).withComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold).withThreshold(hundredMS).withEvaluationPeriods(1).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		return alarmRequest;
	}
	
	/**
	 * High-CPU-Utilization
	 */
	public static PutMetricAlarmRequest createHighCPUUtilization(DBInstance instances, String topicArn){
		if(instances == null) throw new IllegalArgumentException("DBInstance cannot be null");
		if(topicArn == null) throw new IllegalArgumentException("Topic ARN cannot be null");
		PutMetricAlarmRequest alarmRequest = createDefaultPutMetricRequest(instances, topicArn);
		alarmRequest.setAlarmName(instances.getDBInstanceIdentifier()+HIGH_CPU_UTILIZATION);
		Double nintyPercent = 90.0;
		// CPUUtilization >= 90 for 5 minutes
		alarmRequest.withStatistic(STATISTIC_AVERAGE).withMetricName(METRIC_HIGH_CPU_UTILIZATION).withComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold).withThreshold(nintyPercent).withEvaluationPeriods(1).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		return alarmRequest;
	}

	
	/**
	 * Low-Free-Storage-Space
	 */
	public static PutMetricAlarmRequest createLowFreeStorage(DBInstance instances, String topicArn){
		if(instances == null) throw new IllegalArgumentException("DBInstance cannot be null");
		if(topicArn == null) throw new IllegalArgumentException("Topic ARN cannot be null");
		PutMetricAlarmRequest alarmRequest = createDefaultPutMetricRequest(instances, topicArn);
		alarmRequest.setAlarmName(instances.getDBInstanceIdentifier()+LOW_FREE_STOREAGE_SPACE);
		Double tenPercentGB = ((double)instances.getAllocatedStorage()) * 0.1;
		Double tenPercentBytes = tenPercentGB*Constants.BYTES_PER_GIGABYTE;
		// CPUUtilization >= 90 for 5 minutes
		alarmRequest.withStatistic(STATISTIC_AVERAGE).withMetricName(METRIC_FREE_STOREAGE_SPACE).withComparisonOperator(ComparisonOperator.LessThanOrEqualToThreshold).withThreshold(tenPercentBytes).withEvaluationPeriods(1).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		return alarmRequest;
	}
}
