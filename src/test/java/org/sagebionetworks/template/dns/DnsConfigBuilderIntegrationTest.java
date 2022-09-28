package org.sagebionetworks.template.dns;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.TemplateUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sagebionetworks.template.Constants.ROUTE53_DEV_SAGEBASE_ORG_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_DOCS_CLIENT_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_PORTALS_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_SAGEBASE_ORG_DNS_CONFIG_FILE;
import static org.sagebionetworks.template.Constants.ROUTE53_PROD_SYNAPSE_ORG_DNS_CONFIG_FILE;

@ExtendWith(MockitoExtension.class)
public class DnsConfigBuilderIntegrationTest {

	@Test
	void testLoadConfigs() throws Exception{
		DnsConfigBuilder dnsConfigBuilder = TemplateUtils.loadFromJsonFile(ROUTE53_DEV_SAGEBASE_ORG_DNS_CONFIG_FILE, DnsConfigBuilder.class);
		dnsConfigBuilder = TemplateUtils.loadFromJsonFile(ROUTE53_PROD_SAGEBASE_ORG_DNS_CONFIG_FILE, DnsConfigBuilder.class);
		dnsConfigBuilder = TemplateUtils.loadFromJsonFile(ROUTE53_PROD_SYNAPSE_ORG_DNS_CONFIG_FILE, DnsConfigBuilder.class);
		dnsConfigBuilder = TemplateUtils.loadFromJsonFile(ROUTE53_PROD_PORTALS_DNS_CONFIG_FILE, DnsConfigBuilder.class);
		dnsConfigBuilder = TemplateUtils.loadFromJsonFile(ROUTE53_PROD_DOCS_CLIENT_DNS_CONFIG_FILE, DnsConfigBuilder.class);
	}

	@Test
	void testValidateResourcePathInvalid() {
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DnsBuilderMain.validateResourcePath("someBadPath");
		});
		assertEquals("Invalid resource path specified", thrown.getMessage());
	}
}
