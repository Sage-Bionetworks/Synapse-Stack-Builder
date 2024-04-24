package org.sagebionetworks.template.repo.appconfig;

import java.util.List;
import java.util.Objects;

public class AppConfigConfig {
    private List<AppConfigDescriptor> appConfigDescriptors;
    public AppConfigConfig() { }
    public void setAppConfigDescriptors(List<AppConfigDescriptor> appConfigDescriptors) {
        this.appConfigDescriptors = appConfigDescriptors;
    }
    public List<AppConfigDescriptor> getAppConfigDescriptors(){
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
