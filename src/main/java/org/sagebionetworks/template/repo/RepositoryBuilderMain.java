package org.sagebionetworks.template.repo;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Main to build all repository stacks.
 *
 */
public class RepositoryBuilderMain {

	public static void main(String[] args) throws InterruptedException {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		RepositoryTemplateBuilder builder = injector.getInstance(RepositoryTemplateBuilder.class);
		builder.buildAndDeploy();
	}

}
