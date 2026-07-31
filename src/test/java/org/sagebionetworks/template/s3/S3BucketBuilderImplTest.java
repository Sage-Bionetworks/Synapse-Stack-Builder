package org.sagebionetworks.template.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortIncompleteMultipartUpload;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.Event;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketIntelligentTieringConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketIntelligentTieringConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3.model.IntelligentTieringAccessTier;
import software.amazon.awssdk.services.s3.model.IntelligentTieringConfiguration;
import software.amazon.awssdk.services.s3.model.InventoryConfiguration;
import software.amazon.awssdk.services.s3.model.InventoryFrequency;
import software.amazon.awssdk.services.s3.model.InventoryS3BucketDestination;
import software.amazon.awssdk.services.s3.model.LifecycleExpiration;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.PutBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.PutBucketIntelligentTieringConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.QueueConfiguration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionRule;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tiering;
import software.amazon.awssdk.services.s3.model.TopicConfiguration;
import software.amazon.awssdk.services.s3.model.Transition;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
public class S3BucketBuilderImplTest {

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
	private ArgumentCaptor<PutBucketInventoryConfigurationRequest> inventoryConfigurationCaptor;
	
	@Captor
	private ArgumentCaptor<PutBucketLifecycleConfigurationRequest> bucketLifeCycleConfigurationCaptor;
	
	@Captor
	private ArgumentCaptor<VelocityContext> velocityContextCaptor;
	
