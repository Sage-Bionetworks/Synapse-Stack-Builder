package org.sagebionetworks.template.repo;

public enum Environment {

	REPOSITORY_SERVICES("services-repository"),
	REPOSITORY_WORKERS("services-workers"),
	PORTAL("portal");

	String path;

	Environment(String path) {
		this.path = path;
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
		builder.append(path);
		builder.append("/");
		builder.append(version);
		builder.append("/");
		builder.append(path);
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
		builder.append(path);
		builder.append("/");
		builder.append(path);
		builder.append("-");
		builder.append(version);
		builder.append(".war");
		return builder.toString();
	}
}
