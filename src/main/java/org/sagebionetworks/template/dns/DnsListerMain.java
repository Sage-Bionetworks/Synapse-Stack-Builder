package org.sagebionetworks.template.dns;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

public class DnsListerMain {
	public static void main(String[] args) throws Exception {

		String hostedZoneId = args[0];
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		DnsBuilder builder = injector.getInstance(DnsBuilder.class);
		builder.listDns(hostedZoneId);
	}

}
