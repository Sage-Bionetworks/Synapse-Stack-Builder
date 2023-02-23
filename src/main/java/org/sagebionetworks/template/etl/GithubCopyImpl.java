package org.sagebionetworks.template.etl;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import java.io.File;
import java.util.StringJoiner;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

public class GithubCopyImpl implements GithubCopy {
    private static final String EXTENSION = ".py";
    private static final String S3_GLUE_BUCKET = "aws-glue.sagebase.org";
    private AmazonS3 s3Client;
    private GithubConfig githubConfig;
    private ArtifactDownload downloader;
    private Configuration configuration;
    private Logger logger;

    @Inject
    public GithubCopyImpl(AmazonS3 s3Client, GithubConfig githubConfig,
                          ArtifactDownload downloader, Configuration configuration, LoggerFactory loggerFactory) {
        this.s3Client = s3Client;
        this.githubConfig = githubConfig;
        this.downloader = downloader;
        this.configuration = configuration;
        this.logger = loggerFactory.getLogger(GithubCopyImpl.class);

    }

    @Override
    public void copyFileFromGithub() throws ConfigurationPropertyNotFound {
        for (GithubPath githubPath : githubConfig.getGithubPathList()) {
            String stack = configuration.getProperty(PROPERTY_KEY_STACK);
            String bucket = String.join(".", stack, S3_GLUE_BUCKET);
            String filePrefix = githubPath.getFilename().substring(0, githubPath.getFilename().indexOf("."));
            String s3Key = "scripts/" + filePrefix + "_" + githubPath.getVersion() + EXTENSION;
            String githubUrl = new StringJoiner("/").add(githubPath.getBasePath())
                    .add(githubPath.getVersion()).toString();
            logger.info("Github download url: " + githubUrl);
            File download = downloader.downloadFileFromZip(githubUrl, githubPath.getFilePath());
            try {
                logger.info("Uploading file to S3: " + s3Key);
                s3Client.putObject(bucket, s3Key, download);
                logger.info("Uploaded file to S3: " + s3Key);
            } finally {
                try {
                    download.delete();
                } catch (SecurityException securityException) {
                    logger.warn("Unable to delete the file ", securityException.getMessage());
                }
            }
        }

    }
}
