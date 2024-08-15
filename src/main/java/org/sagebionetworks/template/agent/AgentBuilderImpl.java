package org.sagebionetworks.template.agent;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_ARTIFACT_VERSION;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.File;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.LoggerFactory;
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

	@Inject
	public AgentBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine,
			Configuration config, LoggerFactory loggerFactory, ArtifactDownload downloader, AmazonS3 s3Client) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.config = config;
        this.logger = loggerFactory.getLogger(AgentBuilderImpl.class);
		this.downloader = downloader;
		this.s3Client = s3Client;
	}

	@Override
	public void buildAndDeploy() {

		String stack = config.getProperty(PROPERTY_KEY_STACK);
        ValidateArgument.requiredNotEmpty(stack, PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
        ValidateArgument.requiredNotEmpty(instance, PROPERTY_KEY_INSTANCE);
		String artifactVesion = config.getProperty(PROPERTY_KEY_ARTIFACT_VERSION);
        ValidateArgument.requiredNotEmpty(artifactVesion, PROPERTY_KEY_ARTIFACT_VERSION);
        String lambdaURL = String.format(GITHUB_URL_TEMPLATE, artifactVesion, "lambda");
        String layerURL = String.format(GITHUB_URL_TEMPLATE, artifactVesion, "layer");

        String bucket = String.format("%s.lambda.sagebase.org", stack);
        String path = String.format("bedrock-agent/%s/", artifactVesion);
        File buildArtifacts = downloader.downloadFile(lambdaURL);
        try {
        	String key = path+"lambda-develop-SNAPSHOT.jar";
        	s3Client.putObject(new PutObjectRequest(bucket, key, buildArtifacts));
        }finally {
        	buildArtifacts.delete();
        }
       	
	}

}
