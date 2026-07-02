package org.sagebionetworks.template.repo;

import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.docs.SynapseDocsBuilder;
import org.sagebionetworks.template.s3.S3BucketBuilder;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Main to build all repository stacks.
 *
 */
public class RepositoryBuilderMain {

	public static void main(String[] args) throws InterruptedException {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		S3BucketBuilder s3Builder = injector.getInstance(S3BucketBuilder.class);
		SynapseDocsBuilder docsBuilder = injector.getInstance(SynapseDocsBuilder.class);
		RepositoryTemplateBuilder builder = injector.getInstance(RepositoryTemplateBuilder.class);
		s3Builder.buildAllBuckets();
		docsBuilder.deployDocs();
		builder.buildAndDeploy();
	}
}
