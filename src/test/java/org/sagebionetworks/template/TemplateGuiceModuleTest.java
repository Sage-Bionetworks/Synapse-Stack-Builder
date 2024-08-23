package org.sagebionetworks.template;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
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
