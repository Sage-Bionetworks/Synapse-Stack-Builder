package org.sagebionetworks.template.repo.cloudwatchlogs;

import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import java.util.List;

public interface CloudwatchLogsVelocityContextProvider {
    public List<LogDescriptor> getLogDescriptors(EnvironmentType envType);
}
