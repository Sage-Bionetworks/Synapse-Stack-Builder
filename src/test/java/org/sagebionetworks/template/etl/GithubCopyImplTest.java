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
import java.util.Collections;

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
    String bucket;
    String s3Key;
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
    private GithubPath githubPath;

    @BeforeEach
    public void before() {
        stack = "dev";
        bucket = "dev.aws-glue.sagebase.org";
        s3Key = "scripts/test_v1.py";
        githubUrlWithVersion = "https://codeload.github.com/Sage-Bionetworks/Synapse-ETL-Jobs/zip/refs/tags/v1";
        githubPath = new GithubPath();
        githubPath.setBasePath("https://codeload.github.com/Sage-Bionetworks/Synapse-ETL-Jobs/zip/refs/tags");
        githubPath.setFilePath("filePath");
        githubPath.setFilename("test.py");
        githubPath.setVersion("v1");
        when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
        githubCopyImpl = new GithubCopyImpl(mockS3Client, mockGithubConfig, mockDownloader, mockConfig, mockLoggerFactory);
    }

    @Test
    public void testCopyFileFromGithub() {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        when(mockGithubConfig.getGithubPathList()).thenReturn(Collections.singletonList(githubPath));
        when(mockDownloader.downloadFileFromZip(any(), any())).thenReturn(mockFile);

        // call under test
        githubCopyImpl.copyFileFromGithub();
        verify(mockDownloader).downloadFileFromZip(githubUrlWithVersion, githubPath.getFilePath());
        verify(mockS3Client).putObject(bucket, s3Key, mockFile);
        verify(mockLogger, times(3)).info(any(String.class));
        verify(mockFile).delete();
    }

    @Test
    public void testCopyFileFromGithubFail() {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        when(mockGithubConfig.getGithubPathList()).thenReturn(Collections.singletonList(githubPath));
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
