package org.sagebionetworks.template.repo.cloudwatchlogs;

import java.util.List;

import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import com.google.inject.Inject;

public class CloudwatchLogsVelocityContextProviderImpl implements CloudwatchLogsVelocityContextProvider {

    private CloudwatchLogsConfig config;

    @Inject
    public CloudwatchLogsVelocityContextProviderImpl(CloudwatchLogsConfig config) {
        this.config = config;
    }

    @Override
    public List<LogDescriptor> getLogDescriptors(EnvironmentType envType) {
        return config.getLogDescriptors(envType);
    }

}
