package org.sagebionetworks.template.repo.beanstalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

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

	@Test
	public void testUnhealthyHostsAlarmForAllEnvironmentTypes() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		LoadBalancerAlarmsConfig config = injector.getInstance(LoadBalancerAlarmsConfig.class);
		for (EnvironmentType type : EnvironmentType.values()) {
			assertTrue("Missing UnHealthyHostCount alarm for " + type,
					config.getOrDefault(type, Collections.emptyList()).stream()
							.anyMatch(a -> "UnHealthyHostCount".equals(a.getMetric()) && a.isUseTargetGroupDimension()));
		}
	}

	@Test
	public void testWorkersNonHealthAlarmsHaveActionsDisabled() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		LoadBalancerAlarmsConfig config = injector.getInstance(LoadBalancerAlarmsConfig.class);
		List<LoadBalancerAlarm> workers = config.get(EnvironmentType.REPOSITORY_WORKERS);
		assertEquals(3, workers.size());
		for (LoadBalancerAlarm alarm : workers) {
			// Workers only receive health check traffic: only the unhealthy hosts alarm notifies
			assertEquals(alarm.getName(), !"UnHealthyHostCount".equals(alarm.getMetric()), alarm.isDisableActions());
		}
		// No other environment should have actions disabled
		config.entrySet().stream().filter(e -> e.getKey() != EnvironmentType.REPOSITORY_WORKERS)
				.flatMap(e -> e.getValue().stream())
				.forEach(a -> assertFalse(a.getName(), a.isDisableActions()));
	}

}
