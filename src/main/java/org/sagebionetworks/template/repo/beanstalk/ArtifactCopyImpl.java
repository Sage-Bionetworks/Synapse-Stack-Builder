package org.sagebionetworks.template.repo.beanstalk;

import java.io.File;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.repo.beanstalk.ssl.ElasticBeanstalkExtentionBuilder;
import org.sagebionetworks.template.utils.ArtifactDownload;

import com.google.inject.Inject;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ArtifactCopyImpl implements ArtifactCopy {

	private S3Client s3Client;
	private Configuration configuration;
	private ArtifactDownload downloader;
	private ElasticBeanstalkExtentionBuilder ebBuilder;

	private Logger logger;

	@Inject
	public ArtifactCopyImpl(S3Client s3Client, Configuration propertyProvider,
			ArtifactDownload downloader, LoggerFactory loggerFactory, ElasticBeanstalkExtentionBuilder ebBuilder) {
		super();
		this.s3Client = s3Client;
		this.configuration = propertyProvider;
		this.downloader = downloader;
		this.logger = loggerFactory.getLogger(ArtifactCopyImpl.class);
		this.ebBuilder = ebBuilder;
	}

	@Override
	public SourceBundle copyArtifactIfNeeded(EnvironmentType environment, String version, int number) {
		String bucket = configuration.getConfigurationBucket();
		String s3Key = environment.createS3Key(version, number);
		logger.info("Looking for: "+s3Key);
		SourceBundle bundle = new SourceBundle(bucket, s3Key);
		// does the file already exist in S3
 		if (!doesObjectExist(bucket, s3Key)) {
			/*
			 * The file does not exist in S3 so it will needed to be downloaded from
			 * Artifactory and then uploaded to S3
			 */
			String artifactoryUrl = environment.createArtifactoryUrl(version);
			logger.info("Downloading artifact: "+artifactoryUrl);
			File download = downloader.downloadFile(artifactoryUrl);
			File warWithExtentions = null;
			try {
				logger.info("Adding .ebextentions to war: "+s3Key);
				// add the .eb extensions to the given war file.
				warWithExtentions = ebBuilder.copyWarWithExtensions(download, environment);
				logger.info("Uploading artifact to S3: "+s3Key);
				s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(s3Key).build(),
						RequestBody.fromFile(warWithExtentions));
			} finally {
				// cleanup the temp file
				download.delete();
				if(warWithExtentions != null) {
					warWithExtentions.delete();
				}
			}
		}
		return bundle;
	}

	/**
	 * Does the given object already exist in S3? The v2 SDK has no direct equivalent of the v1
	 * doesObjectExist(), so a headObject is issued and a missing object is signaled by a
	 * NoSuchKeyException.
	 */
	private boolean doesObjectExist(String bucket, String key) {
		try {
			s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
			return true;
		} catch (NoSuchKeyException e) {
			return false;
		}
	}

}
