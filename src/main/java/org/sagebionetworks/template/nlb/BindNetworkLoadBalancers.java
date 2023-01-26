package org.sagebionetworks.template.nlb;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * This main is used to setup the binding between the static network load
 * balancers of each DNS record to the dynamic application load balances of each
 * stack.
 *
 */
public class BindNetworkLoadBalancers {

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		BindNetworkLoadBalancerBuilder builder = injector.getInstance(BindNetworkLoadBalancerBuilder.class);
		builder.buildAndDeploy();
	}

}
