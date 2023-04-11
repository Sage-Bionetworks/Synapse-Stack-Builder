package org.sagebionetworks.template.datawarehouse;

import org.sagebionetworks.util.ValidateArgument;

public class EtlJobConfigValidator {

    private EtlJobConfig etlJobConfig;

    public EtlJobConfigValidator(EtlJobConfig etlJobConfig) {
        this.etlJobConfig = etlJobConfig;
    }

    public EtlJobConfig validate() {
    	ValidateArgument.requiredNotBlank(etlJobConfig.getGithubRepo(), "The github repository");
    	ValidateArgument.requiredNotBlank(etlJobConfig.getVersion(), "The version");
        etlJobConfig.getEtlJobDescriptors().forEach(this::validateStream);
        return etlJobConfig;
    }

    private void validateStream(EtlJobDescriptor etlJobDescriptor) {
    	ValidateArgument.requiredNotBlank(etlJobDescriptor.getName(), "The name");
    	ValidateArgument.requiredNotBlank(etlJobDescriptor.getDescription(), "The description");
    	ValidateArgument.requiredNotBlank(etlJobDescriptor.getScriptName(), "The script name");
    	ValidateArgument.requiredNotBlank(etlJobDescriptor.getSourcePath(), "The source path");
    }
}
