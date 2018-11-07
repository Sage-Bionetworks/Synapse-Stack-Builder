package org.sagebionetworks.template.repo.beanstalk.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.repo.beanstalk.ssl.ElasticBeanstalkExtentionBuilderImpl.DOT_EBEXTENSIONS;
import static org.sagebionetworks.template.repo.beanstalk.ssl.ElasticBeanstalkExtentionBuilderImpl.HTTPS_INSTANCE_CONFIG;

import java.io.File;
import java.io.StringWriter;
import java.util.function.Consumer;

import org.apache.velocity.app.VelocityEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.FileProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.war.WarAppender;

@RunWith(MockitoJUnitRunner.class)
public class ElasticBeanstalkExtentionBuilderImplTest {

	@Mock
	CertificateProvider certificateProvider;
	VelocityEngine velocityEngine;
	@Mock
	Configuration configuration;
	@Mock
	WarAppender warAppender;
	@Mock
	FileProvider fileProvider;
	@Mock
	File mockWar;
	@Mock
	File mockTempDirectory;
	@Mock
	File mockWarCopy;
	@Mock
	File mockExtentionsFolder;
	@Mock
	File mockHttpConfig;

	ElasticBeanstalkExtentionBuilderImpl builder;

	StringWriter writer;

	String bucketName;
	String x509CertificateUrl;
	String privateKeyUrl;
	CertificateUrls certificateUrls;

	@Before
	public void before() {
		// Use the actual velocity entity
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		builder = new ElasticBeanstalkExtentionBuilderImpl(certificateProvider, velocityEngine, configuration,
				warAppender, fileProvider);
		// call accept on the consumer.
		doAnswer(new Answer<File>() {
			@Override
			public File answer(InvocationOnMock invocation) throws Throwable {
				Consumer<File> consumer = invocation.getArgumentAt(1, Consumer.class);
				// forward the request to the consumer
				consumer.accept(mockTempDirectory);
				return mockWarCopy;
			}
		}).when(warAppender).appendFilesCopyOfWar(any(File.class), any(Consumer.class));
		when(fileProvider.createNewFile(mockTempDirectory, DOT_EBEXTENSIONS)).thenReturn(mockExtentionsFolder);
		when(fileProvider.createNewFile(mockExtentionsFolder, HTTPS_INSTANCE_CONFIG)).thenReturn(mockHttpConfig);
		writer = new StringWriter();
		when(fileProvider.createFileWriter(mockHttpConfig)).thenReturn(writer);

		bucketName = "someBucket";
		when(configuration.getConfigurationBucket()).thenReturn(bucketName);
		x509CertificateUrl = "x509Url";
		privateKeyUrl = "privateUrl";
		when(certificateProvider.provideCertificateUrls()).thenReturn(new CertificateUrls(x509CertificateUrl, privateKeyUrl));
	}

	@Test
	public void testCopyWarWithExtensions() {
		// Call under test
		File warCopy = builder.copyWarWithExtensions(mockWar);
		assertNotNull(warCopy);
		assertEquals(mockWarCopy, warCopy);
		verify(mockExtentionsFolder).mkdirs();
		
		String httpConfigJson = writer.toString();
		//System.out.println(httpConfigJson);
		assertTrue(httpConfigJson.contains(bucketName));
		assertTrue(httpConfigJson.contains(x509CertificateUrl));
		assertTrue(httpConfigJson.contains(privateKeyUrl));
	}

}
