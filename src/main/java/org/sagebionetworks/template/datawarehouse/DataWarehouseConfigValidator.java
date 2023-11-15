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
        	ValidateArgument.requiredNotBlank(job.getTargetTable(), "The target table");
        	ValidateArgument.requirement(tableNames.contains(job.getTargetTable()), "The table " + job.getTargetTable()+ " defined for job " + job.getName() + " is not defined");
        });
        
        return dataWarehouseConfig;
    }

}
