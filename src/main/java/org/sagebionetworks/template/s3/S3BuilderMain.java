package org.sagebionetworks.template.s3;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class S3BuilderMain {
	
	public static void main(String[] args) throws InterruptedException {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		S3BucketBuilder builder = injector.getInstance(S3BucketBuilder.class);
		builder.buildAllBuckets();
	}

}
