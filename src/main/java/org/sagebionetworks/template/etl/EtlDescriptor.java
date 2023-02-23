package org.sagebionetworks.template.etl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EtlDescriptor {
    private String name;
    private String description;
    private String scriptLocation;
    private String sourcePath;
    private String destinationPath;
    private String destinationFileFormat;
    private Set<String> buckets = new HashSet<>();

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

    public Set<String> getBuckets() {
        return buckets;
    }

    public void setBuckets(Set<String> buckets) {
        this.buckets = buckets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtlDescriptor that = (EtlDescriptor) o;
        return name.equals(that.name) && description.equals(that.description)
                && scriptLocation.equals(that.scriptLocation) && sourcePath.equals(that.sourcePath)
                && destinationPath.equals(that.destinationPath) && destinationFileFormat.equals(that.destinationFileFormat)
                && buckets.equals(that.buckets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, scriptLocation, sourcePath, destinationPath,
                destinationFileFormat, buckets);
    }
}
