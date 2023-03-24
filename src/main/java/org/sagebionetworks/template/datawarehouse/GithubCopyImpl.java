package org.sagebionetworks.template.datawarehouse;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.ConfigurationPropertyNotFound;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

public class GithubCopyImpl implements GithubCopy {
    private static final String S3_GLUE_BUCKET = "aws-glue.sagebase.org";
    private static final String GITHUB_REPOSITORY_NAME = "Synapse-ETL-Jobs";
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
    public String copyFileFromGithub() throws ConfigurationPropertyNotFound {
        String stack = configuration.getProperty(PROPERTY_KEY_STACK);
        String bucket = String.join(".", stack, S3_GLUE_BUCKET);
        String githubUrl = new StringJoiner("/").add(githubConfig.getBasePath())
                .add(githubConfig.getVersion()).toString();
        logger.info("Github download url: " + githubUrl);
        Set<String> filesToBeDownloaded = githubConfig.getFilePaths().stream()
                .map(b -> addRepositoryName(b, githubConfig.getVersion())).collect(Collectors.toSet());
        Map<String, File> downloadFiles = downloader.downloadFileFromZip(githubUrl, githubConfig.getVersion(), filesToBeDownloaded);

        for (Map.Entry<String, File> entry : downloadFiles.entrySet()) {
            String fileName = entry.getKey();
            String s3Key = "scripts/" + fileName;
            try {
                logger.info("Uploading file to S3: " + s3Key);
                s3Client.putObject(bucket, s3Key, entry.getValue());
                logger.info("Uploaded file to S3: " + s3Key);
            } finally {
                entry.getValue().delete();
            }
        }
        return githubConfig.getVersion();
    }

    private String addRepositoryName(String input, String version) {
        return GITHUB_REPOSITORY_NAME + "-" + version.substring(1) + input;
    }
}
