package org.sagebionetworks.template.repo.cloudwatchlogs;

import com.google.inject.Inject;
import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.repo.VelocityContextProvider;

public class CloudwatchLogsVelocityContextProvider  implements VelocityContextProvider {

    private CloudwatchLogsConfig config;

    @Inject
    public CloudwatchLogsVelocityContextProvider(CloudwatchLogsConfig config) {
        this.config = config;
    }

    @Override
    public void addToContext(VelocityContext context) {

    }
}
