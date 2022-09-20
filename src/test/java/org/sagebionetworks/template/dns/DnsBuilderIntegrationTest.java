package org.sagebionetworks.template.dns;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.TemplateUtils;

import static org.sagebionetworks.template.Constants.ROUTE53_DEV_SAGEBASE_ORG_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_DOCS_CLIENT_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_PORTALS_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_SAGEBASE_ORG_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_SYNAPSE_ORG_DNS_CONFIG_FILE;

@ExtendWith(MockitoExtension.class)
public class DnsBuilderIntegrationTest {

	@Test
	void testLoadConfigs() throws Exception{
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		DnsConfigValidator validator = injector.getInstance(DnsConfigValidator.class);
		DnsConfig dnsConfig = TemplateUtils.loadFromJsonFile(ROUTE53_DEV_SAGEBASE_ORG_DNS_CONFIG_FILE, DnsConfig.class);
		validator.validate(dnsConfig);
		dnsConfig = TemplateUtils.loadFromJsonFile(ROUTE53_PROD_SAGEBASE_ORG_DNS_CONFIG_FILE, DnsConfig.class);
		validator.validate(dnsConfig);
		dnsConfig = TemplateUtils.loadFromJsonFile(ROUTE53_PROD_SYNAPSE_ORG_DNS_CONFIG_FILE, DnsConfig.class);
		validator.validate(dnsConfig);
		dnsConfig = TemplateUtils.loadFromJsonFile(ROUTE53_PROD_PORTALS_DNS_CONFIG_FILE, DnsConfig.class);
		validator.validate(dnsConfig);
		dnsConfig = TemplateUtils.loadFromJsonFile(ROUTE53_PROD_DOCS_CLIENT_DNS_CONFIG_FILE, DnsConfig.class);
		validator.validate(dnsConfig);

	}
}
