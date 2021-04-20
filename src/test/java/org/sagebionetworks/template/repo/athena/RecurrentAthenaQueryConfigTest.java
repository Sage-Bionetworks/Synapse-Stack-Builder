package org.sagebionetworks.template.repo.athena;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.queues.SqsQueueDescriptor;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class RecurrentAthenaQueryConfigTest {

	@Test
	public void testConfigFromFile() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		RecurrentAthenaQueryConfig config = injector.getInstance(RecurrentAthenaQueryConfig.class);
		assertNotNull(config);
	}
	

	static RecurrentAthenaQuery query(String name, String path, String cronExpression, String destinationQueue) {
		RecurrentAthenaQuery query = new RecurrentAthenaQuery();
		query.setQueryName(name);
		query.setQueryPath(path);
		query.setCronExpression(cronExpression);
		query.setDestinationQueue(destinationQueue);
		return query;
	}
	
	static SqsQueueDescriptor queue(String name) {
		return new SqsQueueDescriptor(name, Collections.emptyList(), 12, 12, 12);
	}

}
