package org.sagebionetworks.template.repo.beanstalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.DB_ENDPOINT_SUFFIX;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_NUMBER;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.REPO_NUMBER;
import static org.sagebionetworks.template.Constants.STACK;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.FileProvider;
import org.sagebionetworks.template.LoggerFactory;

import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentConfigurationImplTest {

	@Mock
	private AmazonS3 mockS3Client;
	@Mock
	private Configuration mockConfig;
	@Mock
	private VelocityEngine mockVelocityEngine;
	@Mock
	private LoggerFactory mockLoggerFactory;
	@Mock
	private Logger mockLogger;
	@Mock
	private FileProvider mockFileProvider;
	@Mock
	private File mockFile;
	@Mock
	private Template mockTempalte;
	@Captor
	ArgumentCaptor<GetObjectRequest> requestCaptor;
	@Captor
	ArgumentCaptor<PutObjectRequest> putRequestCaptor;

	String bucket;
	String stack;
	String instance;
	String beanstalkNumber;
	String tempDirectory;
	String tempFileName;

	Stack sharedResouces;
	
	String databaseEndpointSuffix;

	EnvironmentConfigurationImpl environConfig;

	@Before
	public void before() throws IOException {
		bucket = "bucket";
		when(mockConfig.getConfigurationBucket()).thenReturn(bucket);

		stack = "dev";
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		instance = "101";
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		beanstalkNumber = "0";
		when(mockConfig.getProperty(PROPERTY_KEY_BEANSTALK_NUMBER + EnvironmentType.REPOSITORY_SERVICES.getShortName()))
				.thenReturn(beanstalkNumber);
		tempDirectory = "temp/dir";
		when(mockFile.getParent()).thenReturn(tempDirectory);
		tempFileName = "file.tmp";
		when(mockFile.getName()).thenReturn(tempFileName);

		when(mockVelocityEngine.getTemplate(any(String.class))).thenReturn(mockTempalte);
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockFile);
		environConfig = new EnvironmentConfigurationImpl(mockS3Client, mockConfig, mockVelocityEngine,
				mockLoggerFactory, mockFileProvider);

		sharedResouces = new Stack();
		Output dbOut = new Output();
		dbOut.withOutputKey(stack+instance+EnvironmentConfigurationImpl.OUTPUT_NAME_SUFFIX_REPOSITORY_DB_ENDPOINT);
		databaseEndpointSuffix = "something.amazon.com";
		dbOut.withOutputValue(stack+"-"+instance+"-db."+databaseEndpointSuffix);
		sharedResouces.withOutputs(dbOut);
	}

	@Test
	public void testCreateEnvironmentConfiguration() {
		// call under test
		String url = environConfig.createEnvironmentConfiguration(sharedResouces);
		assertEquals("https://s3.amazonaws.com/bucket/Stack/dev101-stack.properties", url);
		verify(mockS3Client).getObject(any(GetObjectRequest.class), any(File.class));
		verify(mockTempalte).merge(any(Context.class), any(Writer.class));
		verify(mockS3Client).putObject(any(PutObjectRequest.class));
		verify(mockFile).delete();
		verify(mockLogger, times(2)).info(any(String.class));
	}

	@Test
	public void testDownloadTemplate() {
		// Call under test
		File result = environConfig.downloadTemplate();
		assertEquals(mockFile, result);
		verify(mockS3Client).getObject(requestCaptor.capture(), any(File.class));
		GetObjectRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals(bucket, request.getBucketName());
		assertEquals("templates/dev-template-stack.properties", request.getKey());
		verify(mockLogger).info(any(String.class));
	}
	
	@Test
	public void testExtractDatabaseSuffix() {
		// call under test
		String suffix = environConfig.extractDatabaseSuffix(stack, instance, sharedResouces);
		assertEquals(databaseEndpointSuffix, suffix);
	}

	@Test
	public void testCreateContext() {
		// Call under test
		Context context = environConfig.createContext(sharedResouces);
		assertNotNull(context);
		assertEquals(stack, context.get(STACK));
		assertEquals(instance, context.get(INSTANCE));
		assertEquals(databaseEndpointSuffix, context.get(DB_ENDPOINT_SUFFIX));
		assertEquals(beanstalkNumber, context.get(REPO_NUMBER));
	}

	@Test
	public void testUploadResultFileToS3() {
		String contents = "some contents";
		// call under test
		String url = environConfig.uploadResultFileToS3(contents);
		assertEquals("https://s3.amazonaws.com/bucket/Stack/dev101-stack.properties", url);
		verify(mockS3Client).putObject(putRequestCaptor.capture());
		PutObjectRequest request = putRequestCaptor.getValue();
		assertNotNull(request);
		assertEquals(bucket, request.getBucketName());
		assertEquals("Stack/dev101-stack.properties", request.getKey());
		ObjectMetadata meta = request.getMetadata();
		assertNotNull(meta);
		assertEquals(13L, meta.getContentLength());
		verify(mockLogger).info(any(String.class));
	}
}
