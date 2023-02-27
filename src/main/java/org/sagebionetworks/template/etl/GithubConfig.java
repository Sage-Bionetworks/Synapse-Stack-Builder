package org.sagebionetworks.template.etl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class GithubConfig {
    private String basePath;
    private List<String> filePaths = new ArrayList<>();
    private String version;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
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
        GithubConfig that = (GithubConfig) o;
        return basePath.equals(that.basePath) && filePaths.equals(that.filePaths) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basePath, filePaths, version);
    }
}
