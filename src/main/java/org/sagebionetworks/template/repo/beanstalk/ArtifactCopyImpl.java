package org.sagebionetworks.template.repo.beanstalk;

import java.io.File;

import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.repo.RepositoryPropertyProvider;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Inject;

public class ArtifactCopyImpl implements ArtifactCopy {

	private AmazonS3 s3Client;
	private RepositoryPropertyProvider propertyProvider;
	private ArtifactDownload downloader;

	@Inject
	public ArtifactCopyImpl(AmazonS3 s3Client, RepositoryPropertyProvider propertyProvider,
			ArtifactDownload downloader) {
		super();
		this.s3Client = s3Client;
		this.propertyProvider = propertyProvider;
		this.downloader = downloader;
	}

	@Override
	public SourceBundle copyArtifactIfNeeded(EnvironmentType environment, String version) {
		String stack = propertyProvider.get(Constants.PROPERTY_KEY_STACK);
		String bucket = stack + "-sage.bionetworks";
		String s3Key = environment.createS3Key(version);
		SourceBundle bundle = new SourceBundle(bucket, s3Key);
		// does the file already exist in S3
		if (!s3Client.doesObjectExist(bucket, s3Key)) {
			/*
			 * The file does not exist in S3 so it will needed to be downloaded from
			 * Artifactory and then uploaded to S3
			 */
			String artifactoryUrl = environment.createArtifactoryUrl(version);
			File download = downloader.downloadFile(artifactoryUrl);
			try {
				s3Client.putObject(bucket, s3Key, download);
			} finally {
				// cleanup the temp file
				download.delete();
			}
		}
		return bundle;
	}

}
