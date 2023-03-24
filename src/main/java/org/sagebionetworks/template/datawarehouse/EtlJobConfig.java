package org.sagebionetworks.template.datawarehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class EtlJobConfig {
    private List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();

    public List<EtlJobDescriptor> getEtlDescriptors() {
        return etlJobDescriptors;
    }

    public void setEtlDescriptors(List<EtlJobDescriptor> etlJobDescriptors) {
        this.etlJobDescriptors = etlJobDescriptors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtlJobConfig etlJobConfig = (EtlJobConfig) o;
        return Objects.equals(etlJobDescriptors, etlJobConfig.etlJobDescriptors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(etlJobDescriptors);
    }
}
