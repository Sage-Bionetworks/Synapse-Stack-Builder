package org.sagebionetworks.template.repo.beanstalk;

public enum EnvironmentType {

	REPOSITORY_SERVICES("services-repository", "repo", "SynapesRepoWorkersInstanceProfile", true),
	REPOSITORY_WORKERS("services-workers", "workers","SynapesRepoWorkersInstanceProfile", true),
	PORTAL("portal", "portal", "SynapesPortalInstanceProfile", false);

	String pathName;
	String cnamePrefix;
	String instanceProfileSuffix;
	boolean includeSecrets;

	EnvironmentType(String path, String cnamePrefix, String instanceProfileSuffix, boolean includeSecrets) {
		this.pathName = path;
		this.cnamePrefix = cnamePrefix;
		this.instanceProfileSuffix = instanceProfileSuffix;
		this.includeSecrets = includeSecrets;
	}

	/**
	 * Create the URL used to download the given war file version from artifactory.
	 * 
	 * @param version
	 * @return
	 */
	public String createArtifactoryUrl(String version) {
		StringBuilder builder = new StringBuilder(
				"https://sagebionetworks.jfrog.io/sagebionetworks/libs-releases-local/org/sagebionetworks");
		builder.append("/");
		builder.append(pathName);
		builder.append("/");
		builder.append(version);
		builder.append("/");
		builder.append(pathName);
		builder.append("-");
		builder.append(version);
		builder.append(".war");
		return builder.toString();
	}

	/**
	 * Create the S3 key to be used for the given ware file version.
	 * 
	 * @param version
	 * @return
	 */
	public String createS3Key(String version) {
		StringBuilder builder = new StringBuilder("versions");
		builder.append("/");
		builder.append(pathName);
		builder.append("/");
		builder.append(pathName);
		builder.append("-");
		builder.append(version);
		builder.append(".war");
		return builder.toString();
	}

	/**
	 * Get the short name for this type.
	 * 
	 * @return
	 */
	public String getShortName() {
		return this.cnamePrefix;
	}

	/**
	 * The suffix of Instance Profile that references the IAM service role for this
	 * type.
	 * 
	 * @return
	 */
	public String getInstanceProfileSuffix() {
		return this.instanceProfileSuffix;
	}
	
	/**
	 * Get the EnvironmentType matching the passed prefix.
	 * @param prefix
	 * @return
	 */
	public static EnvironmentType valueOfPrefix(String prefix) {
		for(EnvironmentType type: values()) {
			if(type.cnamePrefix.equals(prefix)){
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown prefix: "+prefix);
	}

	public boolean shouldIncludeSecrets() {
		return includeSecrets;
	}
}
