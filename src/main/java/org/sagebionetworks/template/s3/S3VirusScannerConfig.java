package org.sagebionetworks.template.s3;

import java.util.Objects;

public class S3VirusScannerConfig {

	private String lambdaArtifactSourceUrl;

	private String lambdaArtifactBucket;
	
	private String lambdaArtifactKey;

	private String notificationEmail;

	public S3VirusScannerConfig() {
	}

	public String getLambdaArtifactSourceUrl() {
		return lambdaArtifactSourceUrl;
	}

	public void setLambdaArtifactSourceUrl(String lambdaArtifactSourceUrl) {
		this.lambdaArtifactSourceUrl = lambdaArtifactSourceUrl;
	}

	public String getLambdaArtifactBucket() {
		return lambdaArtifactBucket;
	}

	public void setLambdaArtifactBucket(String lambdaArtifactBucket) {
		this.lambdaArtifactBucket = lambdaArtifactBucket;
	}
	
	public String getLambdaArtifactKey() {
		return lambdaArtifactKey;
	}
	
	public void setLambdaArtifactKey(String lambdaArtifactKey) {
		this.lambdaArtifactKey = lambdaArtifactKey;
	}

	public String getNotificationEmail() {
		return notificationEmail;
	}

	public void setNotificationEmail(String notificationEmail) {
		this.notificationEmail = notificationEmail;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lambdaArtifactBucket, lambdaArtifactKey, lambdaArtifactSourceUrl, notificationEmail);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		S3VirusScannerConfig other = (S3VirusScannerConfig) obj;
		return Objects.equals(lambdaArtifactBucket, other.lambdaArtifactBucket)
				&& Objects.equals(lambdaArtifactKey, other.lambdaArtifactKey)
				&& Objects.equals(lambdaArtifactSourceUrl, other.lambdaArtifactSourceUrl)
				&& Objects.equals(notificationEmail, other.notificationEmail);
	}

	@Override
	public String toString() {
		return "S3VirusScannerConfig [lambdaArtifactSourceUrl=" + lambdaArtifactSourceUrl + ", lambdaArtifactBucket=" + lambdaArtifactBucket
				+ ", lambdaArtifactKey=" + lambdaArtifactKey + ", notificationEmail=" + notificationEmail + "]";
	}

}
