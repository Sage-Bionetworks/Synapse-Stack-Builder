package org.sagebionetworks.template.repo.cloudwatchlogs;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.DeletionPolicy;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

@ExtendWith(MockitoExtension.class)
public class CloudwatchLogsConfigValidatorTest {
    @Mock
    private CloudwatchLogsConfig mockConfig;
    @Mock
    private Configuration mockProps;

    @InjectMocks
    private CloudwatchLogsConfigValidator validator;

    @Test
    public void testMissingEnvironment() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        List<LogDescriptor> logDescriptors = new ArrayList<>();
        LogDescriptor LogDescriptor = new LogDescriptor();
        logDescriptors.add(LogDescriptor);
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, logDescriptors);
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);

        // call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            validator.validate();
        }).getMessage();

        assertEquals("All environments types should appear once in configuration.", errorMessage);
    }

    @Test
    public void testInvalidNumberOfDescriptors() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        List<LogType> logTypes = new ArrayList<LogType>();
        logTypes.add(LogType.SERVICE);
        // Portal only has one desc
        envLogDescriptors.put(EnvironmentType.PORTAL, generateEnvironmentLogs(logTypes));
        logTypes.add(LogType.CATALINA);
        // Others have 2
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, generateEnvironmentLogs(logTypes));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_WORKERS, generateEnvironmentLogs(logTypes));
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);
        
        when(mockProps.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");

        // call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            validator.validate();
        }).getMessage();

        assertEquals("Each environment should have 2 logGroups.", errorMessage);
    }

    @Test
    public void testvalidateEnvironmentDuplicateLogType() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        List<LogType> logTypes = new ArrayList<LogType>();
        logTypes.add(LogType.SERVICE);
        logTypes.add(LogType.SERVICE);
        envLogDescriptors.put(EnvironmentType.PORTAL, generateEnvironmentLogs(logTypes));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, generateEnvironmentLogs(logTypes));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_WORKERS, generateEnvironmentLogs(logTypes));
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);

        // call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            validator.validate();
        }).getMessage();

        assertEquals("Each LogType can only appear once per environment.", errorMessage);
    }

    @Test
    public void testLogDescriptorMissingLogPath() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        List<LogType> logTypes = new ArrayList<>();
        logTypes.add(LogType.SERVICE);
        logTypes.add(LogType.TRACE);
        // 2 log descriptors for each environment
        envLogDescriptors.put(EnvironmentType.REPOSITORY_WORKERS, generateEnvironmentLogs(logTypes));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, generateEnvironmentLogs(logTypes));
        envLogDescriptors.put(EnvironmentType.PORTAL, generateEnvironmentLogs(logTypes));
        // sabotage one of them
        envLogDescriptors.get(EnvironmentType.REPOSITORY_WORKERS).get(0).setLogPath(null);
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);

        // call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            validator.validate();
        }).getMessage();

        assertEquals("Invalid log descriptor", errorMessage);
    }

    @Test
    public void testValidEnvironmentsWithDev() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        List<LogType> logTypes = Arrays.asList(LogType.SERVICE, LogType.TRACE);
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, generateEnvironmentLogs(logTypes));
        envLogDescriptors.put(EnvironmentType.PORTAL, generateEnvironmentLogs(logTypes));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_WORKERS, generateEnvironmentLogs(logTypes));
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);
        
        when(mockProps.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");

        // call under test
        CloudwatchLogsConfig result = validator.validate();
        validateDeletionPolicy(result, DeletionPolicy.Delete);
    }
    
    @Test
    public void testValidEnvironmentsWithProd() {
        Map<EnvironmentType, List<LogDescriptor>> envLogDescriptors = new HashMap<>();
        List<LogType> logTypes = Arrays.asList(LogType.SERVICE, LogType.TRACE);
        envLogDescriptors.put(EnvironmentType.REPOSITORY_SERVICES, generateEnvironmentLogs(logTypes));
        envLogDescriptors.put(EnvironmentType.PORTAL, generateEnvironmentLogs(logTypes));
        envLogDescriptors.put(EnvironmentType.REPOSITORY_WORKERS, generateEnvironmentLogs(logTypes));
        when(mockConfig.getLogDescriptors()).thenReturn(envLogDescriptors);
        
        when(mockProps.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("prod");

        // call under test
        CloudwatchLogsConfig result = validator.validate();
        validateDeletionPolicy(result, DeletionPolicy.Retain);
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

    private List<LogDescriptor> generateEnvironmentLogs(List<LogType> logTypes) {
        List<LogDescriptor> LogDescriptors = new ArrayList<>();
        for (LogType logType: logTypes) {
            LogDescriptor LogDescriptor = new LogDescriptor();
            LogDescriptor.setLogType(logType);
            LogDescriptor.setLogPath("someName");
            LogDescriptor.setDateFormat("YYYY-MM-DD");
            LogDescriptors.add(LogDescriptor);
  
        }
        return LogDescriptors;
    }

}
