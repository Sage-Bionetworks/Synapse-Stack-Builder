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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.beanstalk.ssl.ElasticBeanstalkExtentionBuilder;
import org.sagebionetworks.template.utils.ArtifactDownload;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
public class ArtifactCopyImplTest {

	@Mock
	S3Client mockS3Client;
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
	@Captor
	ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor;
	@Captor
	ArgumentCaptor<HeadObjectRequest> headObjectRequestCaptor;

	ArtifactCopyImpl copier;
	
	String stack;
	String version;
	EnvironmentType environment;
	String bucket;
	String s3Key;
	String artifactoryUrl;
	int beanstalkNumber;
	File realCopyFile;

	@BeforeEach
	public void before() throws Exception {

		beanstalkNumber = 9;
		environment = EnvironmentType.REPOSITORY_WORKERS;
		version = "212.4";

		bucket = "dev-configuration.sage.bionetworks";

		s3Key = environment.createS3Key(version, beanstalkNumber);
		artifactoryUrl = environment.createArtifactoryUrl(version);
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		copier = new ArtifactCopyImpl(mockS3Client, mockPropertyProvider, mockDownloader, mockLoggerFactory, mockEbBuilder);
		// RequestBody.fromFile() eagerly reads the file's size and mimetype, so the copy must
		// resolve to a real, existing file path.
		realCopyFile = File.createTempFile("artifact-copy", ".war");
		realCopyFile.deleteOnExit();
	}

	@Test
	public void testCopyArtifactIfNeededDoesNotExist() {
		when(mockDownloader.downloadFile(any(String.class))).thenReturn(mockFile);
		when(mockEbBuilder.copyWarWithExtensions(eq(mockFile), any(EnvironmentType.class))).thenReturn(mockCopy);
		when(mockCopy.toPath()).thenReturn(realCopyFile.toPath());
		when(mockPropertyProvider.getConfigurationBucket()).thenReturn(bucket);
		// setup object does not exist
		when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

		// call under test
		SourceBundle result = copier.copyArtifactIfNeeded(environment, version, beanstalkNumber);
		assertNotNull(result);
		assertEquals(bucket, result.getBucket());
		assertEquals(s3Key, result.getKey());

		verify(mockS3Client).headObject(headObjectRequestCaptor.capture());
		assertEquals(bucket, headObjectRequestCaptor.getValue().bucket());
		assertEquals(s3Key, headObjectRequestCaptor.getValue().key());
		verify(mockDownloader).downloadFile(artifactoryUrl);
		verify(mockEbBuilder).copyWarWithExtensions(eq(mockFile), any(EnvironmentType.class));
		verify(mockS3Client).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));
		assertEquals(bucket, putObjectRequestCaptor.getValue().bucket());
		assertEquals(s3Key, putObjectRequestCaptor.getValue().key());
		verify(mockLogger, times(4)).info(any(String.class));
		// the temp file should get deleted.
		verify(mockFile).delete();
		verify(mockCopy).delete();
	}

	@Test
	public void testCopyArtifactIfNeededUplodFails() {
		when(mockDownloader.downloadFile(any(String.class))).thenReturn(mockFile);
		when(mockEbBuilder.copyWarWithExtensions(eq(mockFile), any(EnvironmentType.class))).thenReturn(mockCopy);
		when(mockCopy.toPath()).thenReturn(realCopyFile.toPath());
		when(mockPropertyProvider.getConfigurationBucket()).thenReturn(bucket);

		S3Exception exception = (S3Exception) S3Exception.builder().message("something").build();
		when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(exception);

		// setup object does not exist
		when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

		// call under test
		assertThrows(S3Exception.class, ()->{
			copier.copyArtifactIfNeeded(environment, version, beanstalkNumber);
		});
		// file should be deleted even for a failure.
		verify(mockFile).delete();
	}

	@Test
	public void testCopyArtifactIfNeededExist() {
		when(mockPropertyProvider.getConfigurationBucket()).thenReturn(bucket);
		// setup object exists
		when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

		// call under test
		SourceBundle result = copier.copyArtifactIfNeeded(environment, version, beanstalkNumber);
		assertNotNull(result);
		assertEquals(bucket, result.getBucket());
		assertEquals(s3Key, result.getKey());

		verify(mockS3Client).headObject(headObjectRequestCaptor.capture());
		assertEquals(bucket, headObjectRequestCaptor.getValue().bucket());
		assertEquals(s3Key, headObjectRequestCaptor.getValue().key());
		verify(mockDownloader, never()).downloadFile(artifactoryUrl);
		verify(mockEbBuilder, never()).copyWarWithExtensions(eq(mockFile), any(EnvironmentType.class));
		verify(mockS3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
		verify(mockFile, never()).delete();
		verify(mockLogger, times(1)).info(any(String.class));
	}
}
