package org.sagebionetworks.template.etl;

import org.sagebionetworks.template.repo.kinesis.firehose.GlueTableDescriptor;

import java.util.Objects;

public class EtlDescriptor {
    private String name;
    private String description;
    private String scriptLocation;
    private String scriptName;
    private String sourcePath;
    private String destinationPath;
    private String destinationFileFormat;
    private GlueTableDescriptor tableDescriptor = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScriptLocation() {
        return scriptLocation;
    }

    public void setScriptLocation(String scriptLocation) {
        this.scriptLocation = scriptLocation;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getDestinationFileFormat() {
        return destinationFileFormat;
    }

    public void setDestinationFileFormat(String destinationFileFormat) {
        this.destinationFileFormat = destinationFileFormat;
    }

    public GlueTableDescriptor getTableDescriptor() {
        return tableDescriptor;
    }

    public void setTableDescriptor(GlueTableDescriptor tableDescriptor) {
        this.tableDescriptor = tableDescriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtlDescriptor that = (EtlDescriptor) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description)
                && Objects.equals(scriptLocation, that.scriptLocation)
                && Objects.equals(scriptName, that.scriptName)
                && Objects.equals(sourcePath, that.sourcePath)
                && Objects.equals(destinationPath, that.destinationPath)
                && Objects.equals(destinationFileFormat, that.destinationFileFormat)
                && Objects.equals(tableDescriptor, that.tableDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, scriptLocation, scriptName, sourcePath, destinationPath,
                destinationFileFormat, tableDescriptor);
    }
}