package org.sagebionetworks.template.agent;

import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ARTIFACT_VERSION;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.StringJoiner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.utils.ArtifactDownload;
import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.inject.Inject;

public class AgentBuilderImpl implements AgentBuilder {

	private static final String GITHUB_URL_TEMPLATE = "https://github.com/Sage-Bionetworks/Synapse-Agent-Lambda/releases/download/%s/%s-develop-SNAPSHOT.jar";
	private final CloudFormationClient cloudFormationClient;
	private final VelocityEngine velocityEngine;
	private final Configuration config;
	private final Logger logger;
	private final ArtifactDownload downloader;
	private final AmazonS3 s3Client;
	private final StackTagsProvider tagsProvider;

	@Inject
	public AgentBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
			Configuration config, LoggerFactory loggerFactory, ArtifactDownload downloader, AmazonS3 s3Client,
			StackTagsProvider tagsProvider) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.config = config;
		this.logger = loggerFactory.getLogger(AgentBuilderImpl.class);
		this.downloader = downloader;
		this.s3Client = s3Client;
		this.tagsProvider = tagsProvider;
	}

	@Override
	public void buildAndDeploy() throws IOException {

		String stack = config.getProperty(PROPERTY_KEY_STACK);
		ValidateArgument.requiredNotEmpty(stack, PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		ValidateArgument.requiredNotEmpty(instance, PROPERTY_KEY_INSTANCE);
		String artifactVesion = config.getProperty(PROPERTY_KEY_ARTIFACT_VERSION);
		ValidateArgument.requiredNotEmpty(artifactVesion, PROPERTY_KEY_ARTIFACT_VERSION);
		String lambdaURL = String.format(GITHUB_URL_TEMPLATE, artifactVesion, "lambda");

		String bucket = String.format("%s.lambda.sagebase.org", stack);
		String path = String.format("artifacts/bedrock-agent/%s/", artifactVesion);

		// copy the lambda from github and save it to the S3 bucket.
		File lambdaJar = downloader.downloadFile(lambdaURL);
		String lambdaKey = path + "lambda-develop-SNAPSHOT.jar";
		try {
			s3Client.putObject(new PutObjectRequest(bucket, lambdaKey, lambdaJar));
		} finally {
			lambdaJar.delete();
		}

		// copy the layer from github, package it into a zip and then save the zip S3.
		String layerURL = String.format(GITHUB_URL_TEMPLATE, artifactVesion, "layer");
		String layerKey = createAndUploadLayerZip(bucket, path, layerURL);
		String layerName = stack + instance + "layer";
		String actionGroupName = stack+instance+"actionGroupFunctions";
		String stackName = new StringJoiner("-").add(stack).add(instance).add("bedrock-agent").toString();

		VelocityContext context = new VelocityContext();
		context.put("stack", stack);
		context.put("instance", instance);
		context.put("bucketName", bucket);
		context.put("layerZipKey", layerKey);
		context.put("layerName", layerName);
		context.put("lambdaJarKey", lambdaKey);
		context.put("actionGroupFunctionName", actionGroupName);
		
        // Merge the context with the template
        Template template = this.velocityEngine.getTemplate("templates/repo/bedrock/bedrock-resources-template.json");
        
        StringWriter stringWriter = new StringWriter();
        template.merge(context, stringWriter);
        // Parse the resulting template
        String resultJSON = stringWriter.toString();
        JSONObject templateJson = new JSONObject(resultJSON);

        // Format the JSON
        resultJSON = templateJson.toString(JSON_INDENT);
        this.logger.info(resultJSON);
        System.out.println(resultJSON);
        // create or update the template
        this.cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest().withStackName(stackName)
                .withTemplateBody(resultJSON).withTags(tagsProvider.getStackTags())
                .withCapabilities(CAPABILITY_NAMED_IAM));
	}

	/**
	 * Create a layer zip by downloading the jar from githup, adding the jar to a
	 * zip, and then uploading the zip to S3
	 * 
	 * @param bucket
	 * @param path
	 * @param layerURL
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	String createAndUploadLayerZip(String bucket, String path, String layerURL)
			throws IOException, FileNotFoundException {
		File layerJar = downloader.downloadFile(layerURL);
		try {
			File zip = File.createTempFile("layerZip", ".zip");
			try {
				try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zip));
						FileInputStream fis = new FileInputStream(layerJar)) {
					ZipEntry zipEntry = new ZipEntry("java/lib/lambda-layer-develop-SNAPSHOT.jar");
					zipOut.putNextEntry(zipEntry);
					byte[] bytes = new byte[1024];
					int length;
					while ((length = fis.read(bytes)) >= 0) {
						zipOut.write(bytes, 0, length);
					}
				}
				String key = path + "layer-develop-SNAPSHOT.zip";
				s3Client.putObject(new PutObjectRequest(bucket, key, zip));
				return key;
			} finally {
				zip.delete();
			}
		} finally {
			layerJar.delete();
		}
	}

}
