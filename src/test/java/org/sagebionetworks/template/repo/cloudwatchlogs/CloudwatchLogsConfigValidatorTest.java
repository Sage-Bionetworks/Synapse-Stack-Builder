package org.sagebionetworks.template.repo.cloudwatchlogs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudwatchLogsConfigValidatorTest {
    @Mock
    CloudwatchLogsConfig mockConfig;

    @InjectMocks
    CloudwatchLogsConfigValidator validator;

    @Test(expected = IllegalStateException.class)
    public void testMissingEnvironment() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        List<LogDescriptor> LogDescriptors = new ArrayList<>();
        LogDescriptor LogDescriptor = new LogDescriptor();
        LogDescriptors.add(LogDescriptor);
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, LogDescriptors);
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);

        // call under test
        validator.validate();
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidNumberOfDescriptors() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, generateEnvironmentLogs(3));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_WORKERS, generateEnvironmentLogs(3));
        envLogDescriptors.put(EnvironmentType.PORTAL, generateEnvironmentLogs(2));
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);

        // call under test
        validator.validate();
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidRetentionLogDescriptor() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, generateEnvironmentLogs(3));
        envLogDescriptors.put(EnvironmentType.PORTAL, generateEnvironmentLogs(3));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_WORKERS, generateEnvironmentLogs(2));
        LogDescriptor LogDescriptor = new LogDescriptor();
        LogDescriptor.setLogPath("someName");
        LogDescriptor.setRetentionInDays(8);
        envLogDescriptors.get(EnvironmentType.REPOSITORY_WORKERS).add(LogDescriptor);
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);

        // call under test
        validator.validate();
    }

    @Test(expected = IllegalStateException.class)
    public void testNullNameLogDescriptor() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, generateEnvironmentLogs(3));
        envLogDescriptors.put(EnvironmentType.PORTAL, generateEnvironmentLogs(3));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_WORKERS, generateEnvironmentLogs(2));
        LogDescriptor LogDescriptor = new LogDescriptor();
        LogDescriptor.setRetentionInDays(8);
        envLogDescriptors.get(EnvironmentType.REPOSITORY_WORKERS).add(LogDescriptor);
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);

        // call under test
        validator.validate();
    }

    @Test
    public void testValidEnvironments() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, generateEnvironmentLogs(3));
        envLogDescriptors.put(EnvironmentType.PORTAL, generateEnvironmentLogs(3));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_WORKERS, generateEnvironmentLogs(3));
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);

        // call under test
        validator.validate();
    }

    private List<LogDescriptor> generateEnvironmentLogs(int numDescriptors) {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        List<LogDescriptor> LogDescriptors = new ArrayList<>();
        LogDescriptor LogDescriptor = new LogDescriptor();
        LogDescriptor.setLogType(LogType.SERVICE);
        LogDescriptor.setLogPath("someName");
        LogDescriptor.setRetentionInDays(7);
        LogDescriptor.setDateFormat("YYYY-MM-DD");
        LogDescriptor.setMultilineStartPattern("{datetime}");
        for (int i = 0; i < numDescriptors; i++) {
            LogDescriptors.add(LogDescriptor);
        }
        return LogDescriptors;
    }

}