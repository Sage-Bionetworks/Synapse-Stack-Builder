package org.sagebionetworks.template.jobs;

import org.sagebionetworks.repo.model.asynch.AsynchronousAdminRequestBody;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class AsyncAdminJobExecutorMain {
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			throw new IllegalArgumentException("The request body is required");
		}
		
		String stringRequest = args[0];
		
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		
		AsynchAdminJobExecutor jobExecutor = injector.getInstance(AsynchAdminJobExecutor.class);
		
		AsynchronousAdminRequestBody requestBody = EntityFactory.createEntityFromJSONString(stringRequest, AsynchronousAdminRequestBody.class);
		
		jobExecutor.executeJob(requestBody);
	}

}
