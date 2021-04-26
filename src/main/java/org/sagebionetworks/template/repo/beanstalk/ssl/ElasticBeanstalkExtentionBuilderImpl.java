package org.sagebionetworks.template.repo.beanstalk.ssl;

import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.CLOUDWATCH_LOGS_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.LOAD_BALANCER_ALARMS;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.function.Consumer;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.FileProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.template.repo.beanstalk.LoadBalancerAlarmsConfig;
import org.sagebionetworks.template.repo.cloudwatchlogs.CloudwatchLogsVelocityContextProvider;
import org.sagebionetworks.war.WarAppender;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class ElasticBeanstalkExtentionBuilderImpl implements ElasticBeanstalkExtentionBuilder {


	public static final String SSL_CONF = "ssl.conf";

	public static final String SECURITY_CONF = "security.conf";

	public static final String HTTPD_CONF_D = "httpd/conf.d";

	public static final String TEMPLATES_REPO_EBEXTENSIONS_HTTPS_SSL_CONF = "templates/repo/ebextensions/https-ssl.conf";

	public static final String TEMPLATES_REPO_EBEXTENSIONS_SECURITY_CONF = "templates/repo/ebextensions/security.conf";

	public static final String INSTANCE_CONFIG = "instance.config";

	public static final String DOT_EBEXTENSIONS = ".ebextensions";

	public static final String TEMPLATE_EBEXTENSIONS_INSTANCE_CONFIG = "templates/repo/ebextensions/instance.config";

	public static final String TEMPLATE_EBEXTENSIONS_BEANSTALK_LOGS_CW_CONFIG = "templates/repo/ebextensions/beanstalk_logs_cloudwatch.config";

	public static final String BEANSTALK_LOGS_CW_CONFIG = "beanstalk_cwlogs.config";
	
	public static final String TEMPLATE_EBEXTENSIONS_BEANSTALK_ALARMS = "templates/repo/ebextensions/beanstalk_alarms.config";

	public static final String BEANSTALK_ALARMS_CONFIG = "beanstalk_alarms.config";

	CertificateBuilder certificateBuilder;
	VelocityEngine velocityEngine;
	Configuration configuration;
	WarAppender warAppender;
	FileProvider fileProvider;
	CloudwatchLogsVelocityContextProvider cwlContextprovider;
	LoadBalancerAlarmsConfig loadBalancerAlarmsConfig;

	@Inject
	public ElasticBeanstalkExtentionBuilderImpl(CertificateBuilder certificateBuilder, VelocityEngine velocityEngine,
			Configuration configuration, WarAppender warAppender, FileProvider fileProvider, CloudwatchLogsVelocityContextProvider cwlCtxtProvider,
			LoadBalancerAlarmsConfig loadBalancerAlarmsConfig) {
		super();
		this.certificateBuilder = certificateBuilder;
		this.velocityEngine = velocityEngine;
		this.configuration = configuration;
		this.warAppender = warAppender;
		this.fileProvider = fileProvider;
		this.cwlContextprovider = cwlCtxtProvider;
		this.loadBalancerAlarmsConfig = loadBalancerAlarmsConfig;
	}

	@Override
	public File copyWarWithExtensions(File warFile, EnvironmentType envType) {
		VelocityContext context = new VelocityContext();
		context.put("s3bucket", configuration.getConfigurationBucket());
		// Get the certificate information
		context.put("certificates", certificateBuilder.buildNewX509CertificatePair());
		// EnvironmentType in context
		context.put("envType", envType);
		// CloudwatchLog descriptors
		context.put(CLOUDWATCH_LOGS_DESCRIPTORS, cwlContextprovider.getLogDescriptors(envType));
		
		String stack = configuration.getProperty(PROPERTY_KEY_STACK);
		String instance = configuration.getProperty(PROPERTY_KEY_INSTANCE);
		
		context.put("stack", stack);
		context.put("instance", instance);
		
		// Exported resources prefix
		context.put(GLOBAL_RESOURCES_EXPORT_PREFIX, Constants.createGlobalResourcesExportPrefix(stack));
		// Inject the alarms configuration for the environment
		context.put(LOAD_BALANCER_ALARMS, loadBalancerAlarmsConfig.getOrDefault(envType, Collections.emptyList()));

		// add the files to the copy of the war
		return warAppender.appendFilesCopyOfWar(warFile, new Consumer<File>() {

			@Override
			public void accept(File directory) {
				// ensure the .ebextensions directory exists
				File ebextensionsDirectory = fileProvider.createNewFile(directory, DOT_EBEXTENSIONS);
				ebextensionsDirectory.mkdirs();
				// ensure the .ebextensions/httpd/conf.d directory exists.
				File confDDirectory = fileProvider.createNewFile(ebextensionsDirectory, HTTPD_CONF_D);
				confDDirectory.mkdirs();
				// https-instance.config
				Template httpInstanceTempalte = velocityEngine.getTemplate(TEMPLATE_EBEXTENSIONS_INSTANCE_CONFIG);
				File resultFile = fileProvider.createNewFile(ebextensionsDirectory, INSTANCE_CONFIG);
				addTemplateAsFileToDirectory(httpInstanceTempalte, context, resultFile);
				// SSL conf
				resultFile = fileProvider.createNewFile(confDDirectory, SSL_CONF);
				Template sslconf = velocityEngine.getTemplate(TEMPLATES_REPO_EBEXTENSIONS_HTTPS_SSL_CONF);
				addTemplateAsFileToDirectory(sslconf, context, resultFile);
				// ModSecurity conf
				resultFile = fileProvider.createNewFile(confDDirectory, SECURITY_CONF);
				Template modSecurityConf = velocityEngine.getTemplate(TEMPLATES_REPO_EBEXTENSIONS_SECURITY_CONF);
				addTemplateAsFileToDirectory(modSecurityConf, context, resultFile);
				// Beanstalk logs CloudwatchLogs config
				resultFile = fileProvider.createNewFile(ebextensionsDirectory, BEANSTALK_LOGS_CW_CONFIG);
				Template beanstalkClodwatchConf = velocityEngine.getTemplate(TEMPLATE_EBEXTENSIONS_BEANSTALK_LOGS_CW_CONFIG);
				addTemplateAsFileToDirectory(beanstalkClodwatchConf, context, resultFile);
				// Beanstalk environment alarms
				resultFile = fileProvider.createNewFile(ebextensionsDirectory, BEANSTALK_ALARMS_CONFIG);
				Template beanstalkAlarms = velocityEngine.getTemplate(TEMPLATE_EBEXTENSIONS_BEANSTALK_ALARMS);
				addTemplateAsFileToDirectory(beanstalkAlarms, context, resultFile);
			}
		});

	}

	/**
	 * Merge the passed template and context and save the results as a new file in
	 * the passed directory with the given name.
	 * 
	 * @param tempalte
	 * @param context
	 * @param resultFile
	 */
	public void addTemplateAsFileToDirectory(Template tempalte, VelocityContext context, File resultFile) {
		try (Writer writer = fileProvider
				.createFileWriter(resultFile)) {
			tempalte.merge(context, writer);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Helper to run the actual builder
	 * @param args
	 */
	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		ElasticBeanstalkExtentionBuilder builder = injector.getInstance(ElasticBeanstalkExtentionBuilder.class);
		File resultWar = builder.copyWarWithExtensions(new File(args[0]), EnvironmentType.REPOSITORY_SERVICES);
		System.out.println(resultWar.getAbsolutePath());
	}

}
