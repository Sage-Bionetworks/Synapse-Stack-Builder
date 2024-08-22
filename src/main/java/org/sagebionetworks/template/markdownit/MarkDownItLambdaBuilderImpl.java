package org.sagebionetworks.template.markdownit;

import com.amazonaws.services.cloudformation.model.Stack;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import java.io.File;
import java.io.StringWriter;
import java.util.Optional;

import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_ARTIFACT_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_MARKDOWNIT_ARTIFACT_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

public class MarkDownItLambdaBuilderImpl implements MarkDownItLambdaBuilder {

    private static final Logger LOGGER = LogManager.getLogger(MarkDownItLambdaBuilderImpl.class);

    private RepoConfiguration config;

    private ArtifactDownload downloader;

    private CloudFormationClient cloudFormationClient;

    private StackTagsProvider tagsProvider;

    private AmazonS3 s3Client;

    private VelocityEngine velocityEngine;

    @Inject
    public MarkDownItLambdaBuilderImpl(RepoConfiguration config,
                                       ArtifactDownload downloader, CloudFormationClient cloudFormationClient,
                                       StackTagsProvider tagsProvider, AmazonS3 s3Client,
                                       VelocityEngine velocityEngine) {
        this.config = config;
        this.downloader = downloader;
        this.cloudFormationClient = cloudFormationClient;
        this.tagsProvider = tagsProvider;
        this.s3Client = s3Client;
        this.velocityEngine = velocityEngine;

    }

    @Override
    public void buildMarkDownItLambda() {

        String stack = config.getProperty(PROPERTY_KEY_STACK);
        String artifactBucket = config.getProperty(PROPERTY_KEY_LAMBDA_ARTIFACT_BUCKET);
        String lambdaSourceArtifactUrl = config.getProperty(PROPERTY_KEY_LAMBDA_MARKDOWNIT_ARTIFACT_URL);
        String lambdaArtifactKey = String.format("artifacts/markdown-it/%s", FilenameUtils.getName(lambdaSourceArtifactUrl));

        // Download from jfrog and upload to S3
        File artifact = downloader.downloadFile(lambdaSourceArtifactUrl);
        try {
            s3Client.putObject(artifactBucket, lambdaArtifactKey, artifact);
        } finally {
            artifact.delete();
        }

        buildMarkDownItLambdaStack(stack, artifactBucket, lambdaArtifactKey);

    }

    private Optional<Stack> buildMarkDownItLambdaStack(String stack, String artifactBucket, String artifactKey) {

        String stackName = String.format("%s-markdown-it-function", stack);

        // Setup context
        VelocityContext context = new VelocityContext();
        context.put("lambdaArtifactBucket", artifactBucket);
        context.put("lambdaArtifactKey", artifactKey);

        // Generate template
        Template template = velocityEngine.getTemplate(Constants.TEMPLATE_MARKDOWNIT_API_VTP);
        StringWriter stringWriter = new StringWriter();
        template.merge(context, stringWriter);
        String resultJSON = stringWriter.toString();
        LOGGER.info(resultJSON);

        // Create stack
        CreateOrUpdateStackRequest req = new CreateOrUpdateStackRequest()
                .withStackName(stackName)
                .withTemplateBody(resultJSON)
                .withTags(tagsProvider.getStackTags())
                .withCapabilities(CAPABILITY_NAMED_IAM);
        cloudFormationClient.createOrUpdateStack(req);

        try {
            cloudFormationClient.waitForStackToComplete(stackName);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(cloudFormationClient.describeStack(stackName).orElseThrow(()->new IllegalStateException("Stack does not exist: "+stackName)));

    }
}
