package org.sagebionetworks.template.repo.appconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.sagebionetworks.schema.adapter.JSONEntity;

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
                              @JsonProperty(value = "appConfigDefaultConfiguration", required=true) ObjectNode appConfigDefaultConfiguration) {

        this.appConfigName = appConfigName;
        this.appConfigDescription = appConfigDescription;

        ObjectMapper mapper = new ObjectMapper();
        try {
            // The template expects a string containing escaped JSON, so write the JSON to a string, then escape the string by writing it to a string again
            String jsonAsString = mapper.writeValueAsString(appConfigDefaultConfiguration);
            String escapedJsonString = mapper.writeValueAsString(jsonAsString);
            this.appConfigDefaultConfiguration = mapper.writeValueAsString(escapedJsonString);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize appConfigDefaultConfiguration", e);
        }
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
