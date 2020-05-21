package org.sagebionetworks.template.repo.cloudwatchlogs;

public class CloudwatchLogsConfigValidator {

    private CloudwatchLogsConfig config;

    public CloudwatchLogsConfigValidator(CloudwatchLogsConfig config) {
        this.config =  config;
    }

    public CloudwatchLogsConfig validate() {
        return config;
    }
}
