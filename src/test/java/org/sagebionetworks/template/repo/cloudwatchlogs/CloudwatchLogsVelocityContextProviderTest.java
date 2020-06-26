package org.sagebionetworks.template.repo.cloudwatchlogs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudwatchLogsVelocityContextProviderTest {

    @Mock
    CloudwatchLogsConfig mockConfig;

    @InjectMocks
    CloudwatchLogsVelocityContextProviderImpl contextProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        List<LogDescriptor> l = new ArrayList<>();
        LogDescriptor d = new LogDescriptor();
        d.setLogType(LogType.SERVICE);
        d.setLogPath("/var/mypath.log");
        l.add(d);
        when(mockConfig.getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES)).thenReturn(l);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetLogDescriptors() {
        List<LogDescriptor> l = contextProvider.getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES);
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals(LogType.SERVICE, l.get(0).getLogType());
    }
}
