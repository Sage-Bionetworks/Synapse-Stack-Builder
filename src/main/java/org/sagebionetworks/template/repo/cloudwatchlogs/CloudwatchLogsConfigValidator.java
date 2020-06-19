package org.sagebionetworks.template.repo.cloudwatchlogs;

import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CloudwatchLogsConfigValidator {

    private CloudwatchLogsConfig config;

    public CloudwatchLogsConfigValidator(CloudwatchLogsConfig config) {
        this.config =  config;
    }

    public CloudwatchLogsConfig validate() {
        Map<EnvironmentType, List<LogDescriptor>> logDescriptors = config.getLogDescriptors();
        validateEnvironments(logDescriptors);
        for (EnvironmentType k: logDescriptors.keySet()) {
            List<LogDescriptor> descriptors = logDescriptors.get(k);
            if (descriptors.size() != 3) {
                throw new IllegalStateException("Each environment should have 3 logGroups");
            }
            for (LogDescriptor d: descriptors) {
                this.validateLogDescriptor(d);
            }
        }
        return config;
    }

    private void validateLogDescriptor(LogDescriptor logDescriptor) {
        if (!(logDescriptor.getLogType() != null &&
                logDescriptor.getLogPath() != null &&
                logDescriptor.getDateFormat() != null)) {
            throw new IllegalStateException("Invalid logGroupDescriptor!");
        }
    }

    private void validateEnvironments(Map<EnvironmentType, List<LogDescriptor>> logDescriptors) {
        Set<EnvironmentType> environmentTypes = logDescriptors.keySet();
        if (! (environmentTypes.containsAll(Arrays.asList(EnvironmentType.values())) && environmentTypes.size()==3)) {
            throw new IllegalStateException("All environments types should appear once in configuration.");
        }
    }
}
