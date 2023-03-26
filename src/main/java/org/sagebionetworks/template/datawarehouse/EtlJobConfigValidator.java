package org.sagebionetworks.template.datawarehouse;

import org.apache.commons.lang3.StringUtils;

public class EtlJobConfigValidator {

    private EtlJobConfig etlJobConfig;

    public EtlJobConfigValidator(EtlJobConfig etlJobConfig) {
        this.etlJobConfig = etlJobConfig;
    }

    public EtlJobConfig validate() {
        etlJobConfig.getEtlJobDescriptors().forEach(this::validateStream);
        return etlJobConfig;
    }

    private void validateStream(EtlJobDescriptor etlJobDescriptor) {
        if (StringUtils.isBlank(etlJobDescriptor.getName())) {
            throw new IllegalStateException("The etl job name cannot be empty");
        }
        if (StringUtils.isBlank(etlJobDescriptor.getDescription())) {
            throw new IllegalStateException("The etl job description cannot be empty");
        }
        if (StringUtils.isBlank(etlJobDescriptor.getScriptLocation())) {
            throw new IllegalStateException("The etl job script location cannot be empty");
        }
        if (StringUtils.isBlank(etlJobDescriptor.getScriptName())) {
            throw new IllegalStateException("The etl job script name cannot be empty");
        }
        if (StringUtils.isBlank(etlJobDescriptor.getSourcePath())) {
            throw new IllegalStateException("The etl s3 source path cannot be empty");
        }
    }
}
