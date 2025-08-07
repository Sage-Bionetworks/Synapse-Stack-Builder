package org.sagebionetworks.template.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
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

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
public class S3BucketBuilderImplInventoryTest {

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
    public void testBuildAllBucketsWithInventory() throws InterruptedException {

        S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
        inventoryBucket.setName("${stack}.inventory");

        S3InventoryConfig inventoryConfig = new S3InventoryConfig();
        inventoryConfig.setBucket(inventoryBucket.getName());
        inventoryConfig.setPrefix("prefix");

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setInventoryEnabled(true);

        String expectedInventoryBucketName = stack + ".inventory";
        String expectedBucketName = stack + ".bucket";


        when(mockS3Config.getInventoryConfig()).thenReturn(inventoryConfig);
        when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(inventoryBucket, bucket));

        AwsServiceException notFound = S3Exception.builder().message("NotFound").statusCode(404).build();

        // No inventory configuration set
        doThrow(notFound).when(mockS3Client).getBucketInventoryConfiguration(any(GetBucketInventoryConfigurationRequest.class));

        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        BucketLifecycleConfiguration expectedGetBucketConfiguration = BucketLifecycleConfiguration.builder().build();
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedGetBucketConfiguration.rules()).build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
        verify(mockS3Client).getBucketInventoryConfiguration(argThat((GetBucketInventoryConfigurationRequest req) ->
                    expectedBucketName.equals(req.bucket()) && S3BucketBuilderImpl.INVENTORY_ID.equals(req.id()))
        );
        verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));

        verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

        ArgumentCaptor<PutBucketInventoryConfigurationRequest> putInventoryConfigurationCaptor = ArgumentCaptor.forClass(PutBucketInventoryConfigurationRequest.class);
        verify(mockS3Client).putBucketInventoryConfiguration(putInventoryConfigurationCaptor.capture());
        PutBucketInventoryConfigurationRequest actualPutInventoryConfiguration = putInventoryConfigurationCaptor.getValue();
        assertNotNull(actualPutInventoryConfiguration);
        assertEquals(expectedBucketName, actualPutInventoryConfiguration.bucket());
        InventoryConfiguration config = actualPutInventoryConfiguration.inventoryConfiguration();
        assertEquals(S3BucketBuilderImpl.INVENTORY_ID, config.id());
        assertEquals(S3BucketBuilderImpl.INVENTORY_FIELDS, config.optionalFields());
        assertEquals(InventoryFrequency.WEEKLY, config.schedule().frequency());
        InventoryS3BucketDestination destination = config.destination().s3BucketDestination();
        assertEquals("arn:aws:s3:::" + expectedInventoryBucketName, destination.bucket());
        assertEquals("prefix", destination.prefix());
        assertEquals(accountId, destination.accountId());
        assertEquals(InventoryFormat.PARQUET, destination.format());
        verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(DeleteBucketInventoryConfigurationRequest.class));

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
    public void testBuildAllBucketsWithInventoryAndExisting() throws InterruptedException {

        S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
        inventoryBucket.setName("${stack}.inventory");

        S3InventoryConfig inventoryConfig = new S3InventoryConfig();
        inventoryConfig.setBucket(inventoryBucket.getName());
        inventoryConfig.setPrefix("prefix");

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setInventoryEnabled(true);

        String expectedInventoryBucketName = stack + ".inventory";
        String expectedBucketName = stack + ".bucket";

        when(mockS3Config.getInventoryConfig()).thenReturn(inventoryConfig);
        when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(inventoryBucket, bucket));

        // Mimics an existing configuration that is enabled
        when(mockS3Client.getBucketInventoryConfiguration(any(GetBucketInventoryConfigurationRequest.class)))
                .thenReturn(GetBucketInventoryConfigurationResponse.builder()
                        .inventoryConfiguration(InventoryConfiguration.builder().isEnabled(true).build())
                        .build()
        );

        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        BucketLifecycleConfiguration expectedGetBucketConfiguration = BucketLifecycleConfiguration.builder().build();
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedGetBucketConfiguration.rules()).build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        verify(mockS3Client).createBucket(argThat((CreateBucketRequest req) -> expectedInventoryBucketName.equals(req.bucket())));
        verify(mockS3Client).createBucket(argThat((CreateBucketRequest req) -> expectedBucketName.equals(req.bucket())));

        verify(mockS3Client).getBucketEncryption(argThat((GetBucketEncryptionRequest req) -> expectedInventoryBucketName.equals(req.bucket())));
        verify(mockS3Client).getBucketEncryption(argThat((GetBucketEncryptionRequest req) -> expectedBucketName.equals(req.bucket())));

        verify(mockS3Client).getBucketInventoryConfiguration(argThat((GetBucketInventoryConfigurationRequest req) ->
                expectedBucketName.equals(req.bucket()) && S3BucketBuilderImpl.INVENTORY_ID.equals(req.id()))
        );

        verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));

        ArgumentCaptor<PutBucketInventoryConfigurationRequest> putInventoryConfigurationCaptor = ArgumentCaptor.forClass(PutBucketInventoryConfigurationRequest.class);
        verify(mockS3Client).putBucketInventoryConfiguration(putInventoryConfigurationCaptor.capture());
        PutBucketInventoryConfigurationRequest actualPutInventoryConfiguration = putInventoryConfigurationCaptor.getValue();
        assertNotNull(actualPutInventoryConfiguration);
        assertEquals(expectedBucketName, actualPutInventoryConfiguration.bucket());
        InventoryConfiguration config = actualPutInventoryConfiguration.inventoryConfiguration();

        assertEquals(S3BucketBuilderImpl.INVENTORY_ID, config.id());
        assertEquals(S3BucketBuilderImpl.INVENTORY_FIELDS, config.optionalFields());
        assertEquals(InventoryFrequency.WEEKLY, config.schedule().frequency());

        InventoryS3BucketDestination destination = config.destination().s3BucketDestination();

        assertEquals("arn:aws:s3:::" + expectedInventoryBucketName, destination.bucket());
        assertEquals("prefix", destination.prefix());
        assertEquals(accountId, destination.accountId());
        assertEquals(InventoryFormat.PARQUET, destination.format());
        verify(mockS3Client, never()).deleteBucketInventoryConfiguration(argThat((DeleteBucketInventoryConfigurationRequest req) ->
            expectedBucketName.equals(req.bucket()) && S3BucketBuilderImpl.INVENTORY_ID.equals(req.id())
        ));

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
    public void testBuildAllBucketsWithDisabledInventoryAndNonExisting() throws InterruptedException {

        S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
        inventoryBucket.setName("${stack}.inventory");

        S3InventoryConfig inventoryConfig = new S3InventoryConfig();
        inventoryConfig.setBucket(inventoryBucket.getName());
        inventoryConfig.setPrefix("prefix");

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setInventoryEnabled(false);

        String expectedInventoryBucketName = stack + ".inventory";
        String expectedBucketName = stack + ".bucket";

        when(mockS3Config.getInventoryConfig()).thenReturn(inventoryConfig);
        when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(inventoryBucket, bucket));

        AwsServiceException notFound = S3Exception.builder().message("NotFound").statusCode(404).build();

        // No inventory configuration set
        doThrow(notFound).when(mockS3Client).getBucketInventoryConfiguration(any(GetBucketInventoryConfigurationRequest.class));

        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        BucketLifecycleConfiguration expectedGetBucketConfiguration = BucketLifecycleConfiguration.builder().build();
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedGetBucketConfiguration.rules()).build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client).getBucketInventoryConfiguration(argThat((GetBucketInventoryConfigurationRequest req) ->
                expectedBucketName.equals(req.bucket()) && S3BucketBuilderImpl.INVENTORY_ID.equals(req.id()))
        );

        verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
        verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));

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
    public void testBuildAllBucketsWithDisabledInventoryAndExisting() throws InterruptedException {

        S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
        inventoryBucket.setName("${stack}.inventory");

        S3InventoryConfig inventoryConfig = new S3InventoryConfig();
        inventoryConfig.setBucket(inventoryBucket.getName());
        inventoryConfig.setPrefix("prefix");

        S3BucketDescriptor bucket = new S3BucketDescriptor();
        bucket.setName("${stack}.bucket");
        bucket.setInventoryEnabled(false);

        String expectedInventoryBucketName = stack + ".inventory";
        String expectedBucketName = stack + ".bucket";

        when(mockS3Config.getInventoryConfig()).thenReturn(inventoryConfig);
        when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(inventoryBucket, bucket));

        // Mimics an existing configuration that is enabled
        when(mockS3Client.getBucketInventoryConfiguration(any(GetBucketInventoryConfigurationRequest.class))).thenReturn(
                GetBucketInventoryConfigurationResponse.builder()
                        .inventoryConfiguration(
                                InventoryConfiguration.builder()
                                        .isEnabled(true)
                                        .build())
                        .build());

        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        BucketLifecycleConfiguration expectedGetBucketConfiguration = BucketLifecycleConfiguration.builder().build();
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedGetBucketConfiguration.rules()).build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());

        verify(mockS3Client).getBucketInventoryConfiguration(argThat((GetBucketInventoryConfigurationRequest req) ->
                expectedBucketName.equals(req.bucket()) && S3BucketBuilderImpl.INVENTORY_ID.equals(req.id()))
        );

        verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
        verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
        verify(mockS3Client).deleteBucketInventoryConfiguration(argThat((DeleteBucketInventoryConfigurationRequest req) ->
                expectedBucketName.equals(req.bucket()) && S3BucketBuilderImpl.INVENTORY_ID.equals(req.id())
        ));

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
