package org.sagebionetworks.template.etl;


import org.sagebionetworks.template.ConfigurationPropertyNotFound;

public interface GithubCopy {

    void copyFileFromGithub() throws ConfigurationPropertyNotFound;
}
