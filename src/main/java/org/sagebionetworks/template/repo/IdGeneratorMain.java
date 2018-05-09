package org.sagebionetworks.template.repo;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class IdGeneratorMain {

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		IdGeneratorBuilder builder = injector.getInstance(IdGeneratorBuilder.class);
		builder.buildAndDeploy();
	}

}
