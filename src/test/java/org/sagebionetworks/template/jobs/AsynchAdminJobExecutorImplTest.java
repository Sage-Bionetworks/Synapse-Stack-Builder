package org.sagebionetworks.template.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseServiceUnavailable;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousAdminRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.config.RepoConfiguration;

@ExtendWith(MockitoExtension.class)
public class AsynchAdminJobExecutorImplTest {
	
	@Mock
	private SynapseAdminClient mockClient;

	@Mock
	private RepoConfiguration mockConfig;
	
	@InjectMocks
	private AsynchAdminJobExecutorImpl executor;
	
	@Mock
	private AsynchronousAdminRequestBody mockRequest;
	
	@Test
	public void testExectuteJobWithSuccess() throws Exception {
	
		int timeout = 60;
		String jobId = "123";
		
		AsynchronousJobStatus returnStatus = new AsynchronousJobStatus()
				.setJobState(AsynchJobState.COMPLETE);
		
		when(mockConfig.getIntegerProperty(anyString())).thenReturn(timeout);
		when(mockClient.startAdminAsynchronousJob(any())).thenReturn(new AsynchronousJobStatus().setJobId(jobId));
		when(mockClient.getAdminAsynchronousJobStatus(any())).thenReturn(returnStatus);
		
		// Call under test
		executor.executeJob(mockRequest);
		
		verify(mockConfig).getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_TIMEOUT);
		verify(mockConfig).getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_CHECK_INTERVAL);
		verify(mockClient).startAdminAsynchronousJob(mockRequest);
		verify(mockClient).getAdminAsynchronousJobStatus(jobId);
	}
	
	@Test
	public void testExectuteJobWithUnavailable() throws Exception {
	
		int timeout = 60;
		
		when(mockConfig.getIntegerProperty(anyString())).thenReturn(timeout);
		when(mockClient.startAdminAsynchronousJob(any())).thenThrow(SynapseServiceUnavailable.class);
		
		// Call under test
		executor.executeJob(mockRequest);
		
		verify(mockConfig).getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_TIMEOUT);
		verify(mockConfig).getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_CHECK_INTERVAL);
		verify(mockClient).startAdminAsynchronousJob(mockRequest);
		verifyNoMoreInteractions(mockClient);
	}
	
	@Test
	public void testExectuteJobWithProcessing() throws Exception {
	
		int timeout = 60;
		int checkInterval = 100;
		String jobId = "123";
		
		AsynchronousJobStatus returnStatus = new AsynchronousJobStatus()
				.setJobState(AsynchJobState.COMPLETE);
		
		when(mockConfig.getIntegerProperty(anyString())).thenReturn(timeout, checkInterval);
		when(mockClient.startAdminAsynchronousJob(any())).thenReturn(new AsynchronousJobStatus().setJobId(jobId));
		when(mockClient.getAdminAsynchronousJobStatus(any())).thenReturn(new AsynchronousJobStatus().setJobState(AsynchJobState.PROCESSING), returnStatus);
		
		// Call under test
		executor.executeJob(mockRequest);
		
		verify(mockConfig).getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_TIMEOUT);
		verify(mockConfig).getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_CHECK_INTERVAL);
		verify(mockClient).startAdminAsynchronousJob(mockRequest);
		verify(mockClient, times(2)).getAdminAsynchronousJobStatus(jobId);
	}
	
	
	@Test
	public void testExectuteJobWithFailed() throws Exception {
	
		int timeout = 60;
		String jobId = "123";
		
		AsynchronousJobStatus returnStatus = new AsynchronousJobStatus()
				.setJobState(AsynchJobState.FAILED)
				.setErrorMessage("error");
		
		when(mockConfig.getIntegerProperty(anyString())).thenReturn(timeout);
		when(mockClient.startAdminAsynchronousJob(any())).thenReturn(new AsynchronousJobStatus().setJobId(jobId));
		when(mockClient.getAdminAsynchronousJobStatus(any())).thenReturn(returnStatus);
		
		AsynchJobFailedException result = assertThrows(AsynchJobFailedException.class, () -> {			
			// Call under test
			executor.executeJob(mockRequest);
		});
		
		assertEquals(returnStatus, result.getStatus());
		
		verify(mockConfig).getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_TIMEOUT);
		verify(mockConfig).getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_CHECK_INTERVAL);
		verify(mockClient).startAdminAsynchronousJob(mockRequest);
		verify(mockClient).getAdminAsynchronousJobStatus(jobId);
	}

}
