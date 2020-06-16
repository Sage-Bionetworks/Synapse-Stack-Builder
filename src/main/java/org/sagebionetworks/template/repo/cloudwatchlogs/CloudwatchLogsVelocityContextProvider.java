package org.sagebionetworks.template.repo.cloudwatchlogs;

import com.google.inject.Inject;
import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.repo.VelocityContextProvider;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import java.util.List;

public interface CloudwatchLogsVelocityContextProvider<T> {
    List<T> getLogDescriptors(EnvironmentType envType);
}
