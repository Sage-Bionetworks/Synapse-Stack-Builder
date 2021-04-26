package org.sagebionetworks.template.repo.beanstalk;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class LoadBalancerAlarmsConfigTest {

	@Test
	public void testConfigFromFile() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		LoadBalancerAlarmsConfig config = injector.getInstance(LoadBalancerAlarmsConfig.class);
		assertNotNull(config);
	}

}
