package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.repo.beanstalk.ArtifactCopyImpl;
import org.sagebionetworks.template.repo.beanstalk.ArtifactDownload;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.SourceBundle;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactCopyImplTest {
	
	@Mock
	AmazonS3 mockS3Client;
	@Mock
	Configuration mockPropertyProvider;
	@Mock
	ArtifactDownload mockDownloader;
	@Mock
	File mockFile;
	
	ArtifactCopyImpl copier;
	
	String stack;
	String version;
	EnvironmentType environment;
	String bucket;
	String s3Key;
	String artifactoryUrl;

	@Before
	public void before() {
		
		when(mockDownloader.downloadFile(any(String.class))).thenReturn(mockFile);
		
		
		
		environment = EnvironmentType.REPOSITORY_WORKERS;
		version = "212.4";
		
		bucket = "dev-configuration.sage.bionetworks";
		when(mockPropertyProvider.getConfigurationBucket()).thenReturn(bucket);
		s3Key = environment.createS3Key(version);
		artifactoryUrl = environment.createArtifactoryUrl(version);
		
		copier = new ArtifactCopyImpl(mockS3Client, mockPropertyProvider, mockDownloader);
	}
	
	@Test
	public void testCopyArtifactIfNeededDoesNotExist() {
		// setup object does not exist
		when(mockS3Client.doesObjectExist(bucket, s3Key)).thenReturn(false);
		
		// call under test
		SourceBundle result = copier.copyArtifactIfNeeded(environment, version);
		assertNotNull(result);
		assertEquals(bucket, result.getBucket());
		assertEquals(s3Key, result.getKey());
		
		verify(mockS3Client).doesObjectExist(bucket, s3Key);
		verify(mockDownloader).downloadFile(artifactoryUrl);
		verify(mockS3Client).putObject(bucket, s3Key, mockFile);
		// the temp file should get deleted.
		verify(mockFile).delete();
	}
	
	@Test
	public void testCopyArtifactIfNeededUplodFails() {
		AmazonServiceException exception = new AmazonServiceException("something");
		when(mockS3Client.putObject(bucket, s3Key, mockFile)).thenThrow(exception);
		
		// setup object does not exist
		when(mockS3Client.doesObjectExist(bucket, s3Key)).thenReturn(false);
		
		// call under test
		try {
			copier.copyArtifactIfNeeded(environment, version);
			fail();
		} catch (AmazonServiceException e) {
			// expected
		}
		// file should be deleted even for a failure.
		verify(mockFile).delete();
	}
	
	@Test
	public void testCopyArtifactIfNeededExist() {
		// setup object exists
		when(mockS3Client.doesObjectExist(bucket, s3Key)).thenReturn(true);
		
		// call under test
		SourceBundle result = copier.copyArtifactIfNeeded(environment, version);
		assertNotNull(result);
		assertEquals(bucket, result.getBucket());
		assertEquals(s3Key, result.getKey());
		
		verify(mockS3Client).doesObjectExist(bucket, s3Key);
		verify(mockDownloader, never()).downloadFile(artifactoryUrl);
		verify(mockS3Client, never()).putObject(bucket, s3Key, mockFile);
		verify(mockFile, never()).delete();
	}
}
