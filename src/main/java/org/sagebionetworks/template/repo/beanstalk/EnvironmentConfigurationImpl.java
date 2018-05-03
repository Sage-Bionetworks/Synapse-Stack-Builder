package org.sagebionetworks.template.repo.beanstalk;

import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_NUMBER;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.FileProvider;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.TemplateGuiceModule;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class EnvironmentConfigurationImpl implements EnvironmentConfiguration {

	public static final String FILE_RESOURCE_LOADER_PATH = "file.resource.loader.path";

	private AmazonS3 s3Client;
	private Configuration config;
	private VelocityEngine velocityEngine;
	private Logger logger;
	private FileProvider fileProvider;

	@Inject
	public EnvironmentConfigurationImpl(AmazonS3 s3Client, Configuration config, VelocityEngine velocityEngine,
			LoggerFactory loggerFactory, FileProvider fileProvider) {
		super();
		this.s3Client = s3Client;
		this.config = config;
		this.velocityEngine = velocityEngine;
		this.logger = loggerFactory.getLogger(EnvironmentConfigurationImpl.class);
		this.fileProvider = fileProvider;
	}

	@Override
	public String createEnvironmentConfiguration() {
		File temp = null;
		try {
			// download the template
			temp = downloadTemplate();
			velocityEngine.setProperty(FILE_RESOURCE_LOADER_PATH, temp.getParent());
			Template template = velocityEngine.getTemplate(temp.getName());
			// create the context used to create the final file
			VelocityContext context = createContext();
			// Generate the file
			StringWriter writer = new StringWriter();
			template.merge(context, writer);
			// Upload the file to S3
			return uploadResultFileToS3(writer.toString());
		} finally {
			if (temp != null) {
				temp.delete();
			}
		}
	}

	/**
	 * Download the template property file from S3
	 * 
	 * @return
	 */
	File downloadTemplate() {
		// download the template
		File temp;
		try {
			String bucket = config.getConfigurationBucket();
			String stack = config.getProperty(PROPERTY_KEY_STACK);
			String tempalteKey = "templates/" + stack + "-template-stack.properties";
			temp = fileProvider.createTempFile("StackTemplate", ".properties");
			logger.info("Downloading " + tempalteKey + "...");
			s3Client.getObject(new GetObjectRequest(bucket, tempalteKey), temp);
			return temp;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create the context used to merge with the template.
	 * 
	 * @return
	 */
	VelocityContext createContext() {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		// create the context used to create the final file
		VelocityContext context = new VelocityContext();
		context.put(STACK, stack);
		context.put(INSTANCE, instance);
		context.put(DB_ENDPOINT_SUFFIX, config.getProperty(PROPERTY_KEY_REPO_RDS_ENDPOINT_SUFFIX));
		context.put(REPO_NUMBER,
				config.getProperty(PROPERTY_KEY_BEANSTALK_NUMBER + EnvironmentType.REPOSITORY_SERVICES.getShortName()));
		return context;
	}

	/**
	 * Upload the result file to S3
	 * @param fileContents
	 */
	public String uploadResultFileToS3(String fileContents) {
		try {
			String bucket = config.getConfigurationBucket();
			String stack = config.getProperty(PROPERTY_KEY_STACK);
			String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
			byte[] bytes = fileContents.getBytes("UTF-8");
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			String resultKey = "Stack/" + stack + instance + "-stack.properties";
			ObjectMetadata meta = new ObjectMetadata();
			meta.setContentLength(bytes.length);
			logger.info("Uploading " + resultKey + "...");
			s3Client.putObject(new PutObjectRequest(bucket, resultKey, bis, meta));
			return "https://s3.amazonaws.com/" + bucket + "/" + resultKey;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		EnvironmentConfiguration ec = injector.getInstance(EnvironmentConfiguration.class);
		String url = ec.createEnvironmentConfiguration();
		System.out.println(url);
	}

}
