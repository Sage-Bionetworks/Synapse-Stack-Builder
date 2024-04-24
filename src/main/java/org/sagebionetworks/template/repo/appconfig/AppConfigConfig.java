package org.sagebionetworks.template.repo.appconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Class used to deserialize JSON config. This class is immutable.
 */
public class AppConfigConfig {
    private final List<AppConfigDescriptor> appConfigDescriptors;
    @JsonCreator
    public AppConfigConfig(@JsonProperty(value = "appConfigDescriptors", required = true) List<AppConfigDescriptor> appConfigDescriptors) {
        AppConfigConfigValidator validator = new AppConfigConfigValidator();
        validator.validateConfigs(appConfigDescriptors);
        this.appConfigDescriptors = appConfigDescriptors;
    }
    public List<AppConfigDescriptor> getAppConfigConfigurations(){
        return appConfigDescriptors;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AppConfigConfig other = (AppConfigConfig) obj;
        return Objects.equals(appConfigDescriptors, other.appConfigDescriptors);
    }

    @Override
    public String toString() {
        return "AppConfigConfig [appConfigDescriptors=" + appConfigDescriptors + "]";
    }
}
