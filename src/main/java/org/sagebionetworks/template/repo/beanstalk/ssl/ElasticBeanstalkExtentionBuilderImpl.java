package org.sagebionetworks.template.repo.beanstalk.ssl;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.FileProvider;
import org.sagebionetworks.war.AppenderCallback;
import org.sagebionetworks.war.WarAppender;

import com.google.inject.Inject;

public class ElasticBeanstalkExtentionBuilderImpl implements ElasticBeanstalkExtentionBuilder {

	CertificateProvider certificateProvider;
	VelocityEngine velocityEngine;
	Configuration configuration;
	WarAppender warAppender;
	FileProvider fileProvider;

	@Inject
	public ElasticBeanstalkExtentionBuilderImpl(CertificateProvider certificateProvider, VelocityEngine velocityEngine,
			Configuration configuration, WarAppender warAppender) {
		super();
		this.certificateProvider = certificateProvider;
		this.velocityEngine = velocityEngine;
		this.configuration = configuration;
		this.warAppender = warAppender;
	}



	@Override
	public File copyWarWithExtensions(File warFile) {
		VelocityContext context = new VelocityContext();
		context.put("s3bucket", configuration.getConfigurationBucket());
		// Get the certificate information
		context.put("certificates", certificateProvider.provideCertificateUrls());
		Template httpInstanceTempalte = velocityEngine.getTemplate("ebextensions/https-instance.config");
		
		return warAppender.appendFilesCopyOfWar(warFile, new AppenderCallback() {
			
			@Override
			public void appendFilesToDirectory(File dir) throws IOException {
				// ensure the .ebextensions directory exists
				File ebextensions = fileProvider.createNewFile(dir, ".ebextensions");
				ebextensions.mkdirs();
				// write the 
				try (Writer writer = fileProvider.createFileWriter(fileProvider.createNewFile(ebextensions, "https-instance.config"))){
					httpInstanceTempalte.merge(context, writer);
				}
			}
		});
		
	}

}
