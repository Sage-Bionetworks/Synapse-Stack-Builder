package org.sagebionetworks.stack;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.stack.config.InputConfiguration;

import static org.junit.Assert.*;
import static org.sagebionetworks.stack.Constants.*;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.sns.model.CreateTopicResult;
import org.junit.Ignore;
import org.sagebionetworks.factory.MockAmazonClientFactory;

/**
 * Test for the AlarmSetup.
 * 
 * @author John
 *
 */
public class AlarmSetupTest {
	
	String databaseIdentifer;
	DBInstance dbInstance;
	String topicArn;
	InputConfiguration config;
	AmazonCloudWatchClient mockClient;
	AlarmSetup setup;
	GeneratedResources resources;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	
	
	@Before
	public void before() throws IOException{
		databaseIdentifer = "db-id-foo";
		dbInstance = new DBInstance().withDBInstanceClass(DATABASE_INSTANCE_CLASS_SMALL).withDBInstanceIdentifier(databaseIdentifer);
		// Give this 10 GB
		dbInstance.setAllocatedStorage(10);
		topicArn = "arn:123:456";
		config = TestHelper.createTestConfig("dev");
		mockClient = factory.createCloudWatchClient();
		resources = new GeneratedResources();
		resources.setRdsAlertTopic(new CreateTopicResult().withTopicArn(topicArn));
		resources.setStackInstancesDatabase(new DBInstance().withAllocatedStorage(50).withDBInstanceClass(DATABASE_INSTANCE_CLASS_SMALL).withDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier()));
		resources.setIdGeneratorDatabase(new DBInstance().withAllocatedStorage(10).withDBInstanceClass(DATABASE_INSTANCE_CLASS_SMALL).withDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier()));
		setup = new AlarmSetup(factory, config, resources);
	}
	
	@Test
	public void testCreateDefaultPutMetricRequest(){
		PutMetricAlarmRequest expected = new PutMetricAlarmRequest();
		expected.setAlarmDescription("Setup by: "+AlarmSetup.class.getName());
		expected.setActionsEnabled(true);
		expected.withAlarmActions(topicArn);
		expected.setNamespace(NAME_SPACES_AWS_RDS);
		expected.withDimensions(new Dimension().withName(DB_INSTANCE_IDENTIFIER).withValue(databaseIdentifer));
		PutMetricAlarmRequest result = AlarmSetup.createDefaultPutMetricRequest(dbInstance, topicArn);
		assertEquals(expected, result);
	}

	@Test
	public void testCreateLowFreeableMemorySmallInstances(){
		// the free able memory alarm is a function of the instances size
		Double totalMemory = Constants.getDatabaseClassMemrorySizeBytes(DATABASE_INSTANCE_CLASS_SMALL);
		System.out.println("Small memory total: "+totalMemory+" bytes");
		PutMetricAlarmRequest expected = AlarmSetup.createDefaultPutMetricRequest(dbInstance, topicArn);
		expected.setAlarmName(databaseIdentifer+"-Low-Freeable-Memory");

		Double eightPercent = totalMemory - (totalMemory*0.8);
		// Average FreeableMemory < 80% for 1 consecutive period of 5 minutes
		expected.withStatistic("Average").withMetricName(METRIC_FREEABLE_MEMORY).withComparisonOperator(ComparisonOperator.LessThanThreshold).withThreshold(eightPercent).withEvaluationPeriods(1).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		PutMetricAlarmRequest result = AlarmSetup.createLowFreeableMemory(dbInstance, topicArn);
		assertEquals(expected, result);
	}
	
	@Test
	public void testCreateLowFreeableMemoryLargeInstances(){
		// the free able memory alarm is a function of the instances size
		// Set the size to large
		dbInstance.setDBInstanceClass(DATABASE_INSTANCE_CLASS_LARGE);
		Double totalMemory = Constants.getDatabaseClassMemrorySizeBytes(DATABASE_INSTANCE_CLASS_LARGE);
		System.out.println("Large memory total: "+totalMemory+" bytes");
		PutMetricAlarmRequest expected = AlarmSetup.createDefaultPutMetricRequest(dbInstance, topicArn);
		expected.setAlarmName(databaseIdentifer+"-Low-Freeable-Memory");
		Double eightPercent = totalMemory - (totalMemory*0.8);
		// Average FreeableMemory < 80% for 1 consecutive period of 5 minutes
		expected.withStatistic("Average").withMetricName(METRIC_FREEABLE_MEMORY).withComparisonOperator(ComparisonOperator.LessThanThreshold).withThreshold(eightPercent).withEvaluationPeriods(1).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		PutMetricAlarmRequest result = AlarmSetup.createLowFreeableMemory(dbInstance, topicArn);
		assertEquals(expected, result);
	}
	
	@Test
	public void testHightWriteLatency(){
		PutMetricAlarmRequest expected = AlarmSetup.createDefaultPutMetricRequest(dbInstance, topicArn);
		expected.setAlarmName(databaseIdentifer+HIGH_WRITE_LATENCY);
		expected.withStatistic("Average").withMetricName(METRIC_WRITE_LATENCY).withComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold).withThreshold(0.1).withEvaluationPeriods(1).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		PutMetricAlarmRequest result = AlarmSetup.createHighWriteLatency(dbInstance, topicArn);
		assertEquals(expected, result);
	}
	
	@Test
	public void testHightCPU(){
		PutMetricAlarmRequest expected = AlarmSetup.createDefaultPutMetricRequest(dbInstance, topicArn);
		expected.setAlarmName(databaseIdentifer+HIGH_CPU_UTILIZATION);
		expected.withStatistic("Average").withMetricName(METRIC_HIGH_CPU_UTILIZATION).withComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold).withThreshold(90.0).withEvaluationPeriods(1).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		PutMetricAlarmRequest result = AlarmSetup.createHighCPUUtilization(dbInstance, topicArn);
		assertEquals(expected, result);
	}
	
	@Test
	public void testLowFreeStorage(){
		PutMetricAlarmRequest expected = AlarmSetup.createDefaultPutMetricRequest(dbInstance, topicArn);
		expected.setAlarmName(databaseIdentifer+LOW_FREE_STOREAGE_SPACE);
		Double tenPercentGB = ((double)dbInstance.getAllocatedStorage()) * 0.1;
		Double tenPercentBytes = tenPercentGB*Constants.BYTES_PER_GIGABYTE;
		System.out.println("10% = "+tenPercentBytes+" bytes");
		expected.withStatistic("Average").withMetricName(METRIC_FREE_STOREAGE_SPACE).withComparisonOperator(ComparisonOperator.LessThanOrEqualToThreshold).withThreshold(tenPercentBytes).withEvaluationPeriods(1).withPeriod(FIVE_MINUTES_IN_SECONDS);	
		PutMetricAlarmRequest result = AlarmSetup.createLowFreeStorage(dbInstance, topicArn);
		assertEquals(expected, result);
	}
	
	@Test
	public void testSetupAllAlarms(){
		// Make sure all of the expected alarms are there
		setup.setupAllAlarms();
		assertNotNull(resources.getIdGeneratorDatabaseAlarms());
		assertNotNull(resources.getStackInstancesDatabaseAlarms());
		// Are they all there?
		validateExpectedAlarms(config.getIdGeneratorDatabaseIdentifier(), resources.getIdGeneratorDatabaseAlarms());
		validateExpectedAlarms(config.getStackInstanceDatabaseIdentifier(), resources.getStackInstancesDatabaseAlarms());
	}

	@Ignore
	@Test
	public void testDeleteAllAlarms() {
		setup.setupAllAlarms();
		setup.deleteAllAlarmsForDatabase(dbInstance);
	}

	/**
	 * Validate that all of the expected alarms are there.
	 * @param expectedName
	 * @param alarms
	 */
	private static void validateExpectedAlarms(String dbIdentifier, List<PutMetricAlarmRequest> alarms){
		if(dbIdentifier == null) throw new IllegalArgumentException("dbIdentifier names cannot be null");
		if(alarms == null) throw new IllegalArgumentException("Alarms cannot be null");
		String[] expectedAlarmNames = new String[]{
				dbIdentifier+"-Low-Freeable-Memory",
				dbIdentifier+HIGH_WRITE_LATENCY,
				dbIdentifier+HIGH_CPU_UTILIZATION,
				dbIdentifier+LOW_FREE_STOREAGE_SPACE
		};
		assertEquals("Did not find the expected number of alarms",expectedAlarmNames.length, alarms.size());
		for(String name: expectedAlarmNames){
			PutMetricAlarmRequest found = getAlarmByName(name, alarms);
			assertNotNull("Failed to find an alarm with the name: "+name, found);
		}
	}
	
	/**
	 * Helper to validate alarms
	 * @param name
	 * @param alarms
	 * @return
	 */
	private static PutMetricAlarmRequest getAlarmByName(String name, List<PutMetricAlarmRequest> alarms){
		for(PutMetricAlarmRequest put: alarms){
			if(name.equals(put.getAlarmName())) return put;
		}
		return null;
	}
	
	
}
