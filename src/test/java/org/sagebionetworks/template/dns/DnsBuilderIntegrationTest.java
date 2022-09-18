package org.sagebionetworks.template.dns;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.TemplateGuiceModule;

@ExtendWith(MockitoExtension.class)
public class DnsBuilderIntegrationTest {

	@Test
	void testLoadConfig() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());

		DnsConfig config = injector.getInstance(DnsConfig.class);
	}
}
