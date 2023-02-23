package org.sagebionetworks.template.etl;

import org.apache.commons.lang3.StringUtils;

public class GithubConfigValidator {

    private GithubConfig githubConfig;

    public GithubConfigValidator(GithubConfig githubConfig) {
        this.githubConfig = githubConfig;
    }

    public GithubConfig validate(){
        githubConfig.getGithubPathList().forEach(this::validate);
        return githubConfig;
    }

    private void validate(GithubPath githubPath) {
        if (StringUtils.isBlank(githubPath.getBasePath())) {
            throw new IllegalStateException("The github base path cannot be empty");
        }
        if (StringUtils.isBlank(githubPath.getFilePath())) {
            throw new IllegalStateException("The github file path cannot be empty");
        }
        if (StringUtils.isBlank(githubPath.getFilename())) {
            throw new IllegalStateException("The github file name cannot be empty");
        }
        if (StringUtils.isBlank(githubPath.getVersion())) {
            throw new IllegalStateException("The github file version cannot be empty");
        }
    }
}
