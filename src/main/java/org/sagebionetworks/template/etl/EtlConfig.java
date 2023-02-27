package org.sagebionetworks.template.etl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class EtlConfig {
    private List<EtlDescriptor> etlDescriptors = new ArrayList<>();

    public List<EtlDescriptor> getEtlDescriptors() {
        return etlDescriptors;
    }

    public void setEtlDescriptors(List<EtlDescriptor> etlDescriptors) {
        this.etlDescriptors = etlDescriptors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtlConfig etlConfig = (EtlConfig) o;
        return Objects.equals(etlDescriptors, etlConfig.etlDescriptors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(etlDescriptors);
    }
}
