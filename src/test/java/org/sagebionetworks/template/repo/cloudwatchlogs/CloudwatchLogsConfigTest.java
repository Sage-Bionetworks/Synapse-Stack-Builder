package org.sagebionetworks.template.repo.cloudwatchlogs;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class CloudwatchLogsConfigTest {

    @Test
    public void testConfigFromFile() {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        CloudwatchLogsConfig config = injector.getInstance(CloudwatchLogsConfig.class);
        assertNotNull(config);
    }

}
