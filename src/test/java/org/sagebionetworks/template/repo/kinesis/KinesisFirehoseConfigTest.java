package org.sagebionetworks.template.repo.kinesis;

import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.kinesis.firehose.KinesisFirehoseConfig;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class KinesisFirehoseConfigTest {
	
	@Test
	public void testConfigFromFile() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		KinesisFirehoseConfig config = injector.getInstance(KinesisFirehoseConfig.class);
		assertNotNull(config);
	}

}
