package org.sagebionetworks.template.repo.cloudwatchlogs;

import com.google.inject.Inject;
import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.repo.VelocityContextProvider;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import sun.rmi.runtime.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
