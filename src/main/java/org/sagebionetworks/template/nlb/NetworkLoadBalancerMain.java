/**
 * 
 */
package org.sagebionetworks.template.nlb;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class NetworkLoadBalancerMain {

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		NetworkLoadBalancerBuilder builder = injector.getInstance(NetworkLoadBalancerBuilder.class);
		builder.buildAndDeploy();
	}
}
