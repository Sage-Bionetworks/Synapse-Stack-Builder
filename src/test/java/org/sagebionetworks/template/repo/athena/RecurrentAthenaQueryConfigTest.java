package org.sagebionetworks.template.repo.athena;

import static java.time.temporal.TemporalAdjusters.firstInMonth;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.queues.SqsQueueDescriptor;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class RecurrentAthenaQueryConfigTest {

	private Map<String, RecurrentAthenaQuery> queryMap;
	private CronParser eventBridgeCronParser;

	@BeforeEach
	public void before() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		RecurrentAthenaQueryConfig config = injector.getInstance(RecurrentAthenaQueryConfig.class);
		assertNotNull(config);
		this.queryMap = config.getQueries().stream().collect(Collectors.toMap(RecurrentAthenaQuery::getQueryName, Function.identity()));
		// For the cron parser definition supported by event bridge see: https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-schedule-expressions.html
		CronDefinition eventBridgeCronDefinition = CronDefinitionBuilder.defineCron()
		        .withMinutes().and()
		        .withHours().and()
		        .withDayOfMonth().supportsL().supportsW().supportsQuestionMark().and()
		        .withMonth().and()
		        .withDayOfWeek().withMondayDoWValue(2).supportsL().supportsHash().supportsQuestionMark().and()
		        .withYear().and()
		        .instance();
		
		this.eventBridgeCronParser = new CronParser(eventBridgeCronDefinition);
	}

	@Test
	public void testUnlinkedFileHandleConfig() throws ParseException {
		RecurrentAthenaQuery query = queryMap.get("UnlinkedFileHandles");

		assertNotNull(query);

		String expectedCron = "0 10 ? * 2#1 *";
		assertEquals("cron(" + expectedCron + ")", query.getScheduleExpression());
		assertEquals("RECURRENT_ATHENA_QUERIES", query.getDestinationQueue());
		
		Cron cron = eventBridgeCronParser.parse(expectedCron);

		ExecutionTime executionTime = ExecutionTime.forCron(cron);

		ZoneOffset utcZone = ZoneOffset.UTC;

		LocalDate firstMondayOfMonth = LocalDate.now(utcZone).with(firstInMonth(DayOfWeek.MONDAY));
		ZonedDateTime expected = firstMondayOfMonth.atTime(10, 0).atZone(utcZone);

		// Test for the next 12 months
		for (int i = 0; i < 12; i++) {
			ZonedDateTime testingDateTime = firstMondayOfMonth.atTime(10, 0).atZone(utcZone);
			
			assertTrue(executionTime.isMatch(testingDateTime));
			assertEquals(expected, expected);
			
			firstMondayOfMonth = firstMondayOfMonth.plusMonths(1).with(firstInMonth(DayOfWeek.MONDAY));
			
			expected = executionTime.nextExecution(testingDateTime).get();
			
		}

	}

	static RecurrentAthenaQuery query(String name, String path, String cronExpression, String destinationQueue) {
		RecurrentAthenaQuery query = new RecurrentAthenaQuery();
		query.setQueryName(name);
		query.setQueryPath(path);
		query.setScheduleExpression(cronExpression);
		query.setDestinationQueue(destinationQueue);
		return query;
	}

	static SqsQueueDescriptor queue(String name) {
		return new SqsQueueDescriptor(name, Collections.emptyList(), 12, 12, 12);
	}

}
