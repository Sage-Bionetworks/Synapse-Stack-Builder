package org.sagebionetworks.template.repo.queues;

import java.util.Collections;

import org.junit.Test;

public class SqsQueueDescriptorTest {

	final Integer messageVisibilityTimeoutSec = 5;

	final Integer deadLetterQueueMaxFailureCount = 6;
	final Integer oldestMessageInQueueAlarmThresholdSec =7;

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_badQueueName(){
		new SqsQueueDescriptor("BAD-NAME", Collections.emptyList(),
				messageVisibilityTimeoutSec,deadLetterQueueMaxFailureCount,oldestMessageInQueueAlarmThresholdSec);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_badSubScribedTopicsName(){
		new SqsQueueDescriptor("goodName", Collections.singletonList("bad-name"),
				messageVisibilityTimeoutSec,deadLetterQueueMaxFailureCount,oldestMessageInQueueAlarmThresholdSec);
	}

	@Test
	public void testConstructor_goodNames(){
		new SqsQueueDescriptor("goodName", Collections.singletonList("goodNameAgain"),
				messageVisibilityTimeoutSec,deadLetterQueueMaxFailureCount,oldestMessageInQueueAlarmThresholdSec);
	}
}
