package org.sagebionetworks.template.repo.workers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.RepositoryTemplateBuilder;

public class TestWorkerBuilderMain {
	//TODO: this will probably be deleted. used for quicktesting only

	public static void main(String[] args){
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		WorkerQueueBuilder builder = injector.getInstance(WorkerQueueBuilder.class);
		builder.buildAndDeploy();
	}


}
