package org.sagebionetworks.template.s3;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Guice;
import com.google.inject.Injector;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
public class S3BucketBuilderIntegrationTest {

    @Mock
    private RepoConfiguration mockConfig;

    @Mock
    private S3Config mockS3Config;

    @Mock
    private AmazonS3 mockS3Client;

    @Mock
    private LambdaClient mockLambdaClient;

    @Mock
    private StsClient mockStsClient;

    @Mock
    private CloudFormationClientWrapper mockCloudFormationClientWrapper;

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

        builder = new S3BucketBuilderImpl(mockS3Client, mockStsClient, mockLambdaClient, mockConfig, mockS3Config, velocityEngine, mockCloudFormationClientWrapper, mockTagsProvider, mockDownloader);

        stack = "dev";
        accountId = "12345";

        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        GetCallerIdentityResponse expectedGetCallerIdentityResp = GetCallerIdentityResponse.builder().account(accountId).build();
        when(mockStsClient.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(expectedGetCallerIdentityResp);
    }
    
    @Test
    public void testBuildS3BucketPolicyStack() throws InterruptedException {
    	
    	S3BucketDescriptor dataBucket = new S3BucketDescriptor();
    	dataBucket.setName("${stack}data.sagebase.org");
    	dataBucket.setVirusScanEnabled(true);
    	
    	S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
    	inventoryBucket.setName("${stack}.datawarehouse.sagebase.org");

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        
        bucket.setName("${stack}.bucket.sagebase.org");
        bucket.setVirusScanEnabled(true);
        
        S3BucketDescriptor bucket2 = new S3BucketDescriptor();
        
        bucket2.setName("${stack}.bucket2.sagebase.org");
        bucket2.setDevOnly(true);

        when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(dataBucket, inventoryBucket, bucket, bucket2));

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        // Call under test
        builder.buildAllBuckets();

        String expectedStackName = stack + "-synapse-bucket-policies";
        String expectedBucketPolicyTemplate = new JSONObject(TemplateUtils.loadContentFromFile("s3/s3-bucket-policy-test.json")).toString(5);

        verify(mockCloudFormationClientWrapper).createOrUpdateStack(new CreateOrUpdateStackRequest()
                .withStackName("dev-synapse-bucket-policies")
                .withTemplateBody(expectedBucketPolicyTemplate)
                .withTags(Collections.emptyList()));

        verify(mockCloudFormationClientWrapper).waitForStackToComplete(expectedStackName);
        verify(mockCloudFormationClientWrapper).describeStack(expectedStackName);
    }
}