package org.sagebionetworks.template.etl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

@ExtendWith(MockitoExtension.class)
public class GithubCopyImplTest {
    @Mock
    File mockFile;
    String stack;
    String fileName;
    String bucket;
    String s3Key;
    String version;
    String githubUrl;
    String githubUrlWithVersion;
    @Mock
    private AmazonS3 mockS3Client;
    @Mock
    private GithubConfig mockGithubConfig;
    @Mock
    private ArtifactDownload mockDownloader;
    @Mock
    private Configuration mockConfig;
    @Mock
    private Logger mockLogger;
    @Mock
    private LoggerFactory mockLoggerFactory;
    private GithubCopyImpl githubCopyImpl;

    @BeforeEach
    public void before() {
        stack = "dev";
        version = "v0.1.0";
        fileName = "test.py";

        bucket = "dev.aws-glue.sagebase.org";

        s3Key = "scripts/test_v0.1.0.py";
        githubUrl = "https://codeload.github.com/Sage-Bionetworks/Synapse-ETL-Jobs/zip/refs/tags";
        githubUrlWithVersion = "https://codeload.github.com/Sage-Bionetworks/Synapse-ETL-Jobs/zip/refs/tags/v0.1.0";
        when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
        githubCopyImpl = new GithubCopyImpl(mockS3Client, mockGithubConfig, mockDownloader, mockConfig, mockLoggerFactory);
    }

    @Test
    public void testCopyFileFromGithub() {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        when(mockGithubConfig.getFilename()).thenReturn(fileName);
        when(mockGithubConfig.getVersion()).thenReturn(version);
        when(mockGithubConfig.getGithubPath()).thenReturn(githubUrl);
        when(mockDownloader.downloadFileFromZip(any(), any())).thenReturn(mockFile);

        // call under test
        githubCopyImpl.copyFileFromGithub();
        verify(mockDownloader).downloadFileFromZip(githubUrlWithVersion, fileName);
        verify(mockS3Client).putObject(bucket, s3Key, mockFile);
        verify(mockLogger, times(3)).info(any(String.class));
        verify(mockFile).delete();
    }

    @Test
    public void testCopyFileFromGithubFail() {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        when(mockGithubConfig.getFilename()).thenReturn(fileName);
        when(mockGithubConfig.getVersion()).thenReturn(version);
        when(mockGithubConfig.getGithubPath()).thenReturn(githubUrl);
        when(mockDownloader.downloadFileFromZip(any(), any())).thenReturn(mockFile);
        AmazonServiceException exception = new AmazonServiceException("something");
        when(mockS3Client.putObject(any(), any(), any(File.class))).thenThrow(exception);

        // call under test
        assertThrows(AmazonServiceException.class, () -> {
            githubCopyImpl.copyFileFromGithub();
        });
        // file should be deleted even for a failure.
        verify(mockFile).delete();
    }
}
