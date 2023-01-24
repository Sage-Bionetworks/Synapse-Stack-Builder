package org.sagebionetworks.template.repo.beanstalk;

import java.io.File;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.repo.beanstalk.ssl.ElasticBeanstalkExtentionBuilder;
import org.sagebionetworks.template.utils.ArtifactDownload;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Inject;

public class ArtifactCopyImpl implements ArtifactCopy {

	private AmazonS3 s3Client;
	private Configuration configuration;
	private ArtifactDownload downloader;
	private ElasticBeanstalkExtentionBuilder ebBuilder;
	
	private Logger logger;

	@Inject
	public ArtifactCopyImpl(AmazonS3 s3Client, Configuration propertyProvider,
			ArtifactDownload downloader, LoggerFactory loggerFactory, ElasticBeanstalkExtentionBuilder ebBuilder) {
		super();
		this.s3Client = s3Client;
		this.configuration = propertyProvider;
		this.downloader = downloader;
		this.logger = loggerFactory.getLogger(ArtifactCopyImpl.class);
		this.ebBuilder = ebBuilder;
	}

	@Override
	public SourceBundle copyArtifactIfNeeded(EnvironmentType environment, String version) {
		String bucket = configuration.getConfigurationBucket();
		String s3Key = environment.createS3Key(version);
		SourceBundle bundle = new SourceBundle(bucket, s3Key);
		// does the file already exist in S3
 		if (!s3Client.doesObjectExist(bucket, s3Key)) {
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
				s3Client.putObject(bucket, s3Key, warWithExtentions);
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

}
