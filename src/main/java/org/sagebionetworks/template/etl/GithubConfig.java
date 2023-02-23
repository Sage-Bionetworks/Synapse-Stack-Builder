package org.sagebionetworks.template.etl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class GithubConfig {
    private List<GithubPath> githubPathList = new ArrayList<>();

    public List<GithubPath> getGithubPathList() {
        return githubPathList;
    }

    public void setGithubPathList(List<GithubPath> githubPathList) {
        this.githubPathList = githubPathList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GithubConfig that = (GithubConfig) o;
        return githubPathList.equals(that.githubPathList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(githubPathList);
    }
}
