package org.sagebionetworks.template.etl;


import org.sagebionetworks.template.ConfigurationPropertyNotFound;

public interface GithubCopy {

    String copyFileFromGithub() throws ConfigurationPropertyNotFound;
}
