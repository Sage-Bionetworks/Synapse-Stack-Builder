package org.sagebionetworks.template.etl;

import java.util.Objects;

public class GithubConfig {

    private String githubPath;
    private String filename;
    private String version;

    public String getGithubPath() {
        return githubPath;
    }

    public void setGithubPath(String githubPath) {
        this.githubPath = githubPath;
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
        GithubConfig that = (GithubConfig) o;
        return githubPath.equals(that.githubPath) && filename.equals(that.filename) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(githubPath, filename, version);
    }
}
