package org.sagebionetworks.template.repo.appconfig;

import static org.sagebionetworks.template.Constants.APPCONFIG_CONFIGURATIONS;

import org.apache.velocity.VelocityContext;
import org.sagebionetworks.template.repo.VelocityContextProvider;

import com.google.inject.Inject;

/**
 * Provides velocity context for AppConfig
 */

public class AppConfigVelocityContextProvider implements VelocityContextProvider  {
    AppConfigConfig appConfigConfig;

    @Inject
    public AppConfigVelocityContextProvider(AppConfigConfig appConfigConfig){
        this.appConfigConfig = appConfigConfig;
    }

    @Override
    public void addToContext(VelocityContext context) {
        context.put(APPCONFIG_CONFIGURATIONS, appConfigConfig.getAppConfigDescriptors());
    }
}
