package org.sagebionetworks.template.etl;

import java.util.Objects;

public class GithubPath {
    private String basePath;
    private String filePath;
    private String filename;
    private String version;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GithubPath that = (GithubPath) o;
        return basePath.equals(that.basePath) && filePath.equals(that.filePath)
                && filename.equals(that.filename) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basePath, filePath, filename, version);
    }
}
