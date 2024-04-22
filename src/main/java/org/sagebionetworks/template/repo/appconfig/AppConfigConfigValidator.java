package org.sagebionetworks.template.repo.appconfig;

import org.apache.commons.lang3.StringUtils;
import java.util.List;

public class AppConfigConfigValidator {
    public static void validateConfigs(List<AppConfigDescriptor> appConfigDescriptors){
        for(AppConfigDescriptor configDescriptor: appConfigDescriptors){
            validateConfig(configDescriptor);
        }
    }
    public static void validateConfig(AppConfigDescriptor configDescriptor){
        if (StringUtils.isBlank(configDescriptor.appConfigName)) {
            throw new IllegalArgumentException("The AppConfig name cannot be empty");
        }
        if (StringUtils.isBlank(configDescriptor.appConfigDescription)) {
            throw new IllegalArgumentException("The AppConfig description cannot be empty");
        }
        if (StringUtils.isBlank(configDescriptor.appConfigDefaultConfiguration)) {
            throw new IllegalArgumentException("The AppConfig default configuration cannot be empty");
        }
    }
}
