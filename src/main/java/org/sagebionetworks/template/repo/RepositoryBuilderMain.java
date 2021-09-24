package org.sagebionetworks.template.repo;

import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.docs.SynapseDocsBuilder;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Main to build all repository stacks.
 *
 */
public class RepositoryBuilderMain {

	public static void main(String[] args) throws InterruptedException {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		SynapseDocsBuilder docsBuilder = injector.getInstance(SynapseDocsBuilder.class);
		RepositoryTemplateBuilder builder = injector.getInstance(RepositoryTemplateBuilder.class);
		docsBuilder.deployDocs();
		builder.buildAndDeploy();
	}
}
