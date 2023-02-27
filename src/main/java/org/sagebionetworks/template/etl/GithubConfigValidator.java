package org.sagebionetworks.template.etl;

import io.jsonwebtoken.lang.Collections;
import org.apache.commons.lang3.StringUtils;

public class GithubConfigValidator {

    private GithubConfig githubConfig;

    public GithubConfigValidator(GithubConfig githubConfig) {
        this.githubConfig = githubConfig;
    }

    public GithubConfig validate() {
        if (StringUtils.isBlank(githubConfig.getBasePath())) {
            throw new IllegalStateException("The github base path cannot be empty");
        }
        if (Collections.isEmpty(githubConfig.getFilePaths())) {
            throw new IllegalStateException("The github file path list cannot be empty");
        }
        if (StringUtils.isBlank(githubConfig.getVersion())) {
            throw new IllegalStateException("The github file version cannot be empty");
        }
        return githubConfig;
    }
}
