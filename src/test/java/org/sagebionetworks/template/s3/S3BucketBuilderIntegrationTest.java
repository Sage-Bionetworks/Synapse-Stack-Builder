package org.sagebionetworks.template.s3;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import com.amazonaws.services.cloudformation.model.Stack;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.google.inject.Guice;
import com.google.inject.Injector;

@ExtendWith(MockitoExtension.class)
public class S3BucketBuilderIntegrationTest {

    @Mock
    private RepoConfiguration mockConfig;

    @Mock
    private S3Config mockS3Config;

    @Mock
    private AmazonS3 mockS3Client;

    @Mock
    private AWSLambda mockLambdaClient;

    @Mock
    private AWSSecurityTokenService mockStsClient;

    @Mock
    private CloudFormationClient mockCloudFormationClient;

    @Mock
    private GetCallerIdentityResult mockGetCallerIdentityResult;

    @Mock
    private StackTagsProvider mockTagsProvider;

    @Mock
    private ArtifactDownload mockDownloader;

    private S3BucketBuilderImpl builder;
    private String stack;
    private String accountId;

    @BeforeEach
    public void before() {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        VelocityEngine velocityEngine = injector.getInstance(VelocityEngine.class);

        // Validate the real S3Config
        injector.getInstance(S3Config.class);

        builder = new S3BucketBuilderImpl(mockS3Client, mockStsClient, mockLambdaClient, mockConfig, mockS3Config, velocityEngine, mockCloudFormationClient, mockTagsProvider, mockDownloader);

        stack = "dev";
        accountId = "12345";

        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        when(mockStsClient.getCallerIdentity(any())).thenReturn(mockGetCallerIdentityResult);
        when(mockGetCallerIdentityResult.getAccount()).thenReturn(accountId);
    }

    @Test
    public void testBuildS3BucketPolicyStack() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();

        when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));

        Stack bucketPolicyStack = new Stack();

        when(mockCloudFormationClient.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags()).thenReturn(Collections.emptyList());

        // Call under test
        builder.buildAllBuckets();

        String expectedStackName = stack + "-synapse-bucket-policies";
        String expectedBucketPolicyTemplate = new JSONObject(TemplateUtils.loadContentFromFile("s3/s3-bucket-policy-test.json")).toString(5);

        verify(mockCloudFormationClient).createOrUpdateStack(new CreateOrUpdateStackRequest()
                .withStackName("dev-synapse-bucket-policies")
                .withTemplateBody(expectedBucketPolicyTemplate)
                .withTags(Collections.emptyList()));

        verify(mockCloudFormationClient).waitForStackToComplete(expectedStackName);
        verify(mockCloudFormationClient).describeStack(expectedStackName);
    }
}