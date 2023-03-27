package org.sagebionetworks.template.datawarehouse;


import org.sagebionetworks.template.ConfigurationPropertyNotFound;

public interface GithubCopy {

    String copyFileFromGithub() throws ConfigurationPropertyNotFound;
}
