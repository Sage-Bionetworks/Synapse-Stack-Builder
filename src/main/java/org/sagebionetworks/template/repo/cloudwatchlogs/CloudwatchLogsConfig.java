package org.sagebionetworks.template.repo.cloudwatchlogs;

import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudwatchLogsConfig {
    private Map<EnvironmentType, List<LogDescriptor>> logDescriptors = new HashMap<>();

    public void setLogDescriptors(Map<EnvironmentType, List<LogDescriptor>> descriptors) { this.logDescriptors = descriptors; }
    public Map<EnvironmentType, List<LogDescriptor>> getLogDescriptors() { return this.logDescriptors; }

    public List<LogDescriptor> getLogDescriptors(EnvironmentType type) {
        return this.logDescriptors.get(type);
    }

}
