package org.sagebionetworks.template.jobs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseServiceUnavailable;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.asynch.AsynchronousAdminRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;

import com.google.inject.Inject;

public class AsynchAdminJobExecutorImpl implements AsynchAdminJobExecutor {
	
	private static final Logger LOG = LogManager.getLogger(AsynchAdminJobExecutorImpl.class);
	
	private static final int CHECK_INTERVAL_MS = 1000 * 10;
	
	private SynapseAdminClient adminClient;
	private RepoConfiguration config;
	
	@Inject
	public AsynchAdminJobExecutorImpl(SynapseAdminClient adminClient, RepoConfiguration config) {
		this.adminClient = adminClient;
		this.config = config;
	}
	
	@Override
	public void executeJob(AsynchronousAdminRequestBody requestBody) throws Exception {
		
		int jobTimeout = config.getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_TIMEOUT);
		
		int checkInterval;
		
		try {
			checkInterval = config.getIntegerProperty(Constants.PROPERTY_KEY_ADMIN_JOBS_CHECK_INTERVAL);
		} catch (ConfigurationPropertyNotFound ex) {
			checkInterval = CHECK_INTERVAL_MS;
		}
		
		try {
			
			long start = System.currentTimeMillis();
			
			final String jobId = adminClient.startAdminAsynchronousJob(requestBody).getJobId();
			final String jobType = requestBody.getClass().getSimpleName();
			
			LOG.info("Job {} submitted: {} (ID: {})", jobType, requestBody, jobId);
			
			final AsynchronousResponseBody response = TimeUtils.waitFor(jobTimeout, checkInterval, () -> {
				final AsynchronousJobStatus jobStatus = adminClient.getAdminAsynchronousJobStatus(jobId);
				
				LOG.info("Job Status: {} (ID: {})", jobStatus.toString(), jobId);
				
				switch (jobStatus.getJobState()) {
				case COMPLETE:
					return Pair.create(true, jobStatus.getResponseBody());
				case FAILED:
					LOG.error("Job {} FAILED: {} (ID: {}, Time: {} ms)", jobType, jobStatus, jobId, (System.currentTimeMillis() - start));
					throw new AsynchJobFailedException(jobStatus);
				default:
					return Pair.create(false, null);
				}
			});
			
			LOG.info("Job {} completed: {} (ID: {}, Time: {} ms)", jobType, response, jobId, (System.currentTimeMillis() - start));
			
		} catch (SynapseServiceUnavailable ex) {
			LOG.warn("Synapse service unavailable, won't retry: " + ex.getMessage());
		}
		
	}

}
