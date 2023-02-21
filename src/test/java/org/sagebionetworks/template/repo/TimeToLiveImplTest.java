package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PARAM_KEY_TIME_TO_LIVE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TIME_TO_LIVE_HOURS;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.config.TimeToLiveImpl;
import org.sagebionetworks.util.Clock;

import com.amazonaws.services.cloudformation.model.Parameter;

@ExtendWith(MockitoExtension.class)
public class TimeToLiveImplTest {

	@Mock
	private RepoConfiguration mockConfig;

	@Mock
	private Clock mockClock;

	@InjectMocks
	private TimeToLiveImpl ttl;

	@Test
	public void testCreateTimeToLiveParameterWithProd() {
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");

		// call under test
		Optional<Parameter> op = ttl.createTimeToLiveParameter();
		assertEquals(Optional.empty(), op);
	}

	@Test
	public void testCreateTimeToLiveParameterWithDevWithZero() throws InterruptedException {
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getIntegerProperty(PROPERTY_KEY_TIME_TO_LIVE_HOURS)).thenReturn(0);

		// call under test
		Optional<Parameter> op = ttl.createTimeToLiveParameter();
		assertEquals(Optional.empty(), op);
	}

	@Test
	public void testCreateTimeToLiveParameterWithDevWithDefaults() throws InterruptedException {
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getIntegerProperty(PROPERTY_KEY_TIME_TO_LIVE_HOURS)).thenReturn(2);

		when(mockClock.currentTimeMillis()).thenReturn(1676941704344L);

		// call under test
		Optional<Parameter> op = ttl.createTimeToLiveParameter();

		Optional<Parameter> expected = Optional
				.of(new Parameter().withParameterKey(PARAM_KEY_TIME_TO_LIVE)
						.withParameterValue("2023-02-21T03:08:24.344Z"));
		assertEquals(expected, op);
	}

	@Test
	public void testIsTimeToLiveExpiredWithNull() {
		// call under test
		assertFalse(ttl.isTimeToLiveExpired(null));
	}

	@Test
	public void testIsTimeToLiveExpiredWithEmpty() {
		// call under test
		assertFalse(ttl.isTimeToLiveExpired(Collections.emptyList()));
	}

	@Test
	public void testIsTimeToLiveExpiredWithNoMatch() {
		List<Parameter> param = List.of(new Parameter().withParameterKey("wrong"));
		// call under test
		assertFalse(ttl.isTimeToLiveExpired(param));
	}

	@Test
	public void testIsTimeToLiveExpiredWithMatchDefault() {
		List<Parameter> param = List.of(
				new Parameter().withParameterKey(PARAM_KEY_TIME_TO_LIVE).withParameterValue("NONE"));
		// call under test
		assertFalse(ttl.isTimeToLiveExpired(param));
	}

	@Test
	public void testIsTimeToLiveExpiredWithNotExpired() {
		long nowMs = ZonedDateTime.parse("2023-02-21T03:08:04.000Z", DateTimeFormatter.ISO_ZONED_DATE_TIME)
				.toEpochSecond() * 1000;
		when(mockClock.currentTimeMillis()).thenReturn(nowMs);
		List<Parameter> param = List.of(new Parameter().withParameterKey(PARAM_KEY_TIME_TO_LIVE)
				.withParameterValue("2023-02-21T03:08:05.000Z"));
		// call under test
		assertFalse(ttl.isTimeToLiveExpired(param));
	}
	
	@Test
	public void testIsTimeToLiveExpiredWithExpired() {
		long nowMs = ZonedDateTime.parse("2023-02-21T03:08:04.000Z", DateTimeFormatter.ISO_ZONED_DATE_TIME)
				.toEpochSecond() * 1000;
		when(mockClock.currentTimeMillis()).thenReturn(nowMs);
		List<Parameter> param = List.of(new Parameter().withParameterKey(PARAM_KEY_TIME_TO_LIVE)
				.withParameterValue("2023-02-21T03:08:03.000Z"));
		// call under test
		assertTrue(ttl.isTimeToLiveExpired(param));
	}
}
