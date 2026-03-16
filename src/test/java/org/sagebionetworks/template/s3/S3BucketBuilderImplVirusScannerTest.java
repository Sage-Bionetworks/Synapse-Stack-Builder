package org.sagebionetworks.template.s3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
public class S3BucketBuilderImplVirusScannerTest {

    @Mock
    private RepoConfiguration mockConfig;

    @Mock
    private S3Config mockS3Config;

    @Mock
    private S3Client mockS3Client;

    @Mock
    private StsClient mockStsClient;

    @Mock
    private LambdaClient mockLambdaClient;

    @Mock
    private VelocityEngine mockVelocity;

    @Mock
    private CloudFormationClientWrapper mockCloudFormationClientWrapper;

    @Mock
    private StackTagsProvider mockTagsProvider;

    @Mock
    private ArtifactDownload mockDownloader;

    @InjectMocks
    private S3BucketBuilderImpl builder;

    @Mock
    private Template mockTemplate;

    @Mock
    private File mockFile;

    @Captor
    private ArgumentCaptor<PutBucketEncryptionRequest> encryptionRequestCaptor;

    @Captor
    private ArgumentCaptor<InventoryConfiguration> inventoryConfigurationCaptor;

    @Captor
    private ArgumentCaptor<BucketLifecycleConfiguration> bucketLifeCycleConfigurationCaptor;

    @Captor
    private ArgumentCaptor<VelocityContext> velocityContextCaptor;

    @Captor
    private ArgumentCaptor<IntelligentTieringConfiguration> intConfigurationCaptor;

    private String stack;
    private String accountId;

    @BeforeEach
    public void before() {
        stack = "dev";
        accountId = "12345";

        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        GetCallerIdentityResponse expectedGetCallerIdentityResponse = GetCallerIdentityResponse.builder().account(accountId).build();
        when(mockStsClient.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(expectedGetCallerIdentityResponse);
    }

    @Test
    public void testBuildAllBucketsWithVirusScannerConfiguration() throws InterruptedException {
        S3BucketDescriptor bucket = new S3BucketDescriptor();

        bucket.setName("bucket");
        bucket.setVirusScanEnabled(true);

        when(mockConfig.getProperty(Constants.PROPERTY_KEY_LAMBDA_VIRUS_SCANNER_ARTIFACT_URL)).thenReturn("https://some-url/lambda-name.zip");
        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));

        S3VirusScannerConfig virusScannerConfig = new S3VirusScannerConfig();

        virusScannerConfig.setLambdaArtifactBucket("${stack}-lambda-bucket");
        virusScannerConfig.setNotificationEmail("notification@sagebase.org");

