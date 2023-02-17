package org.sagebionetworks.template.repo.cloudwatchlogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.DeletionPolicy;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class CloudwatchLogsConfigTest {

    @Test
    public void testConfigFromFileDev() {
    	System.getProperties().put("org.sagebionetworks.stack", "dev");
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        CloudwatchLogsConfig config = injector.getInstance(CloudwatchLogsConfig.class);
        assertNotNull(config);
        validateDeletionPolicy(config, DeletionPolicy.Delete);
    }
    
    @Test
    public void testConfigFromFileProd() {
    	System.getProperties().put("org.sagebionetworks.stack", "prod");
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        CloudwatchLogsConfig config = injector.getInstance(CloudwatchLogsConfig.class);
        assertNotNull(config);
        validateDeletionPolicy(config, DeletionPolicy.Retain);
    }

    /**
     * Helper to validate the DeletionPolicy for each log. 
     * @param config
     * @param expected
     */
    private void validateDeletionPolicy(CloudwatchLogsConfig config, DeletionPolicy expected) {
    	config.getLogDescriptors().values().forEach(l->{
    		l.forEach(d->{
    			assertEquals(expected.name(), d.getDeletionPolicy());
    		});
    	});
    }
}
