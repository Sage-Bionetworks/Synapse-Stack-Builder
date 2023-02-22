package org.sagebionetworks.template.etl;

import org.apache.commons.lang3.StringUtils;

public class GithubConfigValidator {

    private GithubConfig githubConfig;

    public GithubConfigValidator(GithubConfig githubConfig) {
        this.githubConfig = githubConfig;
    }

    public GithubConfig validate() {
        if (StringUtils.isBlank(githubConfig.getGithubPath())) {
            throw new IllegalStateException("The github path cannot be empty");
        }
        if (StringUtils.isBlank(githubConfig.getFilename())) {
            throw new IllegalStateException("The github file name cannot be empty");
        }
        if (StringUtils.isBlank(githubConfig.getVersion())) {
            throw new IllegalStateException("The github file version cannot be empty");
        }
        return githubConfig;
    }
}
