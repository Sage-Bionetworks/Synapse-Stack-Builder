package org.sagebionetworks.stack;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;
import static org.sagebionetworks.stack.Constants.*;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.rds.model.DBInstance;
import java.util.ArrayList;
import java.util.Arrays;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 * Setups the various cloud watch alarms.
 * 
 * @author John
 *
 */
public class AlarmSetup implements ResourceProcessor {
	

	private static Logger log = Logger.getLogger(AlarmSetup.class.getName());
	
	AmazonCloudWatchClient client;
	InputConfiguration config;
	GeneratedResources resources;
	
	/**
	 * 
	 * @param client
	 * @param config
	 */
	public AlarmSetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		this.initialize(factory, config, resources);
	}
	
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		if(factory == null) throw new IllegalArgumentException("AmazonClientFactory cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		if(resources == null) throw new IllegalArgumentException("GeneratedResources cannot be null");
		if(resources.getRdsAlertTopicArn() == null) throw new IllegalArgumentException("GeneratedResources.getRdsAlertTopic() cannot be null");
		if(resources.getIdGeneratorDatabase() == null) throw new IllegalArgumentException("GeneratedResources.getIdGeneratorDatabase() cannot be null");
		if(resources.getStackInstancesDatabase() == null) throw new IllegalArgumentException("GeneratedResources.getStackInstancesDatabase() cannot be null");
		this.client = factory.createCloudWatchClient();
		this.config = config;
		this.resources = resources;
	}
	
	public void setupResources() {
		this.setupAllAlarms();
	}
	
	public void teardownResources() {
		deleteAllAlarms();
	}
	
	public void describeResources() {
		// This is the topic where all alarm notification are sent
		String topicArn = resources.getRdsAlertTopicArn();
		// setup the alarms for the id generator
		DBInstance instance = resources.getIdGeneratorDatabase();
		resources.setIdGeneratorDatabaseAlarms(describeAllAlarmsForDatabase(instance));
		// setup the alarms for the stack instances database.
		instance = resources.getStackInstancesDatabase();
		resources.setStackInstancesDatabaseAlarms(describeAllAlarmsForDatabase(instance));
	}

	/**
	 * Setup all alarms.
	 */
	public void setupAllAlarms(){
		List<PutMetricAlarmRequest> l;
		DescribeAlarmsResult r;
		// This is the topic where all alarm notification are sent
		String topicArn = resources.getRdsAlertTopicArn();
		// setup the alarms for the id generator
		DBInstance instance = resources.getIdGeneratorDatabase();
		l = createAllAlarmsForDatabase(instance, topicArn);
		r = describeAllAlarmsForDatabase(instance);
		resources.setIdGeneratorDatabaseAlarms(r);
		// setup the alarms for the stack instances database.
		instance = resources.getStackInstancesDatabase();
		l = createAllAlarmsForDatabase(instance, topicArn);
		r = describeAllAlarmsForDatabase(instance);
		resources.setStackInstancesDatabaseAlarms(r);
	}

	/**
	 * Delete all alarms.
	 */
	public void deleteAllAlarms(){
		DBInstance instance;
		// Delete the alarms for the stack instances database.
		instance = resources.getStackInstancesDatabase();
		deleteAllAlarmsForDatabase(instance);
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
		// Swap usage
		alarms.add(createSwapUsage(instances, topicArn));
		// High-Write-Latency
		alarms.add(createHighWriteLatency(instances, topicArn));
		// High CPU
		alarms.add(createHighCPUUtilization(instances, topicArn));
		// Low free storage
		alarms.add(createLowFreeStorage(instances, topicArn));
		// Add all alarms from the lsit
		for(PutMetricAlarmRequest alarm: alarms){
			log.info("Creating or updating alarm: "+alarm);
			client.putMetricAlarm(alarm);
		}
		return alarms;
	}

	public void deleteAllAlarmsForDatabase(DBInstance instance) {
		if (instance == null) throw new IllegalArgumentException("DBInstance cannpt be null");
		
		List<String> alarmsToDelete = this.getAlarms(instance);
		DeleteAlarmsRequest request = new DeleteAlarmsRequest().withAlarmNames(alarmsToDelete);
		client.deleteAlarms(request);
	}
	
	public DescribeAlarmsResult describeAllAlarmsForDatabase(DBInstance instance) {
		if (instance == null) throw new IllegalArgumentException("DBInstance cannpt be null");
		
		List<String> alarmsToDescribe = this.getAlarms(instance);
		DescribeAlarmsRequest req = new DescribeAlarmsRequest().withAlarmNames(alarmsToDescribe);
		DescribeAlarmsResult res = client.describeAlarms(req);
		return res;
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
	
	/**
	 * Low-Free-Storage-Space
	 */
	public static PutMetricAlarmRequest createSwapUsage(DBInstance instances, String topicArn){
		if(instances == null) throw new IllegalArgumentException("DBInstance cannot be null");
		if(topicArn == null) throw new IllegalArgumentException("Topic ARN cannot be null");
		PutMetricAlarmRequest alarmRequest = createDefaultPutMetricRequest(instances, topicArn);
		alarmRequest.setAlarmName(instances.getDBInstanceIdentifier()+SWAP_USAGE);
		Double swapUsageThreshold = 1024.0 * 1024.0 * 512.0;
		alarmRequest.withStatistic(STATISTIC_AVERAGE).withMetricName(METRIC_SWAP_USAGE).withComparisonOperator(ComparisonOperator.LessThanOrEqualToThreshold).withThreshold(swapUsageThreshold).withEvaluationPeriods(2).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		return alarmRequest;
	}
	
	private static List<String> getAlarms(DBInstance instance) {
		if(instance == null) throw new IllegalArgumentException("DBInstance cannot be null");
		List<String> alarms = Arrays.asList(
				instance.getDBInstanceIdentifier()+LOW_FREEABLE_MEMORY_NAME,
				instance.getDBInstanceIdentifier()+HIGH_WRITE_LATENCY,
				instance.getDBInstanceIdentifier()+HIGH_CPU_UTILIZATION,
				instance.getDBInstanceIdentifier()+LOW_FREE_STOREAGE_SPACE);
		return alarms;
	}
}
