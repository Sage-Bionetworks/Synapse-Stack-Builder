package org.sagebionetworks.template.repo.beanstalk;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.beanstalk.ssl.ElasticBeanstalkExtentionBuilder;
import org.sagebionetworks.template.utils.ArtifactDownload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;

@ExtendWith(MockitoExtension.class)
public class ArtifactCopyImplTest {
	
	@Mock
	AmazonS3 mockS3Client;
	@Mock
	Configuration mockPropertyProvider;
	@Mock
	ArtifactDownload mockDownloader;
	@Mock
	File mockFile;
	@Mock 
	File mockCopy;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;
	@Mock
	ElasticBeanstalkExtentionBuilder mockEbBuilder;
	
	ArtifactCopyImpl copier;
	
	String stack;
	String version;
	EnvironmentType environment;
	String bucket;
	String s3Key;
	String artifactoryUrl;
	int beanstalkNumber;

	@BeforeEach
	public void before() {
		
		beanstalkNumber = 9;
		environment = EnvironmentType.REPOSITORY_WORKERS;
		version = "212.4";
		
		bucket = "dev-configuration.sage.bionetworks";

		s3Key = environment.createS3Key(version, beanstalkNumber);
		artifactoryUrl = environment.createArtifactoryUrl(version);
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		copier = new ArtifactCopyImpl(mockS3Client, mockPropertyProvider, mockDownloader, mockLoggerFactory, mockEbBuilder);
	}
	
	@Test
	public void testCopyArtifactIfNeededDoesNotExist() {
		when(mockDownloader.downloadFile(any(String.class))).thenReturn(mockFile);
		when(mockEbBuilder.copyWarWithExtensions(eq(mockFile), any(EnvironmentType.class))).thenReturn(mockCopy);
		when(mockPropertyProvider.getConfigurationBucket()).thenReturn(bucket);
		// setup object does not exist
		when(mockS3Client.doesObjectExist(any(), any())).thenReturn(false);
		
		// call under test
		SourceBundle result = copier.copyArtifactIfNeeded(environment, version, beanstalkNumber);
		assertNotNull(result);
		assertEquals(bucket, result.getBucket());
		assertEquals(s3Key, result.getKey());
		
		verify(mockS3Client).doesObjectExist(bucket, s3Key);
		verify(mockDownloader).downloadFile(artifactoryUrl);
		verify(mockEbBuilder).copyWarWithExtensions(eq(mockFile), any(EnvironmentType.class));
		verify(mockS3Client).putObject(bucket, s3Key, mockCopy);
		verify(mockLogger, times(3)).info(any(String.class));
		// the temp file should get deleted.
		verify(mockFile).delete();
		verify(mockCopy).delete();
	}
	
	@Test
	public void testCopyArtifactIfNeededUplodFails() {
		when(mockDownloader.downloadFile(any(String.class))).thenReturn(mockFile);
		when(mockEbBuilder.copyWarWithExtensions(eq(mockFile), any(EnvironmentType.class))).thenReturn(mockCopy);
		when(mockPropertyProvider.getConfigurationBucket()).thenReturn(bucket);
		
		AmazonServiceException exception = new AmazonServiceException("something");
		when(mockS3Client.putObject(any(), any(), any(File.class))).thenThrow(exception);
		
		// setup object does not exist
		when(mockS3Client.doesObjectExist(any(), any())).thenReturn(false);
		
		// call under test
		assertThrows(AmazonServiceException.class, ()->{
			copier.copyArtifactIfNeeded(environment, version, beanstalkNumber);
		});
		// file should be deleted even for a failure.
		verify(mockFile).delete();
	}
	
	@Test
	public void testCopyArtifactIfNeededExist() {
		when(mockPropertyProvider.getConfigurationBucket()).thenReturn(bucket);
		// setup object exists
		when(mockS3Client.doesObjectExist(any(), any())).thenReturn(true);
		
		// call under test
		SourceBundle result = copier.copyArtifactIfNeeded(environment, version, beanstalkNumber);
		assertNotNull(result);
		assertEquals(bucket, result.getBucket());
		assertEquals(s3Key, result.getKey());
		
		verify(mockS3Client).doesObjectExist(bucket, s3Key);
		verify(mockDownloader, never()).downloadFile(artifactoryUrl);
		verify(mockEbBuilder, never()).copyWarWithExtensions(eq(mockFile), any(EnvironmentType.class));
		verify(mockS3Client, never()).putObject(bucket, s3Key, mockFile);
		verify(mockFile, never()).delete();
		verify(mockLogger, never()).info(any(String.class));
	}
}
