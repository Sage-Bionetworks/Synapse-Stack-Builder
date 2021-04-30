package org.sagebionetworks.template.repo.athena;

import static java.time.temporal.TemporalAdjusters.firstInMonth;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.core.util.CronExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.queues.SqsQueueDescriptor;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class RecurrentAthenaQueryConfigTest {

	private Map<String, RecurrentAthenaQuery> queryMap;

	@BeforeEach
	public void before() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		RecurrentAthenaQueryConfig config = injector.getInstance(RecurrentAthenaQueryConfig.class);
		assertNotNull(config);
		this.queryMap = config.getQueries().stream().collect(Collectors.toMap(RecurrentAthenaQuery::getQueryName, Function.identity()));
	}

	@Test
	public void testUnlinkedFileHandleConfig() throws ParseException {
		RecurrentAthenaQuery query = queryMap.get("UnlinkedFileHandles");

		assertNotNull(query);

		String expectedCron = "0 10 ? * 2#1 *";
		assertEquals("cron(" + expectedCron + ")", query.getScheduleExpression());
		assertEquals("RECURRENT_ATHENA_QUERIES", query.getDestinationQueue());

		// Event Bridge does not support second resolution, so we "fake" running at second 0
		CronExpression cron = new CronExpression("0 " + expectedCron);

		ZoneOffset utcZone = ZoneOffset.UTC;

		cron.setTimeZone(TimeZone.getTimeZone(utcZone));

		LocalDate firstMondayOfMonth = LocalDate.now(utcZone).with(firstInMonth(DayOfWeek.MONDAY));
		Date expected = Date.from(firstMondayOfMonth.atTime(10, 0).toInstant(utcZone));

		// Test for the next 12 months
		for (int i = 0; i < 12; i++) {
			Instant testingInstant = firstMondayOfMonth.atTime(10, 0).toInstant(utcZone);
			
			assertTrue(cron.isSatisfiedBy(Date.from(testingInstant)));
			assertEquals(expected, Date.from(testingInstant));
			
			firstMondayOfMonth = firstMondayOfMonth.plusMonths(1).with(firstInMonth(DayOfWeek.MONDAY));
			
			expected = cron.getNextValidTimeAfter(Date.from(testingInstant));
			
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
