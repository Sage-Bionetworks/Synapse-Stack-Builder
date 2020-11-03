package org.sagebionetworks.template.repo.beanstalk.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.velocity.app.VelocityEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.FileProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsVelocityContextProvider;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogDescriptor;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogType;
import org.sagebionetworks.war.WarAppender;

@RunWith(MockitoJUnitRunner.class)
public class ElasticBeanstalkExtentionBuilderImplTest {

	@Mock
	CertificateBuilder certifiateBuilder;
	// use the actual engine
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
	File mockFile;
	@Mock
	File mockSslConf;
	@Mock
	CloudwatchLogsVelocityContextProvider mockCwlVelocityContextProvider;

	ElasticBeanstalkExtentionBuilderImpl builder;

	StringWriter configWriter;
	StringWriter sslConfWriter;
	StringWriter modSecurityConfWriter;

	String bucketName;
	String x509CertificatePem;
	String privateKeyPem;

	@Before
	public void before() {
		// Use the actual velocity entity
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		builder = new ElasticBeanstalkExtentionBuilderImpl(certifiateBuilder, velocityEngine, configuration,
				warAppender, fileProvider, mockCwlVelocityContextProvider);
		// call accept on the consumer.
		doAnswer(new Answer<File>() {
			@Override
			public File answer(InvocationOnMock invocation) throws Throwable {
				Consumer<File> consumer = invocation.getArgument(1, Consumer.class);
				// forward the request to the consumer
				consumer.accept(mockTempDirectory);
				return mockWarCopy;
			}
		}).when(warAppender).appendFilesCopyOfWar(any(File.class), any(Consumer.class));
		when(fileProvider.createNewFile(any(File.class), any(String.class))).thenReturn(mockFile);
		configWriter = new StringWriter();
		sslConfWriter = new StringWriter();
		modSecurityConfWriter = new StringWriter();
		when(fileProvider.createFileWriter(any(File.class))).thenReturn(configWriter, sslConfWriter, modSecurityConfWriter);
		bucketName = "someBucket";
		when(configuration.getConfigurationBucket()).thenReturn(bucketName);
		x509CertificatePem = "x509pem";
		privateKeyPem = "privatePem";
		when(certifiateBuilder.buildNewX509CertificatePair()).thenReturn(new CertificatePair(x509CertificatePem, privateKeyPem));
		when(mockCwlVelocityContextProvider.getLogDescriptors(any(EnvironmentType.class))).thenReturn(generateLogDescriptors());
	}

	@Test
	public void testCopyWarWithExtensions() {
		// Call under test
		File warCopy = builder.copyWarWithExtensions(mockWar, EnvironmentType.REPOSITORY_SERVICES);

		assertNotNull(warCopy);
		assertEquals(mockWarCopy, warCopy);
		// httpconfig
		String httpConfigJson = configWriter.toString();
		//System.out.println(httpConfigJson);
		assertTrue(httpConfigJson.contains(x509CertificatePem));
		assertTrue(httpConfigJson.contains(privateKeyPem));
		// SSL conf
		String sslConf = sslConfWriter.toString();
		assertTrue(sslConf.contains("/etc/pki/tls/certs/server.crt"));
		assertTrue(sslConf.contains("/etc/pki/tls/certs/server.key"));
		//mod_security conf
		String modSecurityConf = modSecurityConfWriter.toString();
		assertTrue(modSecurityConf.contains("SecServerSignature"));

		verify(mockCwlVelocityContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES);
	}

	private List<LogDescriptor> generateLogDescriptors() {
		List<LogDescriptor> descriptors = new LinkedList<>();
		for (LogType t: LogType.values()) {
			LogDescriptor d = new LogDescriptor();
			d.setLogType(t);
			d.setLogPath("/var/log/mypath.log");
			d.setDateFormat("YYYY-MM-DD");
			descriptors.add(d);
		}
		return descriptors;
	}



}
