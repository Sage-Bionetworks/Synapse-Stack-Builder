package org.sagebionetworks.template.jobs;

import org.sagebionetworks.repo.model.asynch.AsynchronousAdminRequestBody;

public interface AsynchAdminJobExecutor {

	void executeJob(AsynchronousAdminRequestBody requestBody) throws Exception;

}