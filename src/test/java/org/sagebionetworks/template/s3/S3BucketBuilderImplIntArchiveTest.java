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

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
public class S3BucketBuilderImplIntArchiveTest {

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
    public void testBuildAllBucketsWithIntArchiveConfiguration() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
                .withArchiveAccessDays(90)
                .withDeepArchiveAccessDays(180)
                .withTagFilter(new S3TagFilter().withName("test").withValue("tag"))
        );

        String expectedBucketName = stack + ".bucket";

        AwsServiceException notFound = S3Exception.builder()
                .message("Not Found")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchConfiguration").build())
                .statusCode(404)
                .build();

        when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
        doThrow(notFound).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(GetBucketIntelligentTieringConfigurationRequest.class));

        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        // TODO: consider simplifying with argThat()
        GetBucketIntelligentTieringConfigurationRequest expectedGetBucketIntelligentTieringConfiguration = GetBucketIntelligentTieringConfigurationRequest.builder().bucket(expectedBucketName).id( S3BucketBuilderImpl.INT_ARCHIVE_ID).build();
        ArgumentCaptor<PutBucketIntelligentTieringConfigurationRequest> putIntTieringConfigurationReqCaptor = ArgumentCaptor.forClass(PutBucketIntelligentTieringConfigurationRequest.class);
        verify(mockS3Client).putBucketIntelligentTieringConfiguration(putIntTieringConfigurationReqCaptor.capture());
        PutBucketIntelligentTieringConfigurationRequest actualPutIntTieringConfigurationReq = putIntTieringConfigurationReqCaptor.getValue();
        IntelligentTieringConfiguration config = actualPutIntTieringConfigurationReq.intelligentTieringConfiguration();
        assertEquals(expectedBucketName, actualPutIntTieringConfigurationReq.bucket());
        assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, actualPutIntTieringConfigurationReq.id());

        assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, config.id());
        assertEquals(Arrays.asList(
                Tiering.builder()
                        .days(90)
                        .accessTier(IntelligentTieringAccessTier.ARCHIVE_ACCESS)
                        .build(),
                Tiering.builder()
                        .days(180)
                        .accessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS)
                        .build()
        ), config.tierings());

        Tag tag = config.filter().tag();

        assertEquals("test",  tag.key());
        assertEquals("tag",  tag.value());

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
    public void testBuildAllBucketsWithIntArchiveConfigurationAndOtherAmazonExceptionStatusCode() {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
                .withArchiveAccessDays(90)
                .withDeepArchiveAccessDays(180)
                .withTagFilter(new S3TagFilter().withName("test").withValue("tag"))
        );

        String expectedBucketName = stack + ".bucket";

        AwsServiceException anotherEx = S3Exception.builder()
                .message("Not Found")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchConfiguration").build())
                .statusCode(503) //???
                .build();

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        doThrow(anotherEx).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(GetBucketIntelligentTieringConfigurationRequest.class));

        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        AwsServiceException ex = assertThrows(S3Exception.class, () -> {
            // Call under test
            builder.buildAllBuckets();
        });

        assertEquals(anotherEx, ex);

        ArgumentCaptor<GetBucketIntelligentTieringConfigurationRequest> getBucketIntelligentTieringConfigurationRequestCaptor = ArgumentCaptor.forClass(GetBucketIntelligentTieringConfigurationRequest.class);
        verify(mockS3Client).getBucketIntelligentTieringConfiguration(getBucketIntelligentTieringConfigurationRequestCaptor.capture());
        GetBucketIntelligentTieringConfigurationRequest actualGetBucketIntelligentTieringConfigurationRequest = getBucketIntelligentTieringConfigurationRequestCaptor.getValue();
        assertEquals(expectedBucketName, actualGetBucketIntelligentTieringConfigurationRequest.bucket());
        assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, actualGetBucketIntelligentTieringConfigurationRequest.id());
        verify(mockS3Client, never()).putBucketIntelligentTieringConfiguration(any(PutBucketIntelligentTieringConfigurationRequest.class));

    }

    @Test
    public void testBuildAllBucketsWithIntArchiveConfigurationAndOtherAmazonExceptionErrorCode() {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
                .withArchiveAccessDays(90)
                .withDeepArchiveAccessDays(180)
                .withTagFilter(new S3TagFilter().withName("test").withValue("tag"))
        );

        String expectedBucketName = stack + ".bucket";

        AwsServiceException anotherEx = S3Exception.builder()
                .message("Not Found")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchBucket").build())
                .statusCode(404)
                .build();

        when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
        doThrow(anotherEx).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(GetBucketIntelligentTieringConfigurationRequest.class));

        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        AwsServiceException ex = assertThrows(S3Exception.class, () -> {
            // Call under test
            builder.buildAllBuckets();
        });

        assertEquals(anotherEx, ex);

        ArgumentCaptor<GetBucketIntelligentTieringConfigurationRequest> getBucketIntelligentTieringConfigurationRequestCaptor = ArgumentCaptor.forClass(GetBucketIntelligentTieringConfigurationRequest.class);
        verify(mockS3Client).getBucketIntelligentTieringConfiguration(getBucketIntelligentTieringConfigurationRequestCaptor.capture());
        GetBucketIntelligentTieringConfigurationRequest actualGetBucketIntelligentTieringConfigurationRequest = getBucketIntelligentTieringConfigurationRequestCaptor.getValue();
        assertEquals(expectedBucketName, actualGetBucketIntelligentTieringConfigurationRequest.bucket());
        assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, actualGetBucketIntelligentTieringConfigurationRequest.id());
        verify(mockS3Client, never()).putBucketIntelligentTieringConfiguration(any(PutBucketIntelligentTieringConfigurationRequest.class));

    }

    @Test
    public void testBuildAllBucketsWithIntArchiveConfigurationAndNotTagFilter() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
                .withArchiveAccessDays(90)
                .withDeepArchiveAccessDays(180)
        );

        String expectedBucketName = stack + ".bucket";

        AwsServiceException notFound = S3Exception.builder()
                .message("Not Found")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchConfiguration").build())
                .statusCode(404)
                .build();

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        doThrow(notFound).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(GetBucketIntelligentTieringConfigurationRequest.class));
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any(String.class))).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        ArgumentCaptor<GetBucketIntelligentTieringConfigurationRequest> getBucketIntelligentTieringConfigurationRequestCaptor = ArgumentCaptor.forClass(GetBucketIntelligentTieringConfigurationRequest.class);
        verify(mockS3Client).getBucketIntelligentTieringConfiguration(getBucketIntelligentTieringConfigurationRequestCaptor.capture());
        GetBucketIntelligentTieringConfigurationRequest actualGetBucketIntelligentTieringConfigurationRequest = getBucketIntelligentTieringConfigurationRequestCaptor.getValue();
        assertEquals(expectedBucketName, actualGetBucketIntelligentTieringConfigurationRequest.bucket());
        assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, actualGetBucketIntelligentTieringConfigurationRequest.id());

        ArgumentCaptor<PutBucketIntelligentTieringConfigurationRequest> putIntTieringConfigurationReqCaptor = ArgumentCaptor.forClass(PutBucketIntelligentTieringConfigurationRequest.class);
        verify(mockS3Client).putBucketIntelligentTieringConfiguration(putIntTieringConfigurationReqCaptor.capture());
        PutBucketIntelligentTieringConfigurationRequest actualPutIntTieringConfigurationReq = putIntTieringConfigurationReqCaptor.getValue();
        assertEquals(expectedBucketName, actualPutIntTieringConfigurationReq.bucket());
        assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, actualPutIntTieringConfigurationReq.id());

        assertEquals(Arrays.asList(
                Tiering.builder()
                        .days(90)
                        .accessTier(IntelligentTieringAccessTier.ARCHIVE_ACCESS)
                        .build(),
                Tiering.builder()
                        .days(180)
                        .accessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS)
                        .build()
        ), actualPutIntTieringConfigurationReq.intelligentTieringConfiguration().tierings());

        assertNull(actualPutIntTieringConfigurationReq.intelligentTieringConfiguration().filter().and());

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
    public void testBuildAllBucketsWithIntArchiveConfigurationAndSingleTier() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
                .withDeepArchiveAccessDays(180)
        );

        String expectedBucketName = stack + ".bucket";

        AwsServiceException notFound = S3Exception.builder()
                .message("Not Found")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchConfiguration").build())
                .statusCode(404)
                .build();

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        doThrow(notFound).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(GetBucketIntelligentTieringConfigurationRequest.class));
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        ArgumentCaptor<GetBucketIntelligentTieringConfigurationRequest> getBucketIntelligentTieringConfigurationRequestCaptor = ArgumentCaptor.forClass(GetBucketIntelligentTieringConfigurationRequest.class);
        ArgumentCaptor<PutBucketIntelligentTieringConfigurationRequest> putIntTieringConfigurationReqCaptor = ArgumentCaptor.forClass(PutBucketIntelligentTieringConfigurationRequest.class);
        verify(mockS3Client).getBucketIntelligentTieringConfiguration(getBucketIntelligentTieringConfigurationRequestCaptor.capture());
        GetBucketIntelligentTieringConfigurationRequest actualGetBucketIntelligentTieringConfigurationRequest = getBucketIntelligentTieringConfigurationRequestCaptor.getValue();
        assertEquals(expectedBucketName, actualGetBucketIntelligentTieringConfigurationRequest.bucket());
        assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, actualGetBucketIntelligentTieringConfigurationRequest.id());
        verify(mockS3Client).putBucketIntelligentTieringConfiguration(putIntTieringConfigurationReqCaptor.capture());
        PutBucketIntelligentTieringConfigurationRequest actualPutIntTieringConfigurationReq = putIntTieringConfigurationReqCaptor.getValue();
        assertEquals(expectedBucketName, actualPutIntTieringConfigurationReq.bucket());
        assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, actualPutIntTieringConfigurationReq.id());
        
        assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, actualPutIntTieringConfigurationReq.id());

        assertEquals(Collections.singletonList(
                Tiering.builder()
                        .days(180)
                        .accessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS)
                        .build()
        ), actualPutIntTieringConfigurationReq.intelligentTieringConfiguration().tierings());

        assertNull(actualPutIntTieringConfigurationReq.intelligentTieringConfiguration().filter().and());

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
    public void testBuildAllBucketsWithIntArchiveConfigurationAndExisting() throws InterruptedException {

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
                .withArchiveAccessDays(90)
                .withDeepArchiveAccessDays(180)
                .withTagFilter(new S3TagFilter().withName("test").withValue("tag"))
        );

        String expectedBucketName = stack + ".bucket";

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        GetBucketIntelligentTieringConfigurationResponse expectedGetBucketIntelligentTieringConfigurationResponse = GetBucketIntelligentTieringConfigurationResponse.builder()
                .intelligentTieringConfiguration(IntelligentTieringConfiguration.builder().build())
                .build();
        when(mockS3Client.getBucketIntelligentTieringConfiguration(any(GetBucketIntelligentTieringConfigurationRequest.class))).thenReturn(expectedGetBucketIntelligentTieringConfigurationResponse);
        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        GetBucketIntelligentTieringConfigurationRequest expectedGetBucketIntelligentTieringConfiguration = GetBucketIntelligentTieringConfigurationRequest.builder()
                .bucket(expectedBucketName)
                .id( S3BucketBuilderImpl.INT_ARCHIVE_ID)
                .build();
        verify(mockS3Client).getBucketIntelligentTieringConfiguration(expectedGetBucketIntelligentTieringConfiguration);
        verify(mockS3Client, never()).putBucketIntelligentTieringConfiguration(any(PutBucketIntelligentTieringConfigurationRequest.class));

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
