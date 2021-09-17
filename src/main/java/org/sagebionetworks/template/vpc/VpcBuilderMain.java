package org.sagebionetworks.template.vpc;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Main to build the VPC stack.
 *
 */
public class VpcBuilderMain {

	public static void main(String[] args) throws InterruptedException {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		VpcTemplateBuilder builder = injector.getInstance(VpcTemplateBuilder.class);
		builder.buildAndDeploy();
		SubnetTemplateBuilder subnetBuilder = injector.getInstance(SubnetTemplateBuilder.class);
		subnetBuilder.buildAndDeployPublicSubnets();
		subnetBuilder.buildAndDeployPrivateSubnets();
	}

}
