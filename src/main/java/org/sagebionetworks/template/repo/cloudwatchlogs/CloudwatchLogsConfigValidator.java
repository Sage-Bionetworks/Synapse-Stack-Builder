package org.sagebionetworks.template.repo.cloudwatchlogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.DeletionPolicy;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.util.ValidateArgument;

public class CloudwatchLogsConfigValidator {

    private final CloudwatchLogsConfig config;
    private final Configuration props;

    public CloudwatchLogsConfigValidator(CloudwatchLogsConfig config, Configuration props) {
        this.config =  config;
        this.props = props;
    }

    public CloudwatchLogsConfig validate() {
    	boolean isProd = Constants.isProd(props.getProperty(Constants.PROPERTY_KEY_STACK));
        Map<EnvironmentType, List<LogDescriptor>> logDescriptors = config.getLogDescriptors();
        validateEnvironments(logDescriptors);
        for (EnvironmentType k: logDescriptors.keySet()) {
            List<LogDescriptor> descriptors = logDescriptors.get(k);
            if (descriptors.size() != 2) {
                throw new IllegalStateException("Each environment should have 2 logGroups.");
            }
            List<LogType> types = new ArrayList<>();
            for (LogDescriptor d: descriptors) {
            	d.setDeletionPolicy(isProd ? DeletionPolicy.Retain : DeletionPolicy.Delete);
                this.validateLogDescriptor(d);
                if (types.contains(d.getLogType())) {
                    throw new IllegalStateException("Each LogType can only appear once per environment.");
                } else {
                    types.add(d.getLogType());
                }
            }
        }
        return config;
    }

    private void validateLogDescriptor(LogDescriptor logDescriptor) {
        try {
            ValidateArgument.required(logDescriptor.getLogType(), "LogType");
            ValidateArgument.required(logDescriptor.getDateFormat(), "dateFormat");
            ValidateArgument.required(logDescriptor.getLogPath(), "logPath");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid log descriptor", e);
        }
    }

    private void validateEnvironments(Map<EnvironmentType, List<LogDescriptor>> logDescriptors) {
        Set<EnvironmentType> environmentTypes = logDescriptors.keySet();
        if ((environmentTypes.size()!=3) ||
                (!environmentTypes.containsAll(Arrays.asList(EnvironmentType.values())))) {
            throw new IllegalStateException("All environments types should appear once in configuration.");
        }
    }
}
