package org.sagebionetworks.template.datawarehouse;

import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.template.repo.glue.GlueTableDescriptor;
import org.sagebionetworks.util.ValidateArgument;

public class DataWarehouseConfigValidator {

    private DataWarehouseConfig dataWarehouseConfig;

    public DataWarehouseConfigValidator(DataWarehouseConfig dataWarehouseConfig) {
        this.dataWarehouseConfig = dataWarehouseConfig;
    }

    public DataWarehouseConfig validate() {
    	ValidateArgument.requiredNotBlank(dataWarehouseConfig.getGithubRepo(), "The github repository");
    	ValidateArgument.requiredNotBlank(dataWarehouseConfig.getVersion(), "The version");
    	
    	Set<String> tableNames = dataWarehouseConfig.getTableDescriptors().stream().map(GlueTableDescriptor::getName).collect(Collectors.toSet());
    	
        dataWarehouseConfig.getEtlJobDescriptors().forEach( job -> {
        	ValidateArgument.requiredNotBlank(job.getName(), "The name");
        	ValidateArgument.requiredNotBlank(job.getDescription(), "The description");
        	ValidateArgument.requiredNotBlank(job.getScriptName(), "The script name");
        	ValidateArgument.requiredNotBlank(job.getSourcePath(), "The source path");	
        	ValidateArgument.requirement(tableNames.contains(job.getName()), "No table defined for job " + job.getName());
        });
        
        return dataWarehouseConfig;
    }

}