	@Captor
	private ArgumentCaptor<PutBucketIntelligentTieringConfigurationRequest> intConfigurationCaptor;

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
	public void testBuildAllBuckets() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");

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

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getPublicAccessBlock(GetPublicAccessBlockRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).putPublicAccessBlock(PutPublicAccessBlockRequest.builder().bucket(expectedBucketName)
			.publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
				.blockPublicAcls(true)
				.blockPublicPolicy(true)
				.ignorePublicAcls(true)
				.restrictPublicBuckets(true)
				.build()
			)
			.build()
		);
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).putBucketLifecycleConfiguration(bucketLifeCycleConfigurationCaptor.capture());
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
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue().lifecycleConfiguration();
		
		assertEquals(1, config.rules().size());

		LifecycleRule rule = config.rules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS, rule.id());
		assertEquals(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS, rule.abortIncompleteMultipartUpload().daysAfterInitiation().intValue());
		assertEquals(ExpirationStatus.ENABLED, rule.status());
		assertNull(rule.prefix());
		assertNotNull(rule.filter());
		assertNull(rule.filter().prefix());

		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(DeleteBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).putBucketPolicy(any(PutBucketPolicyRequest.class));

	}
	
	@Test
	public void testBuildAllBucketsWithExistingAbortMultipartRule() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");

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
		
		// Mimics an existing life cycle with the abort rule already present
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder()
			.rules(
					allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build()).build()
			).build());

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(DeleteBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).putBucketPolicy(any(PutBucketPolicyRequest.class));
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
	public void testBuildAllBucketsWithExistingAbortMultipartRuleAndUpdate() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");

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
		
		// Mimics an existing life cycle with the abort rule already present
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder()
			.rules(
					allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS - 1).build()).build()
			).build());

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client).putBucketLifecycleConfiguration(bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue().lifecycleConfiguration();
		
		assertEquals(1, config.rules().size());

		LifecycleRule rule = config.rules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS, rule.id());
		assertEquals(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS, rule.abortIncompleteMultipartUpload().daysAfterInitiation().intValue());
		assertEquals(ExpirationStatus.ENABLED, rule.status());
		assertNull(rule.prefix());
		assertNotNull(rule.filter());
		assertNull(rule.filter().prefix());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(DeleteBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).putBucketPolicy(any(PutBucketPolicyRequest.class));

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
	public void testBuildAllBucketsNeedsEncypted() throws InterruptedException {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

		doAnswer(invocation -> {
			((StringWriter) invocation.getArgument(1)).append("{}");
			return null;
		}).when(mockTemplate).merge(any(), any());

		Stack bucketPolicyStack = Stack.builder().build();

		when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
		when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

		S3Exception notFound = (S3Exception) S3Exception.builder().statusCode(404).message("NotFound").build();

		doThrow(notFound).when(mockS3Client).getBucketEncryption(any(GetBucketEncryptionRequest.class));

		String expectedBucketName = stack + ".bucket";

		// call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).putBucketEncryption(encryptionRequestCaptor.capture());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

		PutBucketEncryptionRequest request = encryptionRequestCaptor.getValue();

		assertNotNull(request);
		assertEquals(expectedBucketName, request.bucket());
		assertNotNull(request.serverSideEncryptionConfiguration());
		assertNotNull(request.serverSideEncryptionConfiguration().rules());
		assertEquals(1, request.serverSideEncryptionConfiguration().rules().size());
		ServerSideEncryptionRule rule = request.serverSideEncryptionConfiguration().rules().get(0);
		assertNotNull(rule.applyServerSideEncryptionByDefault());
		assertEquals(ServerSideEncryption.AES256.toString(), rule.applyServerSideEncryptionByDefault().sseAlgorithmAsString());
		
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(DeleteBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).putBucketPolicy(any(PutBucketPolicyRequest.class));

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
	public void testBuildAllBucketsBadName() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		// bad name
		bucket.setName("${stack}.${instance}.one");
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			builder.buildAllBuckets();
		});

		verifyNoMoreInteractions(mockS3Client);

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
		
		S3Exception notFound = (S3Exception) S3Exception.builder().statusCode(404).message("NotFound").build();

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
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedInventoryBucketName).build());
		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedInventoryBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client).getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INVENTORY_ID).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

		verify(mockS3Client).putBucketInventoryConfiguration(inventoryConfigurationCaptor.capture());

		PutBucketInventoryConfigurationRequest inventoryRequest = inventoryConfigurationCaptor.getValue();

		assertEquals(expectedBucketName, inventoryRequest.bucket());
		assertEquals(S3BucketBuilderImpl.INVENTORY_ID, inventoryRequest.id());

		InventoryConfiguration config = inventoryRequest.inventoryConfiguration();

		assertEquals(S3BucketBuilderImpl.INVENTORY_ID, config.id());
		assertEquals(S3BucketBuilderImpl.INVENTORY_FIELDS, config.optionalFieldsAsStrings());
		assertEquals(InventoryFrequency.WEEKLY.toString(), config.schedule().frequencyAsString());

		InventoryS3BucketDestination destination = config.destination().s3BucketDestination();

		assertEquals("arn:aws:s3:::" + expectedInventoryBucketName, destination.bucket());
		assertEquals("prefix", destination.prefix());
		assertEquals(accountId, destination.accountId());
		assertEquals(S3BucketBuilderImpl.INVENTORY_FORMAT, destination.formatAsString());
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
		when(mockS3Client.getBucketInventoryConfiguration(any(GetBucketInventoryConfigurationRequest.class))).thenReturn(
				GetBucketInventoryConfigurationResponse.builder().inventoryConfiguration(
						InventoryConfiguration.builder().isEnabled(true).build()
				).build()
		);
		
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

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedInventoryBucketName).build());
		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedInventoryBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client).getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INVENTORY_ID).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));

		verify(mockS3Client).putBucketInventoryConfiguration(inventoryConfigurationCaptor.capture());

		PutBucketInventoryConfigurationRequest inventoryRequest = inventoryConfigurationCaptor.getValue();

		assertEquals(expectedBucketName, inventoryRequest.bucket());
		assertEquals(S3BucketBuilderImpl.INVENTORY_ID, inventoryRequest.id());

		InventoryConfiguration config = inventoryRequest.inventoryConfiguration();

		assertEquals(S3BucketBuilderImpl.INVENTORY_ID, config.id());
		assertEquals(S3BucketBuilderImpl.INVENTORY_FIELDS, config.optionalFieldsAsStrings());
		assertEquals(InventoryFrequency.WEEKLY.toString(), config.schedule().frequencyAsString());

		InventoryS3BucketDestination destination = config.destination().s3BucketDestination();

		assertEquals("arn:aws:s3:::" + expectedInventoryBucketName, destination.bucket());
		assertEquals("prefix", destination.prefix());
		assertEquals(accountId, destination.accountId());
		assertEquals(S3BucketBuilderImpl.INVENTORY_FORMAT, destination.formatAsString());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INVENTORY_ID).build());

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
		
		S3Exception notFound = (S3Exception) S3Exception.builder().statusCode(404).message("NotFound").build();

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
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedInventoryBucketName).build());
		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedInventoryBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client).getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INVENTORY_ID).build());
		
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
				GetBucketInventoryConfigurationResponse.builder().inventoryConfiguration(
						InventoryConfiguration.builder().isEnabled(true).build()
				).build()
		);

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

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedInventoryBucketName).build());
		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedInventoryBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client).getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INVENTORY_ID).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		verify(mockS3Client).deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INVENTORY_ID).build());

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
	public void testBuildAllBucketsWithRetentionDays() throws InterruptedException {

		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setRetentionDays(30);
		
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
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		
		verify(mockS3Client).putBucketLifecycleConfiguration(bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue().lifecycleConfiguration();
		
		assertEquals(2, config.rules().size());
		
		LifecycleRule rule = config.rules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_RETENTION, rule.id());
		assertEquals(bucket.getRetentionDays(), rule.expiration().days());
		assertEquals(ExpirationStatus.ENABLED, rule.status());
		assertNull(rule.prefix());
		assertNotNull(rule.filter());
		assertNull(rule.filter().prefix());

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
	public void testBuildAllBucketsWithRetentionDaysAndExistingRule() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setRetentionDays(30);
		
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
		
		// Mimics an existing life cycle with a retention rule already present
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder()
			.rules(
				allBucketRule(StorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).transitions(Transition.builder().storageClass(StorageClass.INTELLIGENT_TIERING.toString()).days(30).build()).build(),
				allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build()).build(),
				allBucketRule(S3BucketBuilderImpl.RULE_ID_RETENTION).expiration(LifecycleExpiration.builder().days(30).build()).build()
			).build()
		);
		
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
	public void testBuildAllBucketsWithRetentionDaysAndExistingRuleWithUpdate() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setRetentionDays(30);
		
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
		
		// Mimics an existing life cycle with a retention rule already present
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder()
			.rules(
				allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build()).build(),
				allBucketRule(S3BucketBuilderImpl.RULE_ID_RETENTION).expiration(LifecycleExpiration.builder().days(45).build()).build()
			).build()
		);
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		
		verify(mockS3Client).putBucketLifecycleConfiguration(bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue().lifecycleConfiguration();
		
		assertEquals(2, config.rules().size());
		
		LifecycleRule rule = config.rules().get(1);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_RETENTION, rule.id());
		assertEquals(bucket.getRetentionDays(), rule.expiration().days());
		assertEquals(ExpirationStatus.ENABLED, rule.status());
		assertNull(rule.prefix());
		assertNotNull(rule.filter());
		assertNull(rule.filter().prefix());

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
	public void testBuildAllBucketsWithNoRetentionDaysAndExistingRetentionRule() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		// No retention days set (null)
		
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
		
		// Mimics an existing life cycle with a retention rule already present that should be removed
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder()
			.rules(
				allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build()).build(),
				allBucketRule(S3BucketBuilderImpl.RULE_ID_RETENTION).expiration(LifecycleExpiration.builder().days(30).build()).build()
			).build()
		);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		
		verify(mockS3Client).putBucketLifecycleConfiguration(bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue().lifecycleConfiguration();
		
		// Should only have 1 rule now (abort multipart), retention rule should be removed
		assertEquals(1, config.rules().size());
		
		LifecycleRule rule = config.rules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS, rule.id());

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
	public void testBuildAllBucketsWithNoRetentionDaysAndNoExistingRetentionRule() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		// No retention days set (null)
		
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
		
		// Mimics an existing life cycle with no retention rule present
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder()
			.rules(
				allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build()).build()
			).build()
		);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		
		// Should not update lifecycle configuration since no changes are needed
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
	public void testBuildAllBucketsWithTransitionRule() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setStorageClassTransitions(Arrays.asList(
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.INTELLIGENT_TIERING)
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
				
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		
		verify(mockS3Client).putBucketLifecycleConfiguration(bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue().lifecycleConfiguration();
		
		assertEquals(2, config.rules().size());

		LifecycleRule rule = config.rules().get(0);
		
		assertEquals(1, rule.transitions().size());
		
		assertEquals(StorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, rule.id());
		assertEquals(30, rule.transitions().get(0).days().intValue());
		assertEquals(StorageClass.INTELLIGENT_TIERING.toString(), rule.transitions().get(0).storageClassAsString());
		assertEquals(ExpirationStatus.ENABLED, rule.status());
		assertNull(rule.prefix());
		assertNotNull(rule.filter());
		assertNull(rule.filter().prefix());

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
					.withStorageClass(StorageClass.INTELLIGENT_TIERING)
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
		
		// Mimics an existing life cycle with a transition rule already present
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder()
				.rules(
						allBucketRule(StorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).transitions(Transition.builder().storageClass(StorageClass.INTELLIGENT_TIERING.toString()).days(30).build()).build(),
						allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build()).build()
				).build());
				
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
		bucket.setStorageClassTransitions(Arrays.asList(
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.INTELLIGENT_TIERING)
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
		
		// Mimics an existing life cycle with a transition rule already present
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder()
				.rules(
						allBucketRule(StorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).transitions(Transition.builder().storageClass(StorageClass.INTELLIGENT_TIERING.toString()).days(35).build()).build(),
						allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build()).build()
				).build());
				
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		
		verify(mockS3Client).putBucketLifecycleConfiguration(bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue().lifecycleConfiguration();
		
		assertEquals(2, config.rules().size());
		
		LifecycleRule intRule = config.rules().get(0);
		
		assertEquals(StorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, intRule.id());
		assertEquals(30, intRule.transitions().get(0).days().intValue());
		assertEquals(StorageClass.INTELLIGENT_TIERING.toString(), intRule.transitions().get(0).storageClassAsString());
		assertNull(intRule.prefix());
		assertNotNull(intRule.filter());
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
					.withStorageClass(StorageClass.STANDARD_IA)
					.withDays(15),
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.INTELLIGENT_TIERING)
					.withDays(30),
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.DEEP_ARCHIVE)
					.withDays(90)
					
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
		
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder()
			.rules(
					// The infrequent access is not there
					// The intelligent tiering should be updated
					allBucketRule(StorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).transitions(Transition.builder().storageClass(StorageClass.INTELLIGENT_TIERING.toString()).days(35).build()).build(),
					// This is the same
					allBucketRule(StorageClass.DEEP_ARCHIVE.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).transitions(Transition.builder().storageClass(StorageClass.DEEP_ARCHIVE.toString()).days(90).build()).build()
			).build());
				
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		
		verify(mockS3Client).putBucketLifecycleConfiguration(bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue().lifecycleConfiguration();
		
		assertEquals(4, config.rules().size());
		
		LifecycleRule intRule = config.rules().get(0);
		
		assertEquals(StorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, intRule.id());
		assertEquals(30, intRule.transitions().get(0).days().intValue());
		assertEquals(StorageClass.INTELLIGENT_TIERING.toString(), intRule.transitions().get(0).storageClassAsString());
		assertEquals(ExpirationStatus.ENABLED, intRule.status());
		assertNull(intRule.prefix());
		assertNotNull(intRule.filter());
		assertNull(intRule.filter().prefix());
		
		LifecycleRule arcRule = config.rules().get(1);
		
		assertEquals(StorageClass.DEEP_ARCHIVE.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, arcRule.id());
		assertEquals(90, arcRule.transitions().get(0).days().intValue());
		assertEquals(StorageClass.DEEP_ARCHIVE.toString(), arcRule.transitions().get(0).storageClassAsString());
		assertEquals(ExpirationStatus.ENABLED, arcRule.status());
		assertNull(arcRule.prefix());
		assertNotNull(arcRule.filter());
		assertNull(arcRule.filter().prefix());
		
		LifecycleRule iaRule = config.rules().get(2);
		
		assertEquals(StorageClass.STANDARD_IA.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, iaRule.id());
		assertEquals(15, iaRule.transitions().get(0).days().intValue());
		assertEquals(StorageClass.STANDARD_IA.toString(), iaRule.transitions().get(0).storageClassAsString());
		assertEquals(ExpirationStatus.ENABLED, iaRule.status());
		assertNull(iaRule.prefix());
		assertNotNull(iaRule.filter());
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
					.withStorageClass(StorageClass.INTELLIGENT_TIERING)
					.withDays(30),
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.DEEP_ARCHIVE)
					.withDays(90)
					
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
				
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		
		verify(mockS3Client).putBucketLifecycleConfiguration(bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue().lifecycleConfiguration();
		
		assertEquals(3, config.rules().size());
		
		LifecycleRule intRule = config.rules().get(0);
		
		assertEquals(StorageClass.INTELLIGENT_TIERING.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, intRule.id());
		assertEquals(30, intRule.transitions().get(0).days().intValue());
		assertEquals(StorageClass.INTELLIGENT_TIERING.toString(), intRule.transitions().get(0).storageClassAsString());
		assertEquals(ExpirationStatus.ENABLED, intRule.status());
		assertNull(intRule.prefix());
		assertNotNull(intRule.filter());
		assertNull(intRule.filter().prefix());
		
		LifecycleRule arcRule = config.rules().get(1);
		
		assertEquals(StorageClass.DEEP_ARCHIVE.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, arcRule.id());
		assertEquals(90, arcRule.transitions().get(0).days().intValue());
		assertEquals(StorageClass.DEEP_ARCHIVE.toString(), arcRule.transitions().get(0).storageClassAsString());
		assertEquals(ExpirationStatus.ENABLED, arcRule.status());
		assertNull(arcRule.prefix());
		assertNotNull(arcRule.filter());
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
	
	@Test
	public void testBuildAllBucketsWithDevOnly() throws InterruptedException {
		
		stack = "someStackOtherThanProd";
		
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setDevOnly(true);

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

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(DeleteBucketInventoryConfigurationRequest.class));
		verify(mockS3Client, never()).putBucketPolicy(any(PutBucketPolicyRequest.class));

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
	public void testBuildAllBucketsWithDevAndProd() throws InterruptedException {
		
		stack = "prod";
		
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setDevOnly(true);

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
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
		
		verifyNoMoreInteractions(mockS3Client);

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
	public void testBuildAllBucketsWithIntArchiveConfiguration() throws InterruptedException {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
				.withArchiveAccessDays(90)
				.withDeepArchiveAccessDays(180)
				.withTagFilter(new S3TagFilter().withName("test").withValue("tag"))
		);
		
		String expectedBucketName = stack + ".bucket";
		
		S3Exception notFound = (S3Exception) S3Exception.builder().statusCode(404).message("Not Found")
				.awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchConfiguration").build()).build();
		
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
				
		// Call under test
		builder.buildAllBuckets();
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INT_ARCHIVE_ID).build());
		verify(mockS3Client).putBucketIntelligentTieringConfiguration(intConfigurationCaptor.capture());

		PutBucketIntelligentTieringConfigurationRequest intRequest = intConfigurationCaptor.getValue();

		assertEquals(expectedBucketName, intRequest.bucket());
		assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, intRequest.id());

		IntelligentTieringConfiguration config = intRequest.intelligentTieringConfiguration();

		assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, config.id());
		assertEquals(Arrays.asList(
				Tiering.builder().days(90).accessTier(IntelligentTieringAccessTier.ARCHIVE_ACCESS).build(),
				Tiering.builder().days(180).accessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS).build()
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
		
		S3Exception anotherEx = (S3Exception) S3Exception.builder().statusCode(503).message("Not Found")
				.awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchConfiguration").build()).build();
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		doThrow(anotherEx).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(GetBucketIntelligentTieringConfigurationRequest.class));
				
		S3Exception ex = assertThrows(S3Exception.class, () -> {
			// Call under test
			builder.buildAllBuckets();
		});
		
		assertEquals(anotherEx, ex);
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INT_ARCHIVE_ID).build());
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
		
		S3Exception anotherEx = (S3Exception) S3Exception.builder().statusCode(404).message("Not Found")
				.awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchBucket").build()).build();
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		doThrow(anotherEx).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(GetBucketIntelligentTieringConfigurationRequest.class));
				
		S3Exception ex = assertThrows(S3Exception.class, () -> {
			// Call under test
			builder.buildAllBuckets();
		});
		
		assertEquals(anotherEx, ex);
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INT_ARCHIVE_ID).build());
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
		
		S3Exception notFound = (S3Exception) S3Exception.builder().statusCode(404).message("Not Found")
				.awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchConfiguration").build()).build();
		
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
				
		// Call under test
		builder.buildAllBuckets();
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INT_ARCHIVE_ID).build());
		verify(mockS3Client).putBucketIntelligentTieringConfiguration(intConfigurationCaptor.capture());

		PutBucketIntelligentTieringConfigurationRequest intRequest = intConfigurationCaptor.getValue();

		assertEquals(expectedBucketName, intRequest.bucket());
		assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, intRequest.id());

		IntelligentTieringConfiguration config = intRequest.intelligentTieringConfiguration();

		assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, config.id());
		assertEquals(Arrays.asList(
				Tiering.builder().days(90).accessTier(IntelligentTieringAccessTier.ARCHIVE_ACCESS).build(),
				Tiering.builder().days(180).accessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS).build()
		), config.tierings());
		
		assertNull(config.filter().tag());

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
		
		S3Exception notFound = (S3Exception) S3Exception.builder().statusCode(404).message("Not Found")
				.awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchConfiguration").build()).build();
		
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
				
		// Call under test
		builder.buildAllBuckets();
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INT_ARCHIVE_ID).build());
		verify(mockS3Client).putBucketIntelligentTieringConfiguration(intConfigurationCaptor.capture());

		PutBucketIntelligentTieringConfigurationRequest intRequest = intConfigurationCaptor.getValue();

		assertEquals(expectedBucketName, intRequest.bucket());
		assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, intRequest.id());

		IntelligentTieringConfiguration config = intRequest.intelligentTieringConfiguration();

		assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, config.id());
		assertEquals(Arrays.asList(
				Tiering.builder().days(180).accessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS).build()
		), config.tierings());
		
		assertNull(config.filter().tag());

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
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockS3Client.getBucketIntelligentTieringConfiguration(any(GetBucketIntelligentTieringConfigurationRequest.class)))
				.thenReturn(GetBucketIntelligentTieringConfigurationResponse.builder().intelligentTieringConfiguration(IntelligentTieringConfiguration.builder().build()).build());
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
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationRequest.builder().bucket(expectedBucketName).id(S3BucketBuilderImpl.INT_ARCHIVE_ID).build());
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
	
	@Test
	public void testBuildAllBucketsWithNotificationsConfiguration() throws InterruptedException {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		String topic = "GlobalTopic";
		Set<String> events = new HashSet<>(Arrays.asList("s3:ObjectRestore:Completed", "s3:ObjectRestore:Post"));
		
		bucket.setName("${stack}.bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);
		
		String expectedBucketName = stack + ".bucket";
		String expectedTopicArn = "topicArn";
		String expectedConfigName = topic + "Configuration";
		String expectedGlobalStackName = "synapse-" + stack + "-global-resources";

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		// No notification configuration on the bucket yet
		when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class)))
			.thenReturn(GetBucketNotificationConfigurationResponse.builder().build());
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

		verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
		verify(mockS3Client).getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		ArgumentCaptor<PutBucketNotificationConfigurationRequest> argCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
		
		verify(mockS3Client).putBucketNotificationConfiguration(argCaptor.capture());

		assertEquals(expectedBucketName, argCaptor.getValue().bucket());

		NotificationConfiguration bucketConfig = argCaptor.getValue().notificationConfiguration();
		
		assertEquals(1, bucketConfig.topicConfigurations().size());
		
		TopicConfiguration snsConfig = topicConfigurationById(bucketConfig, expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.topicArn());
		assertEquals(events, new HashSet<>(snsConfig.eventsAsStrings()));

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
	public void testBuildAllBucketsWithNotificationsConfigurationWithEmpty() throws InterruptedException {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		String topic = "GlobalTopic";
		Set<String> events = new HashSet<>(Arrays.asList("s3:ObjectRestore:Completed", "s3:ObjectRestore:Post"));
		
		bucket.setName("${stack}.bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);
		
		String expectedBucketName = stack + ".bucket";
		String expectedTopicArn = "topicArn";
		String expectedConfigName = topic + "Configuration";
		String expectedGlobalStackName = "synapse-" + stack + "-global-resources";

		GetBucketNotificationConfigurationResponse existingConfig = GetBucketNotificationConfigurationResponse.builder().build();

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(existingConfig);
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

		verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
		verify(mockS3Client).getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		ArgumentCaptor<PutBucketNotificationConfigurationRequest> argCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
		
		verify(mockS3Client).putBucketNotificationConfiguration(argCaptor.capture());

		assertEquals(expectedBucketName, argCaptor.getValue().bucket());

		NotificationConfiguration bucketConfig = argCaptor.getValue().notificationConfiguration();
		
		assertEquals(1, bucketConfig.topicConfigurations().size());
		
		TopicConfiguration snsConfig = topicConfigurationById(bucketConfig, expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.topicArn());
		assertEquals(events, new HashSet<>(snsConfig.eventsAsStrings()));

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
	public void testBuildAllBucketsWithNotificationsConfigurationWithExistingNoMatch() throws InterruptedException {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		String topic = "GlobalTopic";
		Set<String> events = new HashSet<>(Arrays.asList("s3:ObjectRestore:Completed", "s3:ObjectRestore:Post"));
		
		bucket.setName("${stack}.bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);
		
		String expectedBucketName = stack + ".bucket";
		String expectedTopicArn = "topicArn";
		String expectedConfigName = topic + "Configuration";
		String expectedGlobalStackName = "synapse-" + stack + "-global-resources";
		
		GetBucketNotificationConfigurationResponse existingConfig = GetBucketNotificationConfigurationResponse.builder()
			.topicConfigurations(TopicConfiguration.builder().id("otherConfig").topicArn("otherArn").events(Event.S3_OBJECT_CREATED).build())
			.build();

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(existingConfig);
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

		verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
		verify(mockS3Client).getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		ArgumentCaptor<PutBucketNotificationConfigurationRequest> argCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
		
		verify(mockS3Client).putBucketNotificationConfiguration(argCaptor.capture());

		assertEquals(expectedBucketName, argCaptor.getValue().bucket());

		NotificationConfiguration bucketConfig = argCaptor.getValue().notificationConfiguration();
		
		assertEquals(2, bucketConfig.topicConfigurations().size());
		
		TopicConfiguration snsConfig = topicConfigurationById(bucketConfig, expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.topicArn());
		assertEquals(events, new HashSet<>(snsConfig.eventsAsStrings()));

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
	public void testBuildAllBucketsWithNotificationsConfigurationWithExistingAndDifferentArn() throws InterruptedException {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		String topic = "GlobalTopic";
		Set<String> events = new HashSet<>(Arrays.asList("s3:ObjectRestore:Completed", "s3:ObjectRestore:Post"));
		
		bucket.setName("${stack}.bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);
		
		String expectedBucketName = stack + ".bucket";
		String expectedTopicArn = "topicArn";
		String expectedConfigName = topic + "Configuration";
		String expectedGlobalStackName = "synapse-" + stack + "-global-resources";
		
		GetBucketNotificationConfigurationResponse existingConfig = GetBucketNotificationConfigurationResponse.builder()
			.topicConfigurations(TopicConfiguration.builder().id(expectedConfigName).topicArn("otherArn").eventsWithStrings(events).build())
			.build();

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(existingConfig);
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

		verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
		verify(mockS3Client).getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		ArgumentCaptor<PutBucketNotificationConfigurationRequest> argCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
		
		verify(mockS3Client).putBucketNotificationConfiguration(argCaptor.capture());

		assertEquals(expectedBucketName, argCaptor.getValue().bucket());

		NotificationConfiguration bucketConfig = argCaptor.getValue().notificationConfiguration();
		
		assertEquals(1, bucketConfig.topicConfigurations().size());
		
		TopicConfiguration snsConfig = topicConfigurationById(bucketConfig, expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.topicArn());
		assertEquals(events, new HashSet<>(snsConfig.eventsAsStrings()));

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
	public void testBuildAllBucketsWithNotificationsConfigurationWithExistingAndDifferentEvents() throws InterruptedException {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		String topic = "GlobalTopic";
		Set<String> events = new HashSet<>(Arrays.asList("s3:ObjectRestore:Completed", "s3:ObjectRestore:Post"));
		
		bucket.setName("${stack}.bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);
		
		String expectedBucketName = stack + ".bucket";
		String expectedTopicArn = "topicArn";
		String expectedConfigName = topic + "Configuration";
		String expectedGlobalStackName = "synapse-" + stack + "-global-resources";
		
		GetBucketNotificationConfigurationResponse existingConfig = GetBucketNotificationConfigurationResponse.builder()
			.topicConfigurations(TopicConfiguration.builder().id(expectedConfigName).topicArn(expectedTopicArn).eventsWithStrings("s3:ObjectRestore:Post").build())
			.build();

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(existingConfig);
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

		verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
		verify(mockS3Client).getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		ArgumentCaptor<PutBucketNotificationConfigurationRequest> argCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
		
		verify(mockS3Client).putBucketNotificationConfiguration(argCaptor.capture());

		assertEquals(expectedBucketName, argCaptor.getValue().bucket());

		NotificationConfiguration bucketConfig = argCaptor.getValue().notificationConfiguration();
		
		assertEquals(1, bucketConfig.topicConfigurations().size());
		
		TopicConfiguration snsConfig = topicConfigurationById(bucketConfig, expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.topicArn());
		assertEquals(events, new HashSet<>(snsConfig.eventsAsStrings()));

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
	public void testBuildAllBucketsWithNotificationsConfigurationWithExistingAndNoUpdate() throws InterruptedException {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		String topic = "GlobalTopic";
		Set<String> events = new HashSet<>(Arrays.asList("s3:ObjectRestore:Completed", "s3:ObjectRestore:Post"));
		
		bucket.setName("${stack}.bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);
		
		String expectedBucketName = stack + ".bucket";
		String expectedTopicArn = "topicArn";
		String expectedConfigName = topic + "Configuration";
		String expectedGlobalStackName = "synapse-" + stack + "-global-resources";
		
		GetBucketNotificationConfigurationResponse existingConfig = GetBucketNotificationConfigurationResponse.builder()
			.topicConfigurations(TopicConfiguration.builder().id(expectedConfigName).topicArn(expectedTopicArn).eventsWithStrings(events).build())
			.build();

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(existingConfig);
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

		verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
		verify(mockS3Client).getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client, never()).putBucketNotificationConfiguration(any(PutBucketNotificationConfigurationRequest.class));

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
	public void testBuildAllBucketsWithNotificationsConfigurationWithMatchingButDifferentType() throws InterruptedException {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		String topic = "GlobalTopic";
		Set<String> events = new HashSet<>(Arrays.asList("s3:ObjectRestore:Completed", "s3:ObjectRestore:Post"));
		
		bucket.setName("${stack}.bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);
		
		String expectedBucketName = stack + ".bucket";
		String expectedTopicArn = "topicArn";
		String expectedConfigName = topic + "Configuration";
		String expectedGlobalStackName = "synapse-" + stack + "-global-resources";
		
		GetBucketNotificationConfigurationResponse existingConfig = GetBucketNotificationConfigurationResponse.builder()
			.queueConfigurations(QueueConfiguration.builder().id(expectedConfigName).queueArn("queueArn").eventsWithStrings(events).build())
			.build();

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(existingConfig);

		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			builder.buildAllBuckets();
		});
		
		assertEquals("The notification configuration " + expectedConfigName + " was found but was not a TopicConfiguration", ex.getMessage());

		verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
		verify(mockS3Client).getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client, never()).putBucketNotificationConfiguration(any(PutBucketNotificationConfigurationRequest.class));
	}
	
	@Test
	public void testBuildAllBucketsWithVirusScannerConfiguration() throws Exception {
		S3BucketDescriptor bucket = new S3BucketDescriptor();

		bucket.setName("bucket");
		bucket.setVirusScanEnabled(true);

		when(mockConfig.getProperty(Constants.PROPERTY_KEY_LAMBDA_VIRUS_SCANNER_ARTIFACT_URL)).thenReturn("https://some-url/lambda-name.zip");
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));

		S3VirusScannerConfig virusScannerConfig = new S3VirusScannerConfig();

		virusScannerConfig.setLambdaArtifactBucket("${stack}-lambda-bucket");
		virusScannerConfig.setNotificationEmail("notification@sagebase.org");

		// The downloaded artifact is uploaded via RequestBody.fromFile, which reads the underlying path
		File artifact = File.createTempFile("virus-scanner-artifact", ".zip");
		artifact.deleteOnExit();
		when(mockFile.toPath()).thenReturn(artifact.toPath());

		when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);
		// No notification configuration on the bucket yet
		when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class)))
			.thenReturn(GetBucketNotificationConfigurationResponse.builder().build());
		when(mockDownloader.downloadFile(any())).thenReturn(mockFile);
		when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);
		
		doAnswer(invocation -> {
			((StringWriter) invocation.getArgument(1)).append("{}");
			return null;
		}).when(mockTemplate).merge(any(), any());

		Stack virusScannerStack = Stack.builder()
				.outputs(
						Output.builder().outputKey(S3BucketBuilderImpl.CF_OUTPUT_VIRUS_TRIGGER_TOPIC).outputValue("snsTopicArn").build(),
						Output.builder().outputKey(S3BucketBuilderImpl.CF_OUTPUT_VIRUS_UPDATER_LAMBDA).outputValue("updaterLambdaArn").build()
				)
				.build();
		
		when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(virusScannerStack));
		when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());
		
		String expectedBucket = stack + "-lambda-bucket";
		String expectedKey = "artifacts/virus-scanner/lambda-name.zip";
		
		// Call under test
		builder.buildAllBuckets();
		
		verify(mockDownloader).downloadFile("https://some-url/lambda-name.zip");
		ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(mockS3Client).putObject(putRequestCaptor.capture(), any(RequestBody.class));
		assertEquals(expectedBucket, putRequestCaptor.getValue().bucket());
		assertEquals(expectedKey, putRequestCaptor.getValue().key());
		verify(mockFile).delete();
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
		
		ArgumentCaptor<PutBucketNotificationConfigurationRequest> argCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
		
		verify(mockS3Client).putBucketNotificationConfiguration(argCaptor.capture());

		assertEquals("bucket", argCaptor.getValue().bucket());

		NotificationConfiguration bucketConfig = argCaptor.getValue().notificationConfiguration();
		
		assertEquals(1, bucketConfig.topicConfigurations().size());
		
		TopicConfiguration snsConfig = topicConfigurationById(bucketConfig, S3BucketBuilderImpl.VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME);
		
		assertEquals("snsTopicArn", snsConfig.topicArn());
		assertEquals(Collections.singleton(Event.S3_OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD.toString()), new HashSet<>(snsConfig.eventsAsStrings()));

		verify(mockLambdaClient).invoke(InvokeRequest.builder()
				.functionName("updaterLambdaArn")
				.invocationType(InvocationType.EVENT)
				.build()
		);	}
		
	@Test
	public void testBuildAllBucketsWithVirusScannerConfigurationAndBucketNotificationRemoval() throws Exception {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		
		bucket.setName("bucket");
		bucket.setVirusScanEnabled(false);

		when(mockConfig.getProperty(Constants.PROPERTY_KEY_LAMBDA_VIRUS_SCANNER_ARTIFACT_URL)).thenReturn("https://some-url/lambda-name.zip");
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		S3VirusScannerConfig virusScannerConfig = new S3VirusScannerConfig();
		
		virusScannerConfig.setLambdaArtifactBucket("${stack}-lambda-bucket");
		virusScannerConfig.setNotificationEmail("notification@sagebase.org");
		
		when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);
		File artifact = File.createTempFile("virus-scanner-artifact", ".zip");
		artifact.deleteOnExit();
		when(mockDownloader.downloadFile(any())).thenReturn(artifact);
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
				)
				.build();
		
		when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(virusScannerStack));
		when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());
				
		// Fake an existing config for the scanner
		GetBucketNotificationConfigurationResponse bucketConfiguration = GetBucketNotificationConfigurationResponse.builder()
			.topicConfigurations(TopicConfiguration.builder().id(S3BucketBuilderImpl.VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME).build())
			.build();
		
		when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(bucketConfiguration);

		String expectedBucket = stack + "-lambda-bucket";
		String expectedKey = "artifacts/virus-scanner/lambda-name.zip";
		
		// Call under test
		builder.buildAllBuckets();
		
		ArgumentCaptor<PutBucketNotificationConfigurationRequest> argCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
		
		verify(mockS3Client).putBucketNotificationConfiguration(argCaptor.capture());

		assertEquals("bucket", argCaptor.getValue().bucket());

		NotificationConfiguration configuration = argCaptor.getValue().notificationConfiguration();
		
		// Make sure the config was removed
		assertTrue(configuration.topicConfigurations().isEmpty());

		verify(mockTemplate, times(2)).merge(velocityContextCaptor.capture(), any());
		List<VelocityContext> contexts = velocityContextCaptor.getAllValues();
		VelocityContext virusScannerBuilderContext = contexts.get(0);
		VelocityContext bucketPolicyBuilderContext = contexts.get(1);

		assertEquals(virusScannerBuilderContext.get(Constants.STACK), stack);
		assertEquals(bucketPolicyBuilderContext.get(Constants.STACK), stack);

		assertEquals(virusScannerBuilderContext.get(Constants.STACK), stack);
		assertEquals(virusScannerBuilderContext.get(S3BucketBuilderImpl.CF_PROPERTY_BUCKETS), Arrays.asList());
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
	
	@Test
	public void testBuildAllBucketsWithNonExistingPublicBlock() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");

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
		
		S3Exception exception = (S3Exception) S3Exception.builder().statusCode(404).message("Nope").build();

		when(mockS3Client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenThrow(exception);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).getPublicAccessBlock(GetPublicAccessBlockRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).putPublicAccessBlock(PutPublicAccessBlockRequest.builder().bucket(expectedBucketName)
			.publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
				.blockPublicAcls(true)
				.blockPublicPolicy(true)
				.ignorePublicAcls(true)
				.restrictPublicBuckets(true)
				.build()
			)
			.build()
		);

	}
	
	@Test
	public void testBuildAllBucketsWithExistingPublicBlock() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");

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
		when(mockS3Client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenReturn(GetPublicAccessBlockResponse.builder()
			.publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
				.blockPublicAcls(false)
				.blockPublicPolicy(false)
				.ignorePublicAcls(false)
				.restrictPublicBuckets(false)
				.build()
			)
			.build()
		);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).getPublicAccessBlock(GetPublicAccessBlockRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client, never()).putPublicAccessBlock(any(PutPublicAccessBlockRequest.class));

	}
	
	private static TopicConfiguration topicConfigurationById(NotificationConfiguration config, String id) {
		return config.topicConfigurations().stream().filter(topicConfig -> id.equals(topicConfig.id())).findFirst().orElse(null);
	}

	private LifecycleRule.Builder allBucketRule(String ruleName) {
		return LifecycleRule.builder().id(ruleName).filter(LifecycleRuleFilter.builder().build()).status(ExpirationStatus.ENABLED);
	}
}
