package org.sagebionetworks.template.repo.appconfig;

import org.sagebionetworks.util.ValidateArgument;

import java.util.List;

public class AppConfigConfigValidator {
    private AppConfigConfig config;
    public AppConfigConfigValidator(AppConfigConfig config){ this.config = config;}
    public AppConfigConfig validate(){
        List<AppConfigDescriptor> appConfig = config.getAppConfigDescriptors();
        appConfig.forEach(this::validateConfig);
        return config;
    }
    public void validateConfig(AppConfigDescriptor configDescriptor){
        ValidateArgument.requiredNotBlank(configDescriptor.appConfigName, "The appConfig name");
        ValidateArgument.requiredNotBlank(configDescriptor.appConfigDescription, "The appConfig description");
        ValidateArgument.requiredNotBlank(configDescriptor.appConfigDefaultConfiguration, "The appConfig default configuration");
    }

}
