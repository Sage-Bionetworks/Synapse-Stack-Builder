package org.sagebionetworks.template.repo.appconfig;

import org.sagebionetworks.util.ValidateArgument;
import org.apache.commons.lang3.StringUtils;
import java.util.List;

public class AppConfigConfigValidator {
    public void validateConfigs(List<AppConfigDescriptor> appConfigDescriptors){
        appConfigDescriptors.forEach(this::validateConfig);
    }
    public void validateConfig(AppConfigDescriptor configDescriptor){
        ValidateArgument.requiredNotBlank(configDescriptor.appConfigName, "The appConfig name");
        ValidateArgument.requiredNotBlank(configDescriptor.appConfigDescription, "The appConfig description");
        ValidateArgument.requiredNotBlank(configDescriptor.appConfigDefaultConfiguration, "The appConfig default configuration");
    }
}
