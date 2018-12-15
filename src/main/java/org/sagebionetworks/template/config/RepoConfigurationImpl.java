package org.sagebionetworks.template.config;

import static org.sagebionetworks.template.Constants.DEFAULT_REPO_PROPERTIES;

public class RepoConfigurationImpl extends ConfigurationImpl implements RepoConfiguration{
	public RepoConfigurationImpl(){
		super();
		initializeWithDefaults(DEFAULT_REPO_PROPERTIES);
	}
}
