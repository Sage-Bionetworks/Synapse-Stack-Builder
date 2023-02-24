package org.sagebionetworks.template.etl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class GithubConfig {
    private List<GithubPath> githubPath = new ArrayList<>();

    public List<GithubPath> getGithubPath() {
        return githubPath;
    }

    public void setGithubPath(List<GithubPath> githubPath) {
        this.githubPath = githubPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GithubConfig that = (GithubConfig) o;
        return githubPath.equals(that.githubPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(githubPath);
    }
}
