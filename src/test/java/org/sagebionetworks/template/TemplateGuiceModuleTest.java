package org.sagebionetworks.template;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.sagebionetworks.template.vpc.VpcTemplateBuilder;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TemplateGuiceModuleTest {

	@Test
	public void testInjector() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		VpcTemplateBuilder builder = injector.getInstance(VpcTemplateBuilder.class);
		assertNotNull(builder);
	}
}