        when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack virusScannerStack = Stack.builder()
                .outputs(
                        Output.builder().outputKey(S3BucketBuilderImpl.CF_OUTPUT_VIRUS_TRIGGER_TOPIC).outputValue("snsTopicArn").build(),
                        Output.builder().outputKey(S3BucketBuilderImpl.CF_OUTPUT_VIRUS_UPDATER_LAMBDA).outputValue("updaterLambdaArn").build()
                ).build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(virusScannerStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(GetBucketNotificationConfigurationResponse.builder().build());

        byte[] testData = "test data".getBytes();
        ByteArrayInputStream mockInputStream = new ByteArrayInputStream(testData);
        when(mockDownloader.downloadAsBytes(any())).thenReturn(testData);

        // Call under test
        builder.buildAllBuckets();

        String expectedBucket = stack + "-lambda-bucket";
        String expectedKey = "artifacts/virus-scanner/lambda-name.zip";

        verify(mockDownloader).downloadAsBytes("https://some-url/lambda-name.zip");
        verify(mockS3Client).putObject(
                    argThat((PutObjectRequest req) ->
                            req.bucket().equals(expectedBucket) && req.key().equals(expectedKey)),
                    any(RequestBody.class)
                    );

        verify(mockTemplate, times(2)).merge(velocityContextCaptor.capture(), any());

        List<VelocityContext> contexts = velocityContextCaptor.getAllValues();
        VelocityContext virusScannerBuilderContext = contexts.get(0);
        VelocityContext bucketPolicyBuilderContext = contexts.get(1);

        assertEquals(virusScannerBuilderContext.get(Constants.STACK), stack);
        assertEquals(virusScannerBuilderContext.get(S3BucketBuilderImpl.CF_PROPERTY_BUCKETS), Arrays.asList(bucket.getName()));
        assertEquals(virusScannerBuilderContext.get(S3BucketBuilderImpl.CF_PROPERTY_NOTIFICATION_EMAIL), "notification@sagebase.org");
        assertEquals(virusScannerBuilderContext.get(S3BucketBuilderImpl.CF_PROPERTY_LAMBDA_BUCKET), expectedBucket);
        assertEquals(virusScannerBuilderContext.get(S3BucketBuilderImpl.CF_PROPERTY_LAMBDA_KEY), expectedKey);

        assertEquals(bucketPolicyBuilderContext.get(Constants.STACK), stack);

        String expectedVirusScannerStackName = stack + "-synapse-virus-scanner";
        String expectedBucketPolicyStackName = stack + "-synapse-bucket-policies";

        ArgumentCaptor<CreateOrUpdateStackRequest> argCreateOrUpdateStack = ArgumentCaptor.forClass(CreateOrUpdateStackRequest.class);
        ArgumentCaptor<String> argCaptorWaitForStack = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> argCaptorDescribeStack = ArgumentCaptor.forClass(String.class);

        verify(mockCloudFormationClientWrapper, times(2)).createOrUpdateStack(argCreateOrUpdateStack.capture());
        verify(mockCloudFormationClientWrapper, times(2)).waitForStackToComplete(argCaptorWaitForStack.capture());
        verify(mockCloudFormationClientWrapper, times(2)).describeStack(argCaptorDescribeStack.capture());

        List<CreateOrUpdateStackRequest> capturedCreateOrUpdateStackArgs = argCreateOrUpdateStack.getAllValues();
        List<String> capturedWaitForStackArgs = argCaptorWaitForStack.getAllValues();
        List<String> capturedDescribeStackArgs = argCaptorDescribeStack.getAllValues();

        assertEquals(capturedCreateOrUpdateStackArgs.get(0), new CreateOrUpdateStackRequest()
                .withStackName(expectedVirusScannerStackName)
                .withTemplateBody("{}")
                .withTags(Collections.emptyList())
                .withCapabilities(Capability.CAPABILITY_NAMED_IAM));

        assertEquals(capturedCreateOrUpdateStackArgs.get(1), new CreateOrUpdateStackRequest()
                .withStackName(expectedBucketPolicyStackName)
                .withTemplateBody("{}")
                .withTags(Collections.emptyList()));

        assertEquals(expectedVirusScannerStackName, capturedWaitForStackArgs.get(0));
        assertEquals(expectedVirusScannerStackName, capturedDescribeStackArgs.get(0));

        assertEquals(expectedBucketPolicyStackName, capturedWaitForStackArgs.get(1));
        assertEquals(expectedBucketPolicyStackName, capturedDescribeStackArgs.get(1));

        ArgumentCaptor<PutBucketNotificationConfigurationRequest> putBucketNotificationConfigurationReqCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
        verify(mockS3Client).putBucketNotificationConfiguration(putBucketNotificationConfigurationReqCaptor.capture());
        PutBucketNotificationConfigurationRequest actualPutBucketNotificationConfigurationReq = putBucketNotificationConfigurationReqCaptor.getValue();
        assertEquals(bucket.getName(), actualPutBucketNotificationConfigurationReq.bucket());
        NotificationConfiguration actualConfig = actualPutBucketNotificationConfigurationReq.notificationConfiguration();
        assertTrue(actualConfig.hasTopicConfigurations());
        assertEquals(1, actualConfig.topicConfigurations().size());
        TopicConfiguration actualTopicConfig = actualConfig.topicConfigurations().get(0);
        assertEquals(S3BucketBuilderImpl.VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME, actualTopicConfig.id());
        assertEquals("snsTopicArn", actualTopicConfig.topicArn());
        assertEquals(List.of(Event.S3_OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD), actualTopicConfig.events());

        verify(mockLambdaClient).invoke(InvokeRequest.builder()
                .functionName("updaterLambdaArn")
                .invocationType(InvocationType.EVENT)
                .build()
        );	}

