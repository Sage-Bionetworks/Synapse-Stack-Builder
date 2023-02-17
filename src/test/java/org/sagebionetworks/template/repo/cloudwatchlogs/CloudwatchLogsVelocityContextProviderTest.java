package org.sagebionetworks.template.repo.cloudwatchlogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.repo.DeletionPolicy;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

@ExtendWith(MockitoExtension.class)
public class CloudwatchLogsVelocityContextProviderTest {

    @Mock
    CloudwatchLogsConfig mockConfig;

    @InjectMocks
    CloudwatchLogsVelocityContextProviderImpl contextProvider;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        List<LogDescriptor> l = new ArrayList<>();
        LogDescriptor d = new LogDescriptor();
        d.setLogType(LogType.SERVICE);
        d.setLogPath("/var/mypath.log");
        d.setDeletionPolicy(DeletionPolicy.Delete);
        l.add(d);
        when(mockConfig.getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES)).thenReturn(l);
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetLogDescriptors() {
        List<LogDescriptor> l = contextProvider.getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES);
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals(LogType.SERVICE, l.get(0).getLogType());
        assertEquals(DeletionPolicy.Delete.name(), l.get(0).getDeletionPolicy());
    }
}
