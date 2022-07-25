package org.sagebionetworks.template.s3;

import java.util.Objects;

public class S3VirusScannerConfig {

	private String lambdaArtifactBucket;

	private String notificationEmail;

	public S3VirusScannerConfig() {
	}

	public String getLambdaArtifactBucket() {
		return lambdaArtifactBucket;
	}

	public void setLambdaArtifactBucket(String lambdaArtifactBucket) {
		this.lambdaArtifactBucket = lambdaArtifactBucket;
	}

	public String getNotificationEmail() {
		return notificationEmail;
	}

	public void setNotificationEmail(String notificationEmail) {
		this.notificationEmail = notificationEmail;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lambdaArtifactBucket, notificationEmail);
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
				&& Objects.equals(notificationEmail, other.notificationEmail);
	}

	@Override
	public String toString() {
		return "S3VirusScannerConfig [lambdaArtifactBucket=" + lambdaArtifactBucket
				+ ", notificationEmail=" + notificationEmail + "]";
	}

}
