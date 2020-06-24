package org.sagebionetworks.template.repo.cloudwatchlogs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import java.util.List;

import static org.junit.Assert.*;

public class CloudwatchLogsConfigTest {

    @Test
    public void testConfigFromFile() {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        CloudwatchLogsConfig config = injector.getInstance(CloudwatchLogsConfig.class);
        assertNotNull(config);
    }

}
