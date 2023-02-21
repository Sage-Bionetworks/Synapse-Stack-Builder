package org.sagebionetworks.template.config;

import static org.sagebionetworks.template.Constants.PARAM_KEY_TIME_TO_LIVE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_TIME_TO_LIVE_HOURS;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.template.Constants;
import org.sagebionetworks.util.Clock;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;

public class TimeToLiveImpl implements TimeToLive {

	private final RepoConfiguration config;
	private final Clock clock;

	@Inject
	public TimeToLiveImpl(RepoConfiguration config, Clock clock) {
		this.config = config;
		this.clock = clock;
	}

	@Override
	public Optional<Parameter> createTimeToLiveParameter() {
		if (!Constants.isProd(config.getProperty(PROPERTY_KEY_STACK))) {
			int ttlHours = config.getIntegerProperty(PROPERTY_KEY_TIME_TO_LIVE_HOURS);
			if (ttlHours > 0) {
				ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock.currentTimeMillis()),
						ZoneId.of("America/Los_Angeles"));
				String expiration = now.plus(Duration.ofHours(ttlHours)).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
				return Optional
						.of(new Parameter().withParameterKey(PARAM_KEY_TIME_TO_LIVE).withParameterValue(expiration));
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isTimeToLiveExpired(List<Parameter> parameters) {
		if (parameters == null) {
			return false;
		}
		return parameters.stream().filter(p -> PARAM_KEY_TIME_TO_LIVE.equals(p.getParameterKey())).map((p) -> {
			try {
				ZonedDateTime deleteOn = ZonedDateTime.parse(p.getParameterValue(),
						DateTimeFormatter.ISO_ZONED_DATE_TIME);
				ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock.currentTimeMillis()),
						ZoneOffset.UTC);
				return deleteOn.isBefore(now);
			} catch (DateTimeParseException e) {
				return false;
			}

		}).findFirst().orElse(false);

	}

}