    @Test
    public void testBuildAllBucketsWithVirusScannerConfigurationAndBucketNotificationRemoval() throws InterruptedException {
        S3BucketDescriptor bucket = new S3BucketDescriptor();

        bucket.setName("bucket");
        bucket.setVirusScanEnabled(false);

        when(mockConfig.getProperty(Constants.PROPERTY_KEY_LAMBDA_VIRUS_SCANNER_ARTIFACT_URL)).thenReturn("https://some-url/lambda-name.zip");
        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));

        S3VirusScannerConfig virusScannerConfig = new S3VirusScannerConfig();

        virusScannerConfig.setLambdaArtifactBucket("${stack}-lambda-bucket");
        virusScannerConfig.setNotificationEmail("notification@sagebase.org");

        when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        Stack virusScannerStack = Stack.builder()
                .outputs(
                        Output.builder().outputKey(S3BucketBuilderImpl.CF_OUTPUT_VIRUS_TRIGGER_TOPIC).outputValue("snsTopicArn").build(),
                        Output.builder().outputKey(S3BucketBuilderImpl.CF_OUTPUT_VIRUS_UPDATER_LAMBDA).outputValue("updaterLambdaArn").build()
                ).build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(virusScannerStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);
        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        GetBucketNotificationConfigurationResponse expectedGetBucketNotificationConfigurationResponse = GetBucketNotificationConfigurationResponse.builder()
                        .topicConfigurations(Collections.singleton(TopicConfiguration.builder()
                                        .id(S3BucketBuilderImpl.VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME)
                                        .build()))
                        .build();
        when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(expectedGetBucketNotificationConfigurationResponse);

        String expectedBucket = stack + "-lambda-bucket";
        String expectedKey = "artifacts/virus-scanner/lambda-name.zip";

        byte[] testData = "test data".getBytes();
        when(mockDownloader.downloadAsBytes(any())).thenReturn(testData);

        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        ArgumentCaptor<PutBucketNotificationConfigurationRequest> putBucketNotificationConfigurationReqCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
        verify(mockS3Client).putBucketNotificationConfiguration(putBucketNotificationConfigurationReqCaptor.capture());
        PutBucketNotificationConfigurationRequest actualPutBucketNotificationConfigurationReq = putBucketNotificationConfigurationReqCaptor.getValue();
        assertEquals(bucket.getName(),  actualPutBucketNotificationConfigurationReq.bucket());
        NotificationConfiguration actualConfig = actualPutBucketNotificationConfigurationReq.notificationConfiguration();
        assertTrue(actualConfig.hasTopicConfigurations() && actualConfig.topicConfigurations().isEmpty());

        verify(mockTemplate, times(2)).merge(velocityContextCaptor.capture(), any());
        List<VelocityContext> contexts = velocityContextCaptor.getAllValues();
        VelocityContext virusScannerBuilderContext = contexts.get(0);
        VelocityContext bucketPolicyBuilderContext = contexts.get(1);

        assertEquals(virusScannerBuilderContext.get(Constants.STACK), stack);
        assertEquals(bucketPolicyBuilderContext.get(Constants.STACK), stack);

        assertEquals(virusScannerBuilderContext.get(Constants.STACK), stack);
        assertEquals(virusScannerBuilderContext.get(S3BucketBuilderImpl.CF_PROPERTY_BUCKETS), List.of());
        assertEquals(virusScannerBuilderContext.get(S3BucketBuilderImpl.CF_PROPERTY_NOTIFICATION_EMAIL), "notification@sagebase.org");
        assertEquals(virusScannerBuilderContext.get(S3BucketBuilderImpl.CF_PROPERTY_LAMBDA_BUCKET), expectedBucket);
        assertEquals(virusScannerBuilderContext.get(S3BucketBuilderImpl.CF_PROPERTY_LAMBDA_KEY), expectedKey);

        assertEquals(bucketPolicyBuilderContext.get(Constants.STACK), stack);

        String expectedVirusScannerStackName = stack + "-synapse-virus-scanner";
        String expectedBucketPolicyStackName = stack + "-synapse-bucket-policies";

        ArgumentCaptor<CreateOrUpdateStackRequest> argCreateOrUpdateStack = ArgumentCaptor.forClass(CreateOrUpdateStackRequest.class);
        ArgumentCaptor<String> argCaptorWaitForStack = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> argCaptorDescribeStack = ArgumentCaptor.forClass(String.class);

        verify(mockCloudFormationClientWrapper, times(2)).createOrUpdateStack(argCreateOrUpdateStack.capture());
        verify(mockCloudFormationClientWrapper, times(2)).waitForStackToComplete(argCaptorWaitForStack.capture());
        verify(mockCloudFormationClientWrapper, times(2)).describeStack(argCaptorDescribeStack.capture());

        List<CreateOrUpdateStackRequest> capturedCreateOrUpdateStackArgs = argCreateOrUpdateStack.getAllValues();
        List<String> capturedWaitForStackArgs = argCaptorWaitForStack.getAllValues();
        List<String> capturedDescribeStackArgs = argCaptorDescribeStack.getAllValues();

        assertEquals(capturedCreateOrUpdateStackArgs.get(0), new CreateOrUpdateStackRequest()
                .withStackName(expectedVirusScannerStackName)
                .withTemplateBody("{}")
                .withTags(Collections.emptyList())
                .withCapabilities(Capability.CAPABILITY_NAMED_IAM));

        assertEquals(capturedCreateOrUpdateStackArgs.get(1), new CreateOrUpdateStackRequest()
                .withStackName(expectedBucketPolicyStackName)
                .withTemplateBody("{}")
                .withTags(Collections.emptyList()));

        assertEquals(expectedVirusScannerStackName, capturedWaitForStackArgs.get(0));
        assertEquals(expectedVirusScannerStackName, capturedDescribeStackArgs.get(0));

        assertEquals(expectedBucketPolicyStackName, capturedWaitForStackArgs.get(1));
        assertEquals(expectedBucketPolicyStackName, capturedDescribeStackArgs.get(1));
    }

    @Test
    public void testBuildAllBucketsWithNoVirusScannerConfiguration() throws InterruptedException {
        S3VirusScannerConfig virusScannerConfig = null;

        when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);

        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        // Call under test
        builder.buildAllBuckets();

        verify(mockTemplate).merge(velocityContextCaptor.capture(), any());

        VelocityContext context = velocityContextCaptor.getValue();

        assertEquals(context.get(Constants.STACK), stack);

        String expectedStackName = stack + "-synapse-bucket-policies";

        verify(mockCloudFormationClientWrapper).createOrUpdateStack(new CreateOrUpdateStackRequest()
                .withStackName(expectedStackName)
                .withTemplateBody("{}")
                .withTags(Collections.emptyList()));

        verify(mockCloudFormationClientWrapper).waitForStackToComplete(expectedStackName);
        verify(mockCloudFormationClientWrapper).describeStack(expectedStackName);

    }

}
