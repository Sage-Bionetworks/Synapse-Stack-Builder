package org.sagebionetworks.template.s3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
public class S3BucketBuilderImplTransitionRulesTest {

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
    public void testBuildAllBucketsWithTransitionRule() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setStorageClassTransitions(Collections.singletonList(
                new S3BucketClassTransition()
                        .withStorageClass(TransitionStorageClass.INTELLIGENT_TIERING)
                        .withDays(30)
        ));

        String expectedBucketName = stack + ".bucket";

        when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        BucketLifecycleConfiguration expectedBucketLifecycleConfiguration = BucketLifecycleConfiguration.builder().build();
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedBucketLifecycleConfiguration.rules()).build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);


        // Call under test
        builder.buildAllBuckets();

        verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
        verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));

        ArgumentCaptor<PutBucketLifecycleConfigurationRequest> putBucketLifecycleConfigurationRequestCaptor = ArgumentCaptor.forClass(PutBucketLifecycleConfigurationRequest.class);
        verify(mockS3Client).putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequestCaptor.capture());
        PutBucketLifecycleConfigurationRequest actualPutBucketLifecycleConfigurationRequest = putBucketLifecycleConfigurationRequestCaptor.getValue();
        BucketLifecycleConfiguration config = actualPutBucketLifecycleConfigurationRequest.lifecycleConfiguration();
        assertEquals(2, config.rules().size());

        // There should be one with id == TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION
        List<LifecycleRule> intRules = config.rules().stream().filter(r -> r.id().equals(TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION)).collect(Collectors.toList());
        assertEquals(1, intRules.size());
        LifecycleRule intRule = intRules.get(0);
        assertEquals(30, intRule.transitions().get(0).days());
        assertEquals(TransitionStorageClass.INTELLIGENT_TIERING.toString(), intRule.transitions().get(0).storageClassAsString());
        assertEquals(ExpirationStatus.ENABLED, intRule.status());
        assertNotNull(intRule.filter());
        assertNull(intRule.filter().and());
        assertNull(intRule.filter().tag());
        assertNull(intRule.filter().prefix());

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

    @Test
    public void testBuildAllBucketsWithTransitionRuleAndExistingRule() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setStorageClassTransitions(Arrays.asList(
                new S3BucketClassTransition()
                        .withStorageClass(TransitionStorageClass.INTELLIGENT_TIERING)
                        .withDays(30)
        ));

        String expectedBucketName = stack + ".bucket";

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        // Mimics an existing life cycle with a transition rule already present
        BucketLifecycleConfiguration expectedBucketLifecycleConfiguration = BucketLifecycleConfiguration.builder()
                .rules(
                        allBucketRuleBuilder(TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).transitions(Transition.builder().storageClass(TransitionStorageClass.INTELLIGENT_TIERING).days(30).build()).build(),
                        allBucketRuleBuilder(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build()).build()
                )
                .build();
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedBucketLifecycleConfiguration.rules()).build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
        verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));

        verify(mockS3Client, never()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));

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

    @Test
    public void testBuildAllBucketsWithTransitionRuleAndExistingRuleWithUpdate() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setStorageClassTransitions(Collections.singletonList(
                new S3BucketClassTransition()
                        .withStorageClass(TransitionStorageClass.INTELLIGENT_TIERING)
                        .withDays(30)
        ));

        String expectedBucketName = stack + ".bucket";

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        // Mimics an existing life cycle with a transition rule already present
        BucketLifecycleConfiguration expectedBucketLifecycleConfiguration = BucketLifecycleConfiguration.builder()
                .rules(
                        allBucketRuleBuilder(TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).transitions(Transition.builder().storageClass(TransitionStorageClass.INTELLIGENT_TIERING).days(35).build()).build(),
                        allBucketRuleBuilder(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build()).build()
                )
                .build();
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedBucketLifecycleConfiguration.rules()).build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
        verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));

        ArgumentCaptor<PutBucketLifecycleConfigurationRequest> putBucketLifecycleConfigurationRequestCaptor = ArgumentCaptor.forClass(PutBucketLifecycleConfigurationRequest.class);
        verify(mockS3Client).putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequestCaptor.capture());
        PutBucketLifecycleConfigurationRequest actualPutBucketLifecycleConfigurationRequest = putBucketLifecycleConfigurationRequestCaptor.getValue();
        BucketLifecycleConfiguration config = actualPutBucketLifecycleConfigurationRequest.lifecycleConfiguration();

        assertEquals(2, config.rules().size());

        // There should be one with id == TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION
        List<LifecycleRule> intRules = config.rules().stream().filter(r -> r.id().equals(TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION)).collect(Collectors.toList());
        assertEquals(1, intRules.size());
        LifecycleRule intRule = intRules.get(0);
        assertEquals(30, intRule.transitions().get(0).days());
        assertEquals(TransitionStorageClass.INTELLIGENT_TIERING.toString(), intRule.transitions().get(0).storageClassAsString());
        assertEquals(ExpirationStatus.ENABLED, intRule.status());
        assertNotNull(intRule.filter());
        assertNull(intRule.filter().and());
        assertNull(intRule.filter().tag());
        assertNull(intRule.filter().prefix());

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

    @Test
    public void testBuildAllBucketsWithTransitionRuleMultiple() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setStorageClassTransitions(Arrays.asList(
                new S3BucketClassTransition()
                        .withStorageClass(TransitionStorageClass.STANDARD_IA)
                        .withDays(15),
                new S3BucketClassTransition()
                        .withStorageClass(TransitionStorageClass.INTELLIGENT_TIERING)
                        .withDays(30),
                new S3BucketClassTransition()
                        .withStorageClass(TransitionStorageClass.DEEP_ARCHIVE)
                        .withDays(90)

        ));

        String expectedBucketName = stack + ".bucket";

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        BucketLifecycleConfiguration expectedBucketLifecycleConfiguration = BucketLifecycleConfiguration.builder()
                .rules(
                        allBucketRuleBuilder(TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).transitions(Transition.builder().storageClass(TransitionStorageClass.INTELLIGENT_TIERING).days(30).build()).build(),
                        allBucketRuleBuilder(TransitionStorageClass.DEEP_ARCHIVE.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).transitions(Transition.builder().storageClass(TransitionStorageClass.INTELLIGENT_TIERING).days(90).build()).build()
                ).build();
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedBucketLifecycleConfiguration.rules()).build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
        verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));

        ArgumentCaptor<PutBucketLifecycleConfigurationRequest> putBucketLifecycleConfigurationRequestCaptor = ArgumentCaptor.forClass(PutBucketLifecycleConfigurationRequest.class);
        verify(mockS3Client).putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequestCaptor.capture());
        PutBucketLifecycleConfigurationRequest actualPutBucketLifecycleConfigurationRequest = putBucketLifecycleConfigurationRequestCaptor.getValue();
        BucketLifecycleConfiguration config = actualPutBucketLifecycleConfigurationRequest.lifecycleConfiguration();
        assertEquals(4, config.rules().size());

        // There should be one with id == TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION
        List<LifecycleRule> intRules = config.rules().stream().filter(r -> r.id().equals(TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION)).collect(Collectors.toList());
        assertEquals(1, intRules.size());
        LifecycleRule intRule = intRules.get(0);
        assertEquals(30, intRule.transitions().get(0).days());
        assertEquals(TransitionStorageClass.INTELLIGENT_TIERING.toString(), intRule.transitions().get(0).storageClassAsString());
        assertEquals(ExpirationStatus.ENABLED, intRule.status());
        assertNotNull(intRule.filter());
        assertNull(intRule.filter().and());
        assertNull(intRule.filter().tag());
        assertNull(intRule.filter().prefix());

        List<LifecycleRule> arcRules = config.rules().stream().filter(r -> r.id().equals(TransitionStorageClass.DEEP_ARCHIVE.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION)).collect(Collectors.toList());
        assertEquals(1, arcRules.size());
        LifecycleRule arcRule = arcRules.get(0);
        assertEquals(90, arcRule.transitions().get(0).days());
        assertEquals(TransitionStorageClass.DEEP_ARCHIVE.toString(), arcRule.transitions().get(0).storageClassAsString());
        assertEquals(ExpirationStatus.ENABLED, arcRule.status());
        assertNotNull(arcRule.filter());
        assertNull(arcRule.filter().and());
        assertNull(arcRule.filter().tag());
        assertNull(arcRule.filter().prefix());

        List<LifecycleRule> iaRules = config.rules().stream().filter(r -> r.id().equals(TransitionStorageClass.STANDARD_IA.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION)).collect(Collectors.toList());
        assertEquals(1, iaRules.size());
        LifecycleRule iaRule = iaRules.get(0);
        assertEquals(15, iaRule.transitions().get(0).days());
        assertEquals(TransitionStorageClass.STANDARD_IA.toString(), iaRule.transitions().get(0).storageClassAsString());
        assertEquals(ExpirationStatus.ENABLED, iaRule.status());
        assertNotNull(iaRule.filter());
        assertNull(iaRule.filter().and());
        assertNull(iaRule.filter().tag());
        assertNull(iaRule.filter().prefix());

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

    @Test
    public void testBuildAllBucketsWithTransitionRuleMultipleWithUpdate() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setStorageClassTransitions(Arrays.asList(
                new S3BucketClassTransition()
                        .withStorageClass(TransitionStorageClass.INTELLIGENT_TIERING)
                        .withDays(30),
                new S3BucketClassTransition()
                        .withStorageClass(TransitionStorageClass.DEEP_ARCHIVE)
                        .withDays(90)

        ));

        String expectedBucketName = stack + ".bucket";

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        BucketLifecycleConfiguration expectedBucketLifecycleConfiguration = BucketLifecycleConfiguration.builder().build();
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedBucketLifecycleConfiguration.rules()).build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
        verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));

        ArgumentCaptor<PutBucketLifecycleConfigurationRequest> putBucketLifecycleConfigurationRequestCaptor = ArgumentCaptor.forClass(PutBucketLifecycleConfigurationRequest.class);
        verify(mockS3Client).putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequestCaptor.capture());
        PutBucketLifecycleConfigurationRequest actualPutBucketLifecycleConfigurationRequest = putBucketLifecycleConfigurationRequestCaptor.getValue();
        BucketLifecycleConfiguration config = actualPutBucketLifecycleConfigurationRequest.lifecycleConfiguration();
        assertEquals(3, config.rules().size());

        // There should be one with id == TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION
        List<LifecycleRule> intRules = config.rules().stream().filter(r -> r.id().equals(TransitionStorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION)).collect(Collectors.toList());
        assertEquals(1, intRules.size());
        LifecycleRule intRule = intRules.get(0);
        assertEquals(30, intRule.transitions().get(0).days());
        assertEquals(TransitionStorageClass.INTELLIGENT_TIERING.toString(), intRule.transitions().get(0).storageClassAsString());
        assertEquals(ExpirationStatus.ENABLED, intRule.status());
        assertNotNull(intRule.filter());
        assertNull(intRule.filter().and());
        assertNull(intRule.filter().tag());
        assertNull(intRule.filter().prefix());

        List<LifecycleRule> arcRules = config.rules().stream().filter(r -> r.id().equals(TransitionStorageClass.DEEP_ARCHIVE.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION)).collect(Collectors.toList());
        assertEquals(1, arcRules.size());
        LifecycleRule arcRule = arcRules.get(0);
        assertEquals(90, arcRule.transitions().get(0).days());
        assertEquals(TransitionStorageClass.DEEP_ARCHIVE.toString(), arcRule.transitions().get(0).storageClassAsString());
        assertEquals(ExpirationStatus.ENABLED, arcRule.status());
        assertNotNull(arcRule.filter());
        assertNull(arcRule.filter().and());
        assertNull(arcRule.filter().tag());
        assertNull(arcRule.filter().prefix());

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

    private LifecycleRule.Builder allBucketRuleBuilder(String ruleName) {
        return LifecycleRule.builder().id(ruleName).filter(LifecycleRuleFilter.builder().build()).status(ExpirationStatus.ENABLED);
    }

}
