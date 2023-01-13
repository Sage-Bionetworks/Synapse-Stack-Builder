package org.sagebionetworks.template.ip.address;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class IpAddressPoolMain {

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		IpAddressPoolBuilder builder = injector.getInstance(IpAddressPoolBuilder.class);
		builder.buildAndDeploy();
	}

}
