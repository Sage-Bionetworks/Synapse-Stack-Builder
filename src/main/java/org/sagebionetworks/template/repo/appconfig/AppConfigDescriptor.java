package org.sagebionetworks.template.repo.appconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Config read from JSON for appConfig
 */
public class AppConfigDescriptor {
    final String appConfigName;
    final String appConfigDescription;
    final String appConfigDefaultConfiguration;
    @JsonCreator
    public AppConfigDescriptor(@JsonProperty(value = "appConfigName", required=true) String appConfigName,
                              @JsonProperty(value = "appConfigDescription", required=true) String appConfigDescription,
                              @JsonProperty(value = "appConfigDefaultConfiguration", required=true) String appConfigDefaultConfiguration) {

        this.appConfigName = appConfigName;
        this.appConfigDescription = appConfigDescription;
        this.appConfigDefaultConfiguration = appConfigDefaultConfiguration;
    }

    ///////////////////////////////////////////
    // getters are only used by Apache Velocity
    ///////////////////////////////////////////
    public String getAppConfigName(){return appConfigName;}
    public String getAppConfigDescription(){return appConfigDescription;}
    public String getAppConfigDefaultConfiguration(){return appConfigDefaultConfiguration;}
    @Override
    public String toString() {
        return "AppConfigDescriptor [appConfigName=" + appConfigName + ", appConfigDescription=" + appConfigDescription
                + ", appConfigDefaultConfiguration=" + appConfigDefaultConfiguration + "]";
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AppConfigDescriptor)) {
            return false;
        }
        AppConfigDescriptor other = (AppConfigDescriptor) obj;
        return Objects.equals(appConfigName, other.appConfigName)
                && Objects.equals(appConfigDescription, other.appConfigDescription)
                && Objects.equals(appConfigDefaultConfiguration, other.appConfigDefaultConfiguration);
    }

}
