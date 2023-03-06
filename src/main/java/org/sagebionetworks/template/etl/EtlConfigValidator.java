package org.sagebionetworks.template.etl;

import org.apache.commons.lang3.StringUtils;

public class EtlConfigValidator {

    private EtlConfig etlConfig;

    public EtlConfigValidator(EtlConfig etlConfig) {
        this.etlConfig = etlConfig;
    }

    public EtlConfig validate() {
        etlConfig.getEtlDescriptors().forEach(this::validateStream);
        return etlConfig;
    }

    private void validateStream(EtlDescriptor etlDescriptor) {
        if (StringUtils.isBlank(etlDescriptor.getName())) {
            throw new IllegalStateException("The etl job name cannot be empty");
        }
        if (StringUtils.isBlank(etlDescriptor.getDescription())) {
            throw new IllegalStateException("The etl job description cannot be empty");
        }
        if (StringUtils.isBlank(etlDescriptor.getScriptLocation())) {
            throw new IllegalStateException("The etl job script location cannot be empty");
        }
        if (StringUtils.isBlank(etlDescriptor.getScriptName())) {
            throw new IllegalStateException("The etl job script name cannot be empty");
        }
        if (StringUtils.isBlank(etlDescriptor.getSourcePath())) {
            throw new IllegalStateException("The etl s3 source path cannot be empty");
        }
        if (StringUtils.isBlank(etlDescriptor.getDestinationPath())) {
            throw new IllegalStateException("The etl s3 destination path cannot be empty");
        }
        if (StringUtils.isBlank(etlDescriptor.getDestinationFileFormat())) {
            throw new IllegalStateException("The etl s3 destination file format cannot be empty");
        }
    }
}
