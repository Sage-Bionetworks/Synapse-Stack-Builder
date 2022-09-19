package org.sagebionetworks.template.dns;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

import static org.sagebionetworks.template.Constants.ROUTE53_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.TemplateUtils.loadFromJsonFile;

public class DnsBuilderMain {

	public static void main(String[] args) throws Exception {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		DnsConfigValidator validator = injector.getInstance(DnsConfigValidator.class);
		// TODO: change to param
		DnsConfig dnsConfig = loadFromJsonFile(ROUTE53_DNS_CONFIG_FILE, DnsConfig.class);
		validator.validate(dnsConfig);
		DnsBuilder builder = injector.getInstance(DnsBuilder.class);
		builder.buildDns(dnsConfig);
	}

}
