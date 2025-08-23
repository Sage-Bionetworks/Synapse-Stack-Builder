package org.sagebionetworks.template.markdownit;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Stack;

import java.io.File;
import java.io.StringWriter;
import java.util.Optional;

import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_ARTIFACT_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_MARKDOWNIT_ARTIFACT_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_MARKDOWNIT_ARTIFACT_VERSION;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_MARKDOWNIT_SUBNETS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_MARKDOWNIT_VPC;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

public class MarkDownItLambdaBuilderImpl implements MarkDownItLambdaBuilder {

    private static final Logger LOGGER = LogManager.getLogger(MarkDownItLambdaBuilderImpl.class);

    private RepoConfiguration config;

    private ArtifactDownload downloader;

    private CloudFormationClientWrapper cloudFormationClientWrapper;

    private StackTagsProvider tagsProvider;

    private AmazonS3 s3Client;

    private VelocityEngine velocityEngine;

    @Inject
    public MarkDownItLambdaBuilderImpl(RepoConfiguration config,
                                       ArtifactDownload downloader, CloudFormationClientWrapper cloudFormationClientWrapper,
                                       StackTagsProvider tagsProvider, AmazonS3 s3Client,
                                       VelocityEngine velocityEngine) {
        this.config = config;
        this.downloader = downloader;
        this.cloudFormationClientWrapper = cloudFormationClientWrapper;
        this.tagsProvider = tagsProvider;
        this.s3Client = s3Client;
        this.velocityEngine = velocityEngine;

    }

    @Override
    public void buildMarkDownItLambda() {

        String stack = config.getProperty(PROPERTY_KEY_STACK);
        String artifactBucket = config.getProperty(PROPERTY_KEY_LAMBDA_ARTIFACT_BUCKET);
        String version = config.getProperty(PROPERTY_KEY_LAMBDA_MARKDOWNIT_ARTIFACT_VERSION);
        String lambdaSourceArtifactUrl = getSourceArtifactUrl(version);
        String lambdaArtifactKey = String.format("artifacts/markdown-it/%s/markdown-it.zip", version);

        // Download from Github and upload to S3
        File artifact = downloader.downloadFile(lambdaSourceArtifactUrl);
        try {
            s3Client.putObject(artifactBucket, lambdaArtifactKey, artifact);
        } finally {
            artifact.delete();
        }

        buildMarkDownItLambdaStack(stack, artifactBucket, lambdaArtifactKey);

    }

    private String getSourceArtifactUrl(String version) {
        final String LAMBDA_SOURCE_ARTIFACT_URL_FORMAT = "https://github.com/Sage-Bionetworks/synapse-markdown-it-lambda/releases/download/%s/markdown-it.zip";
        String lambdaSourceArtifactUrl = String.format(LAMBDA_SOURCE_ARTIFACT_URL_FORMAT, version);
        return lambdaSourceArtifactUrl;
    }

    private Optional<Stack> buildMarkDownItLambdaStack(String stack, String artifactBucket, String artifactKey) {

        String stackName = String.format("%s-markdown-it-function-direct", stack);

        String vpcId = config.getProperty(PROPERTY_KEY_LAMBDA_MARKDOWNIT_VPC);
        String[] subnetIds = config.getCommaSeparatedProperty(PROPERTY_KEY_LAMBDA_MARKDOWNIT_SUBNETS);

        // Setup context
        VelocityContext context = new VelocityContext();
        context.put("stack", stack);
        context.put("lambdaArtifactBucket", artifactBucket);
        context.put("lambdaArtifactKey", artifactKey);
        context.put("vpcId", vpcId);
        context.put("subnetIds", subnetIds);

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
                .withTags(tagsProvider.getStackTags(config))
                .withCapabilities(Capability.CAPABILITY_NAMED_IAM);
        //cloudFormationClientWrapper.createOrUpdateStack(req);

        try {
            cloudFormationClientWrapper.waitForStackToComplete(stackName);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(cloudFormationClientWrapper.describeStack(stackName).orElseThrow(()->new IllegalStateException("Stack does not exist: "+stackName)));

    }
}
