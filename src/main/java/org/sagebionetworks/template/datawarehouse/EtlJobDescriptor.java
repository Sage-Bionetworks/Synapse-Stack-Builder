package org.sagebionetworks.template.datawarehouse;

import org.sagebionetworks.template.repo.glue.GlueTableDescriptor;

import java.util.Objects;

public class EtlJobDescriptor {
    private String name;
    private String description;
    private String scriptName;
    private String sourcePath;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtlJobDescriptor that = (EtlJobDescriptor) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description)
                && Objects.equals(scriptName, that.scriptName)
                && Objects.equals(sourcePath, that.sourcePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, scriptName, sourcePath);
    }
}