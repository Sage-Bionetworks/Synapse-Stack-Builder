package org.sagebionetworks.template.repo.beanstalk.ssl;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.function.Consumer;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.FileProvider;
import org.sagebionetworks.war.WarAppender;

import com.google.inject.Inject;

public class ElasticBeanstalkExtentionBuilderImpl implements ElasticBeanstalkExtentionBuilder {

	public static final String TEMPLATE_EBEXTENSION_SERVER_XML = "/templates/repo/ebextensions/server.xml";

	public static final String HTTPS_INSTANCE_CONFIG = "https-instance.config";

	public static final String DOT_EBEXTENSIONS = ".ebextensions";

	public static final String TEMPLATE_EBEXTENSIONS_HTTP_INSTANCE_CONFIG = "templates/repo/ebextensions/https-instance.config";

	CertificateBuilder certificateBuilder;
	VelocityEngine velocityEngine;
	Configuration configuration;
	WarAppender warAppender;
	FileProvider fileProvider;

	@Inject
	public ElasticBeanstalkExtentionBuilderImpl(CertificateBuilder certificateBuilder, VelocityEngine velocityEngine,
			Configuration configuration, WarAppender warAppender, FileProvider fileProvider) {
		super();
		this.certificateBuilder = certificateBuilder;
		this.velocityEngine = velocityEngine;
		this.configuration = configuration;
		this.warAppender = warAppender;
		this.fileProvider = fileProvider;
	}

	@Override
	public File copyWarWithExtensions(File warFile) {
		VelocityContext context = new VelocityContext();
		context.put("s3bucket", configuration.getConfigurationBucket());
		// Get the certificate information
		context.put("certificates", certificateBuilder.buildNewX509CertificatePair());



		// add the files to the copy of the war
		return warAppender.appendFilesCopyOfWar(warFile, new Consumer<File>() {

			@Override
			public void accept(File directory) {
				// ensure the .ebextensions directory exists
				File ebextensions = fileProvider.createNewFile(directory, DOT_EBEXTENSIONS);
				ebextensions.mkdirs();
				// https-instance.config
				Template httpInstanceTempalte = velocityEngine.getTemplate(TEMPLATE_EBEXTENSIONS_HTTP_INSTANCE_CONFIG);
				addTemplateAsFileToDirectory(httpInstanceTempalte, context, ebextensions, HTTPS_INSTANCE_CONFIG);
				// server.xml
				Template serverTempalte = velocityEngine.getTemplate(TEMPLATE_EBEXTENSION_SERVER_XML);
				addTemplateAsFileToDirectory(serverTempalte, context, ebextensions, "server.xml");
			}
		});

	}

	/**
	 * Merge the passed template and context and save the results as a new file in
	 * the passed directory with the given name.
	 * 
	 * @param tempalte
	 * @param context
	 * @param destinationDirectory
	 * @param resultFileName
	 */
	public void addTemplateAsFileToDirectory(Template tempalte, VelocityContext context, File destinationDirectory,
			String resultFileName) {
		try (Writer writer = fileProvider
				.createFileWriter(fileProvider.createNewFile(destinationDirectory, resultFileName))) {
			tempalte.merge(context, writer);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
