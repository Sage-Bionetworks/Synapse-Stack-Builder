package org.sagebionetworks.template.vpc;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Simple main to create the VPC template stack.
 * @author John
 *
 */
public class VpcBuilderMain {

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		VpcTemplateBuilder builder = injector.getInstance(VpcTemplateBuilder.class);
		builder.buildAndDeploy();
	}

}
