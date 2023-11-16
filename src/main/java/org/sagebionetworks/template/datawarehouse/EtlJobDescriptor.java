package org.sagebionetworks.template.datawarehouse;

import org.sagebionetworks.template.repo.glue.GlueTableDescriptor;

import java.util.Objects;

public class EtlJobDescriptor {
    private String name;
    private String description;
    private String scriptName;
    private String sourcePath;
    private String targetTable;

    public String getName() {
        return name;
    }

    public EtlJobDescriptor withName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EtlJobDescriptor withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getScriptName() {
        return scriptName;
    }

    public EtlJobDescriptor withScriptName(String scriptName) {
        this.scriptName = scriptName;
        return this;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public EtlJobDescriptor withSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
        return this;
    }
    
    public String getTargetTable() {
		return targetTable;
	}
    
    public EtlJobDescriptor withTargetTable(String targetTable) {
		this.targetTable = targetTable;
		return this;
	}

    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof EtlJobDescriptor)) {
			return false;
		}
		EtlJobDescriptor other = (EtlJobDescriptor) obj;
		return Objects.equals(description, other.description) && Objects.equals(name, other.name)
				&& Objects.equals(scriptName, other.scriptName) && Objects.equals(sourcePath, other.sourcePath)
				&& Objects.equals(targetTable, other.targetTable);
	}

    @Override
	public int hashCode() {
		return Objects.hash(description, name, scriptName, sourcePath, targetTable);
	}
}