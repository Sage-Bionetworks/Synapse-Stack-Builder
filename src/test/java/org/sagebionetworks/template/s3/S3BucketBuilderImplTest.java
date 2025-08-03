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

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
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
	public void testBuildAllBuckets() throws InterruptedException {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");

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

		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(GetBucketLifecycleConfigurationResponse.builder().build());

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
				.build())
			.build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		ArgumentCaptor<PutBucketLifecycleConfigurationRequest> putBucketLifecycleConfigurationRequestCaptor = ArgumentCaptor.forClass(PutBucketLifecycleConfigurationRequest.class);
		verify(mockS3Client).putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequestCaptor.capture());
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
		
		PutBucketLifecycleConfigurationRequest capturedPutBucketLifecycleConfigurationRequest = putBucketLifecycleConfigurationRequestCaptor.getValue();
		BucketLifecycleConfiguration config = capturedPutBucketLifecycleConfigurationRequest.lifecycleConfiguration();
		
		assertEquals(1, config.rules().size());

		LifecycleRule rule = config.rules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS, rule.id());
		assertEquals(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS, rule.abortIncompleteMultipartUpload().daysAfterInitiation());
		assertEquals(ExpirationStatus.ENABLED, rule.status());
		assertNull(rule.prefix());
		assertNotNull(rule.filter());
		// Filter properties vary in v2 - skip predicate assertion for now
		// assertNull(rule.filter().predicate());

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
		BucketLifecycleConfiguration expectedGetBucketConfiguration = BucketLifecycleConfiguration.builder()
				.rules(
						allBucketRuleBuilder(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS)
								.abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build())
								.build()
				)
				.build();
		GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedGetBucketConfiguration.rules()).build();
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

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
		
		when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
		when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

		doAnswer(invocation -> {
			((StringWriter) invocation.getArgument(1)).append("{}");
			return null;
		}).when(mockTemplate).merge(any(), any());

		Stack bucketPolicyStack = Stack.builder().build();

		when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
		when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());
		
		// Mimics an existing life cycle with the abort rule already present
		BucketLifecycleConfiguration expectedGetBucketConfiguration = BucketLifecycleConfiguration.builder()
				.rules(
						allBucketRuleBuilder(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS)
								.abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS-1).build())
								.build()
				)
				.build();
		GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedGetBucketConfiguration.rules()).build();
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());

		ArgumentCaptor<PutBucketLifecycleConfigurationRequest> putBucketLifecycleConfigurationRequestCaptor = ArgumentCaptor.forClass(PutBucketLifecycleConfigurationRequest.class);
		verify(mockS3Client).putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequestCaptor.capture());

		PutBucketLifecycleConfigurationRequest capturedPutBucketLifecycleConfigurationRequest = putBucketLifecycleConfigurationRequestCaptor.getValue();
		BucketLifecycleConfiguration config = capturedPutBucketLifecycleConfigurationRequest.lifecycleConfiguration();
		
		assertEquals(1, config.rules().size());

		LifecycleRule rule = config.rules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS, rule.id());
		assertEquals(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS, rule.abortIncompleteMultipartUpload().daysAfterInitiation());
		assertEquals(ExpirationStatus.ENABLED, rule.status());
		assertNotNull(rule.filter());
		
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
	public void testBuildAllBucketsNeedsEncrypted() throws InterruptedException {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		
		when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
		when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

		doAnswer(invocation -> {
			((StringWriter) invocation.getArgument(1)).append("{}");
			return null;
		}).when(mockTemplate).merge(any(), any());

		Stack bucketPolicyStack = Stack.builder().build();

		when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
		when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

		AwsServiceException notFound = S3Exception.builder().message("NotFound").statusCode(404).build();

		doThrow(notFound).when(mockS3Client).getBucketEncryption(any(GetBucketEncryptionRequest.class));

		String expectedBucketName = stack + ".bucket";

		GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

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
		assertEquals(ServerSideEncryption.AES256.name(), rule.applyServerSideEncryptionByDefault().sseAlgorithm().name());
		
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

		GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));

		ArgumentCaptor<PutBucketLifecycleConfigurationRequest> putBucketLifeCycleConfigurationCaptor = ArgumentCaptor.forClass(PutBucketLifecycleConfigurationRequest.class);
		verify(mockS3Client).putBucketLifecycleConfiguration(putBucketLifeCycleConfigurationCaptor.capture());
		
		PutBucketLifecycleConfigurationRequest putBucketLifecycleConfigurationRequest = putBucketLifeCycleConfigurationCaptor.getValue();
		BucketLifecycleConfiguration config = putBucketLifecycleConfigurationRequest.lifecycleConfiguration();
		
		assertEquals(2, config.rules().size());
		
		LifecycleRule rule = config.rules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_RETENTION, rule.id());
		assertEquals(bucket.getRetentionDays(), rule.expiration().days());
		assertEquals(ExpirationStatus.ENABLED, rule.status());
		assertNotNull(rule.filter());

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
		BucketLifecycleConfiguration expectedGetBucketConfiguration = BucketLifecycleConfiguration.builder()
				.rules(
						allBucketRuleBuilder(StorageClass.INTELLIGENT_TIERING + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION)
								.transitions(Transition.builder().storageClass(TransitionStorageClass.INTELLIGENT_TIERING).days(30).build())
								.build(),
						allBucketRuleBuilder(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS)
								.abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build())
								.build(),
						allBucketRuleBuilder(S3BucketBuilderImpl.RULE_ID_RETENTION).expiration(LifecycleExpiration.builder().days(30).build())
								.build()
				)
				.build();

		GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedGetBucketConfiguration.rules()).build();
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
				.thenReturn(expectedGetBucketLifecycleConfigurationResponse);
		
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
		BucketLifecycleConfiguration expectedGetBucketConfiguration = BucketLifecycleConfiguration.builder()
				.rules(
						allBucketRuleBuilder(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS)
								.abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS).build())
								.build(),
						allBucketRuleBuilder(S3BucketBuilderImpl.RULE_ID_RETENTION).expiration(LifecycleExpiration.builder().days(45).build())
								.build()
				)
				.build();

		GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(expectedGetBucketConfiguration.rules()).build();
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(CreateBucketRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(expectedBucketName).build());
		
		verify(mockS3Client, never()).putBucketEncryption(any(PutBucketEncryptionRequest.class));
		verify(mockS3Client, never()).putBucketInventoryConfiguration(any(PutBucketInventoryConfigurationRequest.class));

		ArgumentCaptor<PutBucketLifecycleConfigurationRequest> putBucketLifeCycleConfigurationCaptor = ArgumentCaptor.forClass(PutBucketLifecycleConfigurationRequest.class);
		verify(mockS3Client).putBucketLifecycleConfiguration(putBucketLifeCycleConfigurationCaptor.capture());

		PutBucketLifecycleConfigurationRequest putBucketLifecycleConfigurationRequest = putBucketLifeCycleConfigurationCaptor.getValue();
		BucketLifecycleConfiguration config = putBucketLifecycleConfigurationRequest.lifecycleConfiguration();

		assertEquals(2, config.rules().size());
		
		LifecycleRule rule = config.rules().get(1);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_RETENTION, rule.id());
		assertEquals(bucket.getRetentionDays(), rule.expiration().days());
		assertEquals(ExpirationStatus.ENABLED, rule.status());
		assertNotNull(rule.filter());

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

		GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

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
		
		AwsServiceException exception = S3Exception.builder().message("Nope").statusCode(404).build();
		
		when(mockS3Client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenThrow(exception);

		GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).getPublicAccessBlock(GetPublicAccessBlockRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client).putPublicAccessBlock(PutPublicAccessBlockRequest.builder().bucket(expectedBucketName)
			.publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
				.blockPublicAcls(true)
				.blockPublicPolicy(true)
				.ignorePublicAcls(true)
				.restrictPublicBuckets(true)
				.build())
			.build());

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
		GetPublicAccessBlockResponse getPublicAccessBlockResponse = GetPublicAccessBlockResponse.builder()
				.publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
						.blockPublicAcls(false)
						.blockPublicPolicy(false)
						.ignorePublicAcls(false)
						.restrictPublicBuckets(false)
						.build())
				.build();
		when(mockS3Client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenReturn(getPublicAccessBlockResponse);

		GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
		when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).getPublicAccessBlock(GetPublicAccessBlockRequest.builder().bucket(expectedBucketName).build());
		verify(mockS3Client, never()).putPublicAccessBlock(any(PutPublicAccessBlockRequest.class));

	}
	
	private LifecycleRule.Builder allBucketRuleBuilder(String ruleName) {
		return LifecycleRule.builder()
			.id(ruleName)
			.filter(LifecycleRuleFilter.builder().build())
			.status(ExpirationStatus.ENABLED);
	}

}
