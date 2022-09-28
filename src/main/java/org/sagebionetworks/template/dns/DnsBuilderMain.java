package org.sagebionetworks.template.dns;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

import java.util.Arrays;

import static org.sagebionetworks.template.Constants.ROUTE53_DEV_SAGEBASE_ORG_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_SAGEBASE_ORG_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_SYNAPSE_ORG_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.TEMPLATE_RESOURCE_PATH;
import static org.sagebionetworks.template.TemplateUtils.loadFromJsonFile;

public class DnsBuilderMain {

	public static void main(String[] args) throws Exception {

		String prefix = args[0];
		String dnsConfigFileResourcePath = String.format(TEMPLATE_RESOURCE_PATH, prefix);
		DnsBuilderMain.validateResourcePath(dnsConfigFileResourcePath);
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		// loads row config
		DnsConfigBuilder dnsConfigBuilder = loadFromJsonFile(dnsConfigFileResourcePath, DnsConfigBuilder.class);
		DnsConfig dnsConfig = dnsConfigBuilder.build(); // validates and creates actual config
		DnsBuilder builder = injector.getInstance(DnsBuilder.class);
		builder.buildDns(dnsConfig);
	}

	public static void validateResourcePath(String resourcePath) {
		if (! Arrays.asList(ROUTE53_DEV_SAGEBASE_ORG_DNS_CONFIG_FILE, ROUTE53_PROD_SAGEBASE_ORG_DNS_CONFIG_FILE, ROUTE53_PROD_SYNAPSE_ORG_DNS_CONFIG_FILE).contains(resourcePath)) {
			throw new IllegalArgumentException("Invalid resource path specified");
		}
	}

}
