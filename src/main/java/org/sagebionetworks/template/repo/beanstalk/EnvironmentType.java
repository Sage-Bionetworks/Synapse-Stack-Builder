package org.sagebionetworks.template.repo.beanstalk;

public enum EnvironmentType {

	REPOSITORY_SERVICES("services-repository", "repo"),
	REPOSITORY_WORKERS("services-workers", "workers"),
	PORTAL("portal","portal");

	String pathName;
	String cnamePrefix;

	EnvironmentType(String path, String cnamePrefix) {
		this.pathName = path;
		this.cnamePrefix = cnamePrefix;
	}

	/**
	 * Create the URL used to download the given war file version from artifactory.
	 * 
	 * @param version
	 * @return
	 */
	public String createArtifactoryUrl(String version) {
		StringBuilder builder = new StringBuilder(
				"http://sagebionetworks.artifactoryonline.com/sagebionetworks/libs-releases-local/org/sagebionetworks");
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
	 * @return
	 */
	public String getShortName() {
		return this.cnamePrefix;
	}
}
