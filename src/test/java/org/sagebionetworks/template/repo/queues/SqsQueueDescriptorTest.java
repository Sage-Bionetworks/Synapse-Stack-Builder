package org.sagebionetworks.template.repo.queues;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;

public class SqsQueueDescriptorTest {

	final Integer messageVisibilityTimeoutSec = 5;

	final Integer deadLetterQueueMaxFailureCount = 6;
	final Integer oldestMessageInQueueAlarmThresholdSec = 7;
	private Integer messageRetentionPeriodSec = 8;

	@Test
	public void testConstructor_badQueueName() {
		assertThrows(IllegalArgumentException.class, () -> {
			new SqsQueueDescriptor("BAD-NAME", Collections.emptyList(), messageVisibilityTimeoutSec,
					deadLetterQueueMaxFailureCount, oldestMessageInQueueAlarmThresholdSec, messageRetentionPeriodSec);
		});
	}

	@Test
	public void testConstructor_badSubScribedTopicsName() {
		assertThrows(IllegalArgumentException.class, () -> {
			new SqsQueueDescriptor("goodName", Collections.singletonList("bad-name"), messageVisibilityTimeoutSec,
					deadLetterQueueMaxFailureCount, oldestMessageInQueueAlarmThresholdSec, messageRetentionPeriodSec);
		});
	}

	@Test
	public void testConstructor_goodNames() {
		new SqsQueueDescriptor("goodName", Collections.singletonList("goodNameAgain"), messageVisibilityTimeoutSec,
				deadLetterQueueMaxFailureCount, oldestMessageInQueueAlarmThresholdSec, messageRetentionPeriodSec);
	}
}
