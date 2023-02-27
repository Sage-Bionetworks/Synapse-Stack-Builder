package org.sagebionetworks.template.etl;

import java.util.Objects;

public class EtlDescriptor {
    private String name;
    private String description;
    private String scriptLocation;
    private String sourcePath;
    private String destinationPath;
    private String destinationFileFormat;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtlDescriptor that = (EtlDescriptor) o;
        return name.equals(that.name) && description.equals(that.description)
                && scriptLocation.equals(that.scriptLocation) && sourcePath.equals(that.sourcePath)
                && destinationPath.equals(that.destinationPath) && destinationFileFormat.equals(that.destinationFileFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, scriptLocation, sourcePath, destinationPath,
                destinationFileFormat);
    }
}
