package org.sagebionetworks.template.cron;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.TimeToLive;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;

@ExtendWith(MockitoExtension.class)
public class ExpiredStackTeardownImplTest {

	@Mock
	private CloudFormationClient mockCloudFormationClient;
	@Mock
	private TimeToLive mockTimeToLive;
	@Mock
	private LoggerFactory mockLoggerFactory;
	@Mock
	private Logger mockLogger;

	private ExpiredStackTeardownImpl down;
	
	@BeforeEach
	public void before() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		down = new ExpiredStackTeardownImpl(mockCloudFormationClient, mockTimeToLive, mockLoggerFactory);
	}

	@Test
	public void testFindAndDeleteExpiredStacksWithCreateComplete() {

		Stack stack = new Stack().withStackName("deleteMe").withStackStatus(StackStatus.CREATE_COMPLETE)
				.withParameters(new Parameter().withParameterKey("key")).withEnableTerminationProtection(false);

		when(mockCloudFormationClient.streamOverAllStacks()).thenReturn(List.of(stack).stream());
		when(mockTimeToLive.isTimeToLiveExpired(any())).thenReturn(true);
		
		// call under test
		down.findAndDeleteExpiredStacks();

		verify(mockCloudFormationClient).deleteStack(stack.getStackName());
		verify(mockTimeToLive).isTimeToLiveExpired(stack.getParameters());
		verify(mockLogger).info("Deleting stack: 'deleteMe'...");
		
		verifyNoMoreInteractions(mockCloudFormationClient);
		verifyNoMoreInteractions(mockTimeToLive);
		verifyNoMoreInteractions(mockLogger);

	}
	
	@Test
	public void testFindAndDeleteExpiredStacksWithUpdateComplete() {

		Stack stack = new Stack().withStackName("deleteMe").withStackStatus(StackStatus.UPDATE_COMPLETE)
				.withParameters(new Parameter().withParameterKey("key")).withEnableTerminationProtection(false);

		when(mockCloudFormationClient.streamOverAllStacks()).thenReturn(List.of(stack).stream());
		when(mockTimeToLive.isTimeToLiveExpired(any())).thenReturn(true);
		
		// call under test
		down.findAndDeleteExpiredStacks();

		verify(mockCloudFormationClient).deleteStack(stack.getStackName());
		verify(mockTimeToLive).isTimeToLiveExpired(stack.getParameters());
		verify(mockLogger).info("Deleting stack: 'deleteMe'...");
		
		verifyNoMoreInteractions(mockCloudFormationClient);
		verifyNoMoreInteractions(mockTimeToLive);
		verifyNoMoreInteractions(mockLogger);

	}
	
	@Test
	public void testFindAndDeleteExpiredStacksWithUpdateRollback() {

		Stack stack = new Stack().withStackName("deleteMe").withStackStatus(StackStatus.UPDATE_ROLLBACK_COMPLETE)
				.withParameters(new Parameter().withParameterKey("key")).withEnableTerminationProtection(false);

		when(mockCloudFormationClient.streamOverAllStacks()).thenReturn(List.of(stack).stream());
		when(mockTimeToLive.isTimeToLiveExpired(any())).thenReturn(true);
		
		// call under test
		down.findAndDeleteExpiredStacks();

		verify(mockCloudFormationClient).deleteStack(stack.getStackName());
		verify(mockTimeToLive).isTimeToLiveExpired(stack.getParameters());
		verify(mockLogger).info("Deleting stack: 'deleteMe'...");
		
		verifyNoMoreInteractions(mockCloudFormationClient);
		verifyNoMoreInteractions(mockTimeToLive);
		verifyNoMoreInteractions(mockLogger);

	}
	
	@Test
	public void testFindAndDeleteExpiredStacksWithNotExpired() {

		Stack stack = new Stack().withStackName("deleteMe").withStackStatus(StackStatus.CREATE_COMPLETE)
				.withParameters(new Parameter().withParameterKey("key")).withEnableTerminationProtection(false);

		when(mockCloudFormationClient.streamOverAllStacks()).thenReturn(List.of(stack).stream());
		when(mockTimeToLive.isTimeToLiveExpired(any())).thenReturn(false);
		
		// call under test
		down.findAndDeleteExpiredStacks();

		verify(mockTimeToLive).isTimeToLiveExpired(stack.getParameters());
		
		verifyNoMoreInteractions(mockCloudFormationClient);
		verifyNoMoreInteractions(mockTimeToLive);
		verifyNoMoreInteractions(mockLogger);

	}
	
	@Test
	public void testFindAndDeleteExpiredStacksWithTerminationProtection() {

		Stack stack = new Stack().withStackName("deleteMe").withStackStatus(StackStatus.CREATE_COMPLETE)
				.withParameters(new Parameter().withParameterKey("key")).withEnableTerminationProtection(true);

		when(mockCloudFormationClient.streamOverAllStacks()).thenReturn(List.of(stack).stream());
		when(mockTimeToLive.isTimeToLiveExpired(any())).thenReturn(true);
		
		// call under test
		down.findAndDeleteExpiredStacks();

		verify(mockTimeToLive).isTimeToLiveExpired(stack.getParameters());
		
		verifyNoMoreInteractions(mockCloudFormationClient);
		verifyNoMoreInteractions(mockTimeToLive);
		verifyNoMoreInteractions(mockLogger);

	}

	@Test
	public void testFindAndDeleteExpiredStacksWithNoWork() {
		when(mockCloudFormationClient.streamOverAllStacks()).thenReturn(new ArrayList<Stack>().stream());
		// call under test
		down.findAndDeleteExpiredStacks();

		verify(mockCloudFormationClient, never()).deleteStack(any());

	}
	
	/**
	 * A failure to delete one stack should not stop the delete of the other stacks.
	 */
	@Test
	public void testFindAndDeleteExpiredStacksWithOneFailure() {

		Stack one = new Stack().withStackName("one").withStackStatus(StackStatus.CREATE_COMPLETE)
				.withParameters(new Parameter().withParameterKey("key")).withEnableTerminationProtection(false);
		Stack two = new Stack().withStackName("two").withStackStatus(StackStatus.CREATE_COMPLETE)
				.withParameters(new Parameter().withParameterKey("key")).withEnableTerminationProtection(false);

		when(mockCloudFormationClient.streamOverAllStacks()).thenReturn(List.of(one, two).stream());
		when(mockTimeToLive.isTimeToLiveExpired(any())).thenReturn(true);
		
		doThrow(new IllegalArgumentException("nope")).when(mockCloudFormationClient).deleteStack("one");;
		doNothing().when(mockCloudFormationClient).deleteStack("two");
		
		// call under test
		down.findAndDeleteExpiredStacks();

		verify(mockCloudFormationClient).deleteStack(one.getStackName());
		verify(mockCloudFormationClient).deleteStack(two.getStackName());
		verify(mockTimeToLive, times(2)).isTimeToLiveExpired(any());
		verify(mockLogger, times(2)).info(any(String.class));
		verify(mockLogger, times(1)).error(any(String.class), any(Throwable.class));
		
		verifyNoMoreInteractions(mockCloudFormationClient);
		verifyNoMoreInteractions(mockTimeToLive);
		verifyNoMoreInteractions(mockLogger);

	}
}
