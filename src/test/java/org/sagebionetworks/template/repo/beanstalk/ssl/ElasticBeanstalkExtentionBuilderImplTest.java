package org.sagebionetworks.template.repo.beanstalk.ssl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.repo.beanstalk.ssl.ElasticBeanstalkExtentionBuilderImpl.TEMPLATES_REPO_EBEXTENSIONS_UPDATE_TOMCAT_SERVER_XML_SH;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.FileProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.LoadBalancerAlarm;
import org.sagebionetworks.template.repo.beanstalk.LoadBalancerAlarmsConfig;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsVelocityContextProvider;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogDescriptor;
import org.sagebionetworks.template.repo.cloudwatchlogs.LogType;
import org.sagebionetworks.war.WarAppender;

import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.Statistic;

@ExtendWith(MockitoExtension.class)
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
	@Mock
	LoadBalancerAlarmsConfig mockLoadBalanacerAlarmsConfig;

	ElasticBeanstalkExtentionBuilderImpl builder;

	StringWriter configWriter;
	StringWriter sslConfWriter;
	StringWriter modSecurityConfWriter;
	StringWriter logsConfWriter;
	StringWriter alarmsConfWriter;
	StringWriter modDeflateConfWriter;
	StringWriter targetGroupWriter;

	String bucketName;
	String x509CertificatePem;
	String privateKeyPem;

	@BeforeEach
	public void before() {
		MockitoAnnotations.initMocks(this);

		when(configuration.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(configuration.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("123");
		// Use the actual velocity entity
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		builder = new ElasticBeanstalkExtentionBuilderImpl(certifiateBuilder, velocityEngine, configuration,
				warAppender, fileProvider, mockCwlVelocityContextProvider, mockLoadBalanacerAlarmsConfig);
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
		logsConfWriter = new StringWriter();
		alarmsConfWriter = new StringWriter();
		modDeflateConfWriter = new StringWriter();
		targetGroupWriter = new StringWriter();

		when(fileProvider.createFileWriter(any(File.class))).thenReturn(configWriter, sslConfWriter,
				modSecurityConfWriter, modDeflateConfWriter, logsConfWriter, alarmsConfWriter, alarmsConfWriter,
				targetGroupWriter);
		bucketName = "someBucket";
		when(configuration.getConfigurationBucket()).thenReturn(bucketName);
		x509CertificatePem = "x509pem";
		privateKeyPem = "privatePem";
		when(certifiateBuilder.buildNewX509CertificatePair())
				.thenReturn(new CertificatePair(x509CertificatePem, privateKeyPem));
		when(mockCwlVelocityContextProvider.getLogDescriptors(any(EnvironmentType.class)))
				.thenReturn(generateLogDescriptors());
		when(mockLoadBalanacerAlarmsConfig.getOrDefault(any(), any())).thenReturn(generateLoadBalancerAlarms());
	}

	@Test
	public void testCopyWarWithExtensions() {
		// Call under test
		File warCopy = builder.copyWarWithExtensions(mockWar, EnvironmentType.REPOSITORY_SERVICES);

		assertNotNull(warCopy);
		assertEquals(mockWarCopy, warCopy);
		// httpconfig
		String httpConfigJson = configWriter.toString();
		JSONObject configJson = new JSONObject(httpConfigJson);

		// the contents should match the original file
		assertEquals(TemplateUtils.loadContentFromFile(TEMPLATES_REPO_EBEXTENSIONS_UPDATE_TOMCAT_SERVER_XML_SH),
				configJson.getJSONObject("files").getJSONObject("/tmp/update_tomcat_server_xml.sh")
						.getString("content"));
		assertEquals("{\"command\":\"sh /tmp/update_tomcat_server_xml.sh\"}",
				configJson.getJSONObject("container_commands").getJSONObject("00").toString());

		assertTrue(httpConfigJson.contains(x509CertificatePem));
		assertTrue(httpConfigJson.contains(privateKeyPem));
		// SSL conf
		String sslConf = sslConfWriter.toString();
		assertTrue(sslConf.contains("/etc/pki/tls/certs/server.crt"));
		assertTrue(sslConf.contains("/etc/pki/tls/certs/server.key"));
		// mod_security conf
		String modSecurityConf = modSecurityConfWriter.toString();
		assertTrue(modSecurityConf.contains("SecServerSignature"));
		// mod_deflate conf
		String modDeflateConf = modDeflateConfWriter.toString();
		assertTrue(modDeflateConf.contains("SetOutputFilter DEFLATE"));

		verify(mockCwlVelocityContextProvider).getLogDescriptors(EnvironmentType.REPOSITORY_SERVICES);

		String alarmsConf = alarmsConfWriter.toString();

		assertTrue(alarmsConf.contains("AWSELBSomeAlarm"));
		assertTrue(alarmsConf.contains("-AWS-ELB-Some-Alarm"));

		// alb target group
		String targetGroupConf = targetGroupWriter.toString();
		assertTrue(targetGroupConf.contains("\"Outputs\": "));
		assertTrue(targetGroupConf.contains("\"Value\": {\"Ref\" : \"AWSEBV2LoadBalancer\" },"));
		assertTrue(targetGroupConf.contains("\"repo-dev-123-0-alb-arn\""));
	}

	private List<LogDescriptor> generateLogDescriptors() {
		List<LogDescriptor> descriptors = new LinkedList<>();
		for (LogType t : LogType.values()) {
			LogDescriptor d = new LogDescriptor();
			d.setLogType(t);
			d.setLogPath("/var/log/mypath.log");
			d.setDateFormat("YYYY-MM-DD");
			descriptors.add(d);
		}
		return descriptors;
	}

	private List<LoadBalancerAlarm> generateLoadBalancerAlarms() {
		LoadBalancerAlarm alarm = new LoadBalancerAlarm();
		alarm.setName("Some-Alarm");
		alarm.setDescription("Description");
		alarm.setMetric("HTTPCode_ELB_5XX_Count");
		alarm.setComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold);
		alarm.setEvaluationPeriods(1);
		alarm.setStatistic(Statistic.Sum);
		alarm.setPeriod(60);
		alarm.setThreshold(20D);
		return Arrays.asList(alarm);
	}

}
