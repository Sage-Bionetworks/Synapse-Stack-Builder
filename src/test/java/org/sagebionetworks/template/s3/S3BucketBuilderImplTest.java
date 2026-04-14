package org.sagebionetworks.template.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.EnumSet;
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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortIncompleteMultipartUpload;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Transition;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.GetBucketIntelligentTieringConfigurationResult;
import com.amazonaws.services.s3.model.GetBucketInventoryConfigurationResult;
import com.amazonaws.services.s3.model.GetPublicAccessBlockRequest;
import com.amazonaws.services.s3.model.GetPublicAccessBlockResult;
import com.amazonaws.services.s3.model.PublicAccessBlockConfiguration;
import com.amazonaws.services.s3.model.QueueConfiguration;
import com.amazonaws.services.s3.model.S3Event;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.SetPublicAccessBlockRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.TopicConfiguration;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringAccessTier;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringConfiguration;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringTagPredicate;
import com.amazonaws.services.s3.model.intelligenttiering.Tiering;
import com.amazonaws.services.s3.model.inventory.InventoryConfiguration;
import com.amazonaws.services.s3.model.inventory.InventoryFrequency;
import com.amazonaws.services.s3.model.inventory.InventoryS3BucketDestination;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
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
	private AmazonS3 mockS3Client;

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
	private ArgumentCaptor<SetBucketEncryptionRequest> encryptionRequestCaptor;
	
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

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getPublicAccessBlock(new GetPublicAccessBlockRequest().withBucketName(expectedBucketName));
		verify(mockS3Client).setPublicAccessBlock(new SetPublicAccessBlockRequest().withBucketName(expectedBucketName)
			.withPublicAccessBlockConfiguration(new PublicAccessBlockConfiguration()
				.withBlockPublicAcls(true)
				.withBlockPublicPolicy(true)
				.withIgnorePublicAcls(true)
				.withRestrictPublicBuckets(true)
			)
		);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
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
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		assertEquals(1, config.getRules().size());

		Rule rule = config.getRules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS, rule.getId());
		assertEquals(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS, rule.getAbortIncompleteMultipartUpload().getDaysAfterInitiation());
		assertEquals(BucketLifecycleConfiguration.ENABLED, rule.getStatus());
		assertNull(rule.getPrefix());
		assertNotNull(rule.getFilter());
		assertNull(rule.getFilter().getPredicate());

		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).setBucketPolicy(any(), any());

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
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration()
			.withRules(
					allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS))
			));

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).setBucketPolicy(any(), any());
		verify(mockS3Client, never()).setBucketLifecycleConfiguration(any(), any());

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
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration()
			.withRules(
					allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS - 1))
			));

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		assertEquals(1, config.getRules().size());

		Rule rule = config.getRules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS, rule.getId());
		assertEquals(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS, rule.getAbortIncompleteMultipartUpload().getDaysAfterInitiation());
		assertEquals(BucketLifecycleConfiguration.ENABLED, rule.getStatus());
		assertNull(rule.getPrefix());
		assertNotNull(rule.getFilter());
		assertNull(rule.getFilter().getPredicate());
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).setBucketPolicy(any(), any());

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

		AmazonServiceException notFound = new AmazonServiceException("NotFound");
		notFound.setStatusCode(404);

		doThrow(notFound).when(mockS3Client).getBucketEncryption(anyString());

		String expectedBucketName = stack + ".bucket";

		// call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).setBucketEncryption(encryptionRequestCaptor.capture());
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);

		SetBucketEncryptionRequest request = encryptionRequestCaptor.getValue();

		assertNotNull(request);
		assertEquals(expectedBucketName, request.getBucketName());
		assertNotNull(request.getServerSideEncryptionConfiguration());
		assertNotNull(request.getServerSideEncryptionConfiguration().getRules());
		assertEquals(1, request.getServerSideEncryptionConfiguration().getRules().size());
		ServerSideEncryptionRule rule = request.getServerSideEncryptionConfiguration().getRules().get(0);
		assertNotNull(rule.getApplyServerSideEncryptionByDefault());
		assertEquals(SSEAlgorithm.AES256.name(), rule.getApplyServerSideEncryptionByDefault().getSSEAlgorithm());
		
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).setBucketPolicy(any(), any());

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
		
		AmazonServiceException notFound = new AmazonServiceException("NotFound");
		notFound.setStatusCode(404);

		// No inventory configuration set
		doThrow(notFound).when(mockS3Client).getBucketInventoryConfiguration(anyString(), anyString());

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

		verify(mockS3Client).createBucket(expectedInventoryBucketName);
		verify(mockS3Client).createBucket(expectedBucketName);
		
		verify(mockS3Client).getBucketEncryption(expectedInventoryBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		
		verify(mockS3Client).getBucketInventoryConfiguration(expectedBucketName, S3BucketBuilderImpl.INVENTORY_ID);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);

		verify(mockS3Client).setBucketInventoryConfiguration(eq(expectedBucketName), inventoryConfigurationCaptor.capture());
		
		InventoryConfiguration config = inventoryConfigurationCaptor.getValue();
		
		assertEquals(S3BucketBuilderImpl.INVENTORY_ID, config.getId());
		assertEquals(S3BucketBuilderImpl.INVENTORY_FIELDS, config.getOptionalFields());
		assertEquals(InventoryFrequency.Weekly.toString(), config.getSchedule().getFrequency());
		
		InventoryS3BucketDestination destination = config.getDestination().getS3BucketDestination();
		
		assertEquals("arn:aws:s3:::" + expectedInventoryBucketName, destination.getBucketArn());
		assertEquals("prefix", destination.getPrefix());
		assertEquals(accountId, destination.getAccountId());
		assertEquals(S3BucketBuilderImpl.INVENTORY_FORMAT, destination.getFormat());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(), any());

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
		when(mockS3Client.getBucketInventoryConfiguration(anyString(), anyString())).thenReturn(
				new GetBucketInventoryConfigurationResult().withInventoryConfiguration(
						new InventoryConfiguration()
						.withEnabled(true))
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

		verify(mockS3Client).createBucket(expectedInventoryBucketName);
		verify(mockS3Client).createBucket(expectedBucketName);
		
		verify(mockS3Client).getBucketEncryption(expectedInventoryBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		
		verify(mockS3Client).getBucketInventoryConfiguration(expectedBucketName, S3BucketBuilderImpl.INVENTORY_ID);
		
		verify(mockS3Client, never()).setBucketEncryption(any());

		verify(mockS3Client).setBucketInventoryConfiguration(eq(expectedBucketName), inventoryConfigurationCaptor.capture());
		
		InventoryConfiguration config = inventoryConfigurationCaptor.getValue();
		
		assertEquals(S3BucketBuilderImpl.INVENTORY_ID, config.getId());
		assertEquals(S3BucketBuilderImpl.INVENTORY_FIELDS, config.getOptionalFields());
		assertEquals(InventoryFrequency.Weekly.toString(), config.getSchedule().getFrequency());
		
		InventoryS3BucketDestination destination = config.getDestination().getS3BucketDestination();
		
		assertEquals("arn:aws:s3:::" + expectedInventoryBucketName, destination.getBucketArn());
		assertEquals("prefix", destination.getPrefix());
		assertEquals(accountId, destination.getAccountId());
		assertEquals(S3BucketBuilderImpl.INVENTORY_FORMAT, destination.getFormat());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(expectedBucketName, S3BucketBuilderImpl.INVENTORY_ID);

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
		
		AmazonServiceException notFound = new AmazonServiceException("NotFound");
		notFound.setStatusCode(404);

		// No inventory configuration set
		doThrow(notFound).when(mockS3Client).getBucketInventoryConfiguration(anyString(), anyString());

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

		verify(mockS3Client).createBucket(expectedInventoryBucketName);
		verify(mockS3Client).createBucket(expectedBucketName);
		
		verify(mockS3Client).getBucketEncryption(expectedInventoryBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		
		verify(mockS3Client).getBucketInventoryConfiguration(expectedBucketName, S3BucketBuilderImpl.INVENTORY_ID);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());

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
		when(mockS3Client.getBucketInventoryConfiguration(anyString(), anyString())).thenReturn(
				new GetBucketInventoryConfigurationResult().withInventoryConfiguration(
						new InventoryConfiguration()
						.withEnabled(true))
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

		verify(mockS3Client).createBucket(expectedInventoryBucketName);
		verify(mockS3Client).createBucket(expectedBucketName);
		
		verify(mockS3Client).getBucketEncryption(expectedInventoryBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		
		verify(mockS3Client).getBucketInventoryConfiguration(expectedBucketName, S3BucketBuilderImpl.INVENTORY_ID);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		verify(mockS3Client).deleteBucketInventoryConfiguration(expectedBucketName, S3BucketBuilderImpl.INVENTORY_ID);

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

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		assertEquals(2, config.getRules().size());
		
		Rule rule = config.getRules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_RETENTION, rule.getId());
		assertEquals(bucket.getRetentionDays(), rule.getExpirationInDays());
		assertEquals(BucketLifecycleConfiguration.ENABLED, rule.getStatus());
		assertNull(rule.getPrefix());
		assertNotNull(rule.getFilter());
		assertNull(rule.getFilter().getPredicate());

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
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration()
			.withRules(
				allBucketRule(StorageClass.IntelligentTiering.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).addTransition(new Transition().withStorageClass(StorageClass.IntelligentTiering).withDays(30)),
				allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS)),
				allBucketRule(S3BucketBuilderImpl.RULE_ID_RETENTION).withExpirationInDays(30)
			)
		);
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client, never()).setBucketLifecycleConfiguration(any(), any());

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
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration()
			.withRules(
				allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS)),
				allBucketRule(S3BucketBuilderImpl.RULE_ID_RETENTION).withExpirationInDays(45)
			)
		);
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		assertEquals(2, config.getRules().size());
		
		Rule rule = config.getRules().get(1);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_RETENTION, rule.getId());
		assertEquals(bucket.getRetentionDays(), rule.getExpirationInDays());
		assertEquals(BucketLifecycleConfiguration.ENABLED, rule.getStatus());
		assertNull(rule.getPrefix());
		assertNotNull(rule.getFilter());
		assertNull(rule.getFilter().getPredicate());

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
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration()
			.withRules(
				allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS)),
				allBucketRule(S3BucketBuilderImpl.RULE_ID_RETENTION).withExpirationInDays(30)
			)
		);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		// Should only have 1 rule now (abort multipart), retention rule should be removed
		assertEquals(1, config.getRules().size());
		
		Rule rule = config.getRules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS, rule.getId());

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
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration()
			.withRules(
				allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS))
			)
		);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		// Should not update lifecycle configuration since no changes are needed
		verify(mockS3Client, never()).setBucketLifecycleConfiguration(any(), any());

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
					.withStorageClass(StorageClass.IntelligentTiering)
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

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		assertEquals(2, config.getRules().size());

		Rule rule = config.getRules().get(0);
		
		assertEquals(1, rule.getTransitions().size());
		
		assertEquals(StorageClass.IntelligentTiering.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, rule.getId());
		assertEquals(30, rule.getTransitions().get(0).getDays());
		assertEquals(StorageClass.IntelligentTiering.toString(), rule.getTransitions().get(0).getStorageClassAsString());
		assertEquals(BucketLifecycleConfiguration.ENABLED, rule.getStatus());
		assertNull(rule.getPrefix());
		assertNotNull(rule.getFilter());
		assertNull(rule.getFilter().getPredicate());

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
					.withStorageClass(StorageClass.IntelligentTiering)
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
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration()
				.withRules(
						allBucketRule(StorageClass.IntelligentTiering.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).addTransition(new Transition().withStorageClass(StorageClass.IntelligentTiering).withDays(30)),
						allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS))
				));
				
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client, never()).setBucketLifecycleConfiguration(any(), any());

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
					.withStorageClass(StorageClass.IntelligentTiering)
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
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration()
				.withRules(
						allBucketRule(StorageClass.IntelligentTiering.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).addTransition(new Transition().withStorageClass(StorageClass.IntelligentTiering).withDays(35)),
						allBucketRule(S3BucketBuilderImpl.RULE_ID_ABORT_MULTIPART_UPLOADS).withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(S3BucketBuilderImpl.ABORT_MULTIPART_UPLOAD_DAYS))
				));
				
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		assertEquals(2, config.getRules().size());
		
		Rule intRule = config.getRules().get(0);
		
		assertEquals(StorageClass.IntelligentTiering.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, intRule.getId());
		assertEquals(30, intRule.getTransitions().get(0).getDays());
		assertEquals(StorageClass.IntelligentTiering.toString(), intRule.getTransitions().get(0).getStorageClassAsString());
		assertNull(intRule.getPrefix());
		assertNotNull(intRule.getFilter());
		assertNull(intRule.getFilter().getPredicate());

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
					.withStorageClass(StorageClass.StandardInfrequentAccess)
					.withDays(15),
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.IntelligentTiering)
					.withDays(30),
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.DeepArchive)
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
		
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration()
			.withRules(
					// The infrequent access is not there
					// The intelligent tiering should be updated
					allBucketRule(StorageClass.IntelligentTiering.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).addTransition(new Transition().withStorageClass(StorageClass.IntelligentTiering).withDays(35)),
					// This is the same
					allBucketRule(StorageClass.DeepArchive.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION).addTransition(new Transition().withStorageClass(StorageClass.DeepArchive).withDays(90))
			));
				
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		assertEquals(4, config.getRules().size());
		
		Rule intRule = config.getRules().get(0);
		
		assertEquals(StorageClass.IntelligentTiering.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, intRule.getId());
		assertEquals(30, intRule.getTransitions().get(0).getDays());
		assertEquals(StorageClass.IntelligentTiering.toString(), intRule.getTransitions().get(0).getStorageClassAsString());
		assertEquals(BucketLifecycleConfiguration.ENABLED, intRule.getStatus());
		assertNull(intRule.getPrefix());
		assertNotNull(intRule.getFilter());
		assertNull(intRule.getFilter().getPredicate());
		
		Rule arcRule = config.getRules().get(1);
		
		assertEquals(StorageClass.DeepArchive.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, arcRule.getId());
		assertEquals(90, arcRule.getTransitions().get(0).getDays());
		assertEquals(StorageClass.DeepArchive.toString(), arcRule.getTransitions().get(0).getStorageClassAsString());
		assertEquals(BucketLifecycleConfiguration.ENABLED, arcRule.getStatus());
		assertNull(arcRule.getPrefix());
		assertNotNull(arcRule.getFilter());
		assertNull(arcRule.getFilter().getPredicate());
		
		Rule iaRule = config.getRules().get(2);
		
		assertEquals(StorageClass.StandardInfrequentAccess.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, iaRule.getId());
		assertEquals(15, iaRule.getTransitions().get(0).getDays());
		assertEquals(StorageClass.StandardInfrequentAccess.toString(), iaRule.getTransitions().get(0).getStorageClassAsString());
		assertEquals(BucketLifecycleConfiguration.ENABLED, iaRule.getStatus());
		assertNull(iaRule.getPrefix());
		assertNotNull(iaRule.getFilter());
		assertNull(iaRule.getFilter().getPredicate());

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
					.withStorageClass(StorageClass.IntelligentTiering)
					.withDays(30),
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.DeepArchive)
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

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		assertEquals(3, config.getRules().size());
		
		Rule intRule = config.getRules().get(0);
		
		assertEquals(StorageClass.IntelligentTiering.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, intRule.getId());
		assertEquals(30, intRule.getTransitions().get(0).getDays());
		assertEquals(StorageClass.IntelligentTiering.toString(), intRule.getTransitions().get(0).getStorageClassAsString());
		assertEquals(BucketLifecycleConfiguration.ENABLED, intRule.getStatus());
		assertNull(intRule.getPrefix());
		assertNotNull(intRule.getFilter());
		assertNull(intRule.getFilter().getPredicate());
		
		Rule arcRule = config.getRules().get(1);
		
		assertEquals(StorageClass.DeepArchive.name() + S3BucketBuilderImpl.RULE_ID_CLASS_TRANSITION, arcRule.getId());
		assertEquals(90, arcRule.getTransitions().get(0).getDays());
		assertEquals(StorageClass.DeepArchive.toString(), arcRule.getTransitions().get(0).getStorageClassAsString());
		assertEquals(BucketLifecycleConfiguration.ENABLED, arcRule.getStatus());
		assertNull(arcRule.getPrefix());
		assertNotNull(arcRule.getFilter());
		assertNull(arcRule.getFilter().getPredicate());

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

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);

		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).setBucketPolicy(any(), any());

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
		
		AmazonS3Exception notFound = new AmazonS3Exception("Not Found");
		notFound.setErrorCode("NoSuchConfiguration");
		notFound.setStatusCode(404);
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		doThrow(notFound).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(), any());

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
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(expectedBucketName, S3BucketBuilderImpl.INT_ARCHIVE_ID);
		verify(mockS3Client).setBucketIntelligentTieringConfiguration(eq(expectedBucketName), intConfigurationCaptor.capture());
		
		IntelligentTieringConfiguration config = intConfigurationCaptor.getValue();
		
		assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, config.getId());
		assertEquals(Arrays.asList(
				new Tiering().withDays(90).withIntelligentTieringAccessTier(IntelligentTieringAccessTier.ARCHIVE_ACCESS),
				new Tiering().withDays(180).withIntelligentTieringAccessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS)
		), config.getTierings());
		
		Tag tag = ((IntelligentTieringTagPredicate)config.getFilter().getPredicate()).getTag();
		
		assertEquals("test",  tag.getKey());
		assertEquals("tag",  tag.getValue());

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
		
		AmazonS3Exception anotherEx = new AmazonS3Exception("Not Found");
		anotherEx.setErrorCode("NoSuchConfiguration");
		anotherEx.setStatusCode(503);
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		doThrow(anotherEx).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(), any());
				
		AmazonS3Exception ex = assertThrows(AmazonS3Exception.class, () -> {			
			// Call under test
			builder.buildAllBuckets();
		});
		
		assertEquals(anotherEx, ex);
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(expectedBucketName, S3BucketBuilderImpl.INT_ARCHIVE_ID);
		verify(mockS3Client, never()).setBucketIntelligentTieringConfiguration(any(), any());

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
		
		AmazonS3Exception anotherEx = new AmazonS3Exception("Not Found");
		anotherEx.setErrorCode("NoSuchBucket");
		anotherEx.setStatusCode(404);
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		doThrow(anotherEx).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(), any());
				
		AmazonS3Exception ex = assertThrows(AmazonS3Exception.class, () -> {			
			// Call under test
			builder.buildAllBuckets();
		});
		
		assertEquals(anotherEx, ex);
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(expectedBucketName, S3BucketBuilderImpl.INT_ARCHIVE_ID);
		verify(mockS3Client, never()).setBucketIntelligentTieringConfiguration(any(), any());
		
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
		
		AmazonS3Exception notFound = new AmazonS3Exception("Not Found");
		notFound.setErrorCode("NoSuchConfiguration");
		notFound.setStatusCode(404);
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		doThrow(notFound).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(), any());
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
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(expectedBucketName, S3BucketBuilderImpl.INT_ARCHIVE_ID);
		verify(mockS3Client).setBucketIntelligentTieringConfiguration(eq(expectedBucketName), intConfigurationCaptor.capture());
		
		IntelligentTieringConfiguration config = intConfigurationCaptor.getValue();
		
		assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, config.getId());
		assertEquals(Arrays.asList(
				new Tiering().withDays(90).withIntelligentTieringAccessTier(IntelligentTieringAccessTier.ARCHIVE_ACCESS),
				new Tiering().withDays(180).withIntelligentTieringAccessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS)
		), config.getTierings());
		
		assertNull(config.getFilter().getPredicate());

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
		
		AmazonS3Exception notFound = new AmazonS3Exception("Not Found");
		notFound.setErrorCode("NoSuchConfiguration");
		notFound.setStatusCode(404);
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		doThrow(notFound).when(mockS3Client).getBucketIntelligentTieringConfiguration(any(), any());
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
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(expectedBucketName, S3BucketBuilderImpl.INT_ARCHIVE_ID);
		verify(mockS3Client).setBucketIntelligentTieringConfiguration(eq(expectedBucketName), intConfigurationCaptor.capture());
		
		IntelligentTieringConfiguration config = intConfigurationCaptor.getValue();
		
		assertEquals(S3BucketBuilderImpl.INT_ARCHIVE_ID, config.getId());
		assertEquals(Arrays.asList(
				new Tiering().withDays(180).withIntelligentTieringAccessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS)
		), config.getTierings());
		
		assertNull(config.getFilter().getPredicate());

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
		when(mockS3Client.getBucketIntelligentTieringConfiguration(any(), any())).thenReturn(new GetBucketIntelligentTieringConfigurationResult().withIntelligentTieringConfiguration(new IntelligentTieringConfiguration()));
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
		
		verify(mockS3Client).getBucketIntelligentTieringConfiguration(expectedBucketName, S3BucketBuilderImpl.INT_ARCHIVE_ID);
		verify(mockS3Client, never()).setBucketIntelligentTieringConfiguration(any(), any());

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
		verify(mockS3Client).getBucketNotificationConfiguration(expectedBucketName);
		
		ArgumentCaptor<BucketNotificationConfiguration> argCaptor = ArgumentCaptor.forClass(BucketNotificationConfiguration.class);
		
		verify(mockS3Client).setBucketNotificationConfiguration(eq(expectedBucketName), argCaptor.capture());
		
		BucketNotificationConfiguration bucketConfig = argCaptor.getValue();
		
		assertEquals(1, bucketConfig.getConfigurations().size());
		
		TopicConfiguration snsConfig = (TopicConfiguration) bucketConfig.getConfigurationByName(expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.getTopicARN());
		assertEquals(events, snsConfig.getEvents());

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

		BucketNotificationConfiguration existingConfig = new BucketNotificationConfiguration();

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(anyString())).thenReturn(existingConfig);
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
		verify(mockS3Client).getBucketNotificationConfiguration(expectedBucketName);
		
		ArgumentCaptor<BucketNotificationConfiguration> argCaptor = ArgumentCaptor.forClass(BucketNotificationConfiguration.class);
		
		verify(mockS3Client).setBucketNotificationConfiguration(eq(expectedBucketName), argCaptor.capture());
		
		BucketNotificationConfiguration bucketConfig = argCaptor.getValue();
		
		assertEquals(1, bucketConfig.getConfigurations().size());
		
		TopicConfiguration snsConfig = (TopicConfiguration) bucketConfig.getConfigurationByName(expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.getTopicARN());
		assertEquals(events, snsConfig.getEvents());

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
		
		BucketNotificationConfiguration existingConfig = new BucketNotificationConfiguration();
		
		existingConfig.addConfiguration("otherConfig", new TopicConfiguration("otherArn", EnumSet.of(S3Event.ObjectCreated)));

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(anyString())).thenReturn(existingConfig);
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
		verify(mockS3Client).getBucketNotificationConfiguration(expectedBucketName);
		
		ArgumentCaptor<BucketNotificationConfiguration> argCaptor = ArgumentCaptor.forClass(BucketNotificationConfiguration.class);
		
		verify(mockS3Client).setBucketNotificationConfiguration(eq(expectedBucketName), argCaptor.capture());
		
		BucketNotificationConfiguration bucketConfig = argCaptor.getValue();
		
		assertEquals(2, bucketConfig.getConfigurations().size());
		
		TopicConfiguration snsConfig = (TopicConfiguration) bucketConfig.getConfigurationByName(expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.getTopicARN());
		assertEquals(events, snsConfig.getEvents());

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
		
		BucketNotificationConfiguration existingConfig = new BucketNotificationConfiguration();
		
		existingConfig.addConfiguration(expectedConfigName, new TopicConfiguration().withTopicARN("otherArn").withEvents(events));

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(anyString())).thenReturn(existingConfig);
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
		verify(mockS3Client).getBucketNotificationConfiguration(expectedBucketName);
		
		ArgumentCaptor<BucketNotificationConfiguration> argCaptor = ArgumentCaptor.forClass(BucketNotificationConfiguration.class);
		
		verify(mockS3Client).setBucketNotificationConfiguration(eq(expectedBucketName), argCaptor.capture());
		
		BucketNotificationConfiguration bucketConfig = argCaptor.getValue();
		
		assertEquals(1, bucketConfig.getConfigurations().size());
		
		TopicConfiguration snsConfig = (TopicConfiguration) bucketConfig.getConfigurationByName(expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.getTopicARN());
		assertEquals(events, snsConfig.getEvents());

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
		
		BucketNotificationConfiguration existingConfig = new BucketNotificationConfiguration();
		
		existingConfig.addConfiguration(expectedConfigName, new TopicConfiguration(expectedTopicArn, "s3:ObjectRestore:Post"));

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(anyString())).thenReturn(existingConfig);
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
		verify(mockS3Client).getBucketNotificationConfiguration(expectedBucketName);
		
		ArgumentCaptor<BucketNotificationConfiguration> argCaptor = ArgumentCaptor.forClass(BucketNotificationConfiguration.class);
		
		verify(mockS3Client).setBucketNotificationConfiguration(eq(expectedBucketName), argCaptor.capture());
		
		BucketNotificationConfiguration bucketConfig = argCaptor.getValue();
		
		assertEquals(1, bucketConfig.getConfigurations().size());
		
		TopicConfiguration snsConfig = (TopicConfiguration) bucketConfig.getConfigurationByName(expectedConfigName);
		
		assertEquals(expectedTopicArn, snsConfig.getTopicARN());
		assertEquals(events, snsConfig.getEvents());

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
		
		BucketNotificationConfiguration existingConfig = new BucketNotificationConfiguration();
		
		existingConfig.addConfiguration(expectedConfigName, new TopicConfiguration().withTopicARN(expectedTopicArn).withEvents(events));

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(anyString())).thenReturn(existingConfig);
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
		verify(mockS3Client).getBucketNotificationConfiguration(expectedBucketName);
		verify(mockS3Client, never()).setBucketNotificationConfiguration(any(), any());

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
		
		BucketNotificationConfiguration existingConfig = new BucketNotificationConfiguration();
		
		existingConfig.addConfiguration(expectedConfigName, new QueueConfiguration().withQueueARN("queueArn").withEvents(events));

		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
		when(mockS3Client.getBucketNotificationConfiguration(anyString())).thenReturn(existingConfig);

		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			builder.buildAllBuckets();
		});
		
		assertEquals("The notification configuration " + expectedConfigName + " was found but was not a TopicConfiguration", ex.getMessage());

		verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
		verify(mockS3Client).getBucketNotificationConfiguration(expectedBucketName);
		verify(mockS3Client, never()).setBucketNotificationConfiguration(any(), any());
	}
	
	@Test
	public void testBuildAllBucketsWithVirusScannerConfiguration() throws InterruptedException {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		
		bucket.setName("bucket");
		bucket.setVirusScanEnabled(true);
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_LAMBDA_VIRUS_SCANNER_ARTIFACT_URL)).thenReturn("https://some-url/lambda-name.zip");
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		S3VirusScannerConfig virusScannerConfig = new S3VirusScannerConfig();
		
		virusScannerConfig.setLambdaArtifactBucket("${stack}-lambda-bucket");
		virusScannerConfig.setNotificationEmail("notification@sagebase.org");
		
		when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);
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
		verify(mockS3Client).putObject(expectedBucket, expectedKey, mockFile);
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
		
		ArgumentCaptor<BucketNotificationConfiguration> argCaptor = ArgumentCaptor.forClass(BucketNotificationConfiguration.class);
		
		verify(mockS3Client).setBucketNotificationConfiguration(eq("bucket"), argCaptor.capture());
		
		BucketNotificationConfiguration bucketConfig = argCaptor.getValue();
		
		assertEquals(1, bucketConfig.getConfigurations().size());
		
		TopicConfiguration snsConfig = (TopicConfiguration) bucketConfig.getConfigurationByName(S3BucketBuilderImpl.VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME);
		
		assertEquals("snsTopicArn", snsConfig.getTopicARN());
		assertEquals(Collections.singleton(S3Event.ObjectCreatedByCompleteMultipartUpload.toString()), snsConfig.getEvents());

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
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		S3VirusScannerConfig virusScannerConfig = new S3VirusScannerConfig();
		
		virusScannerConfig.setLambdaArtifactBucket("${stack}-lambda-bucket");
		virusScannerConfig.setNotificationEmail("notification@sagebase.org");
		
		when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);
		when(mockDownloader.downloadFile(any())).thenReturn(new File("tmpFile"));
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
		BucketNotificationConfiguration bucketConfiguration = new BucketNotificationConfiguration();
		bucketConfiguration.addConfiguration(S3BucketBuilderImpl.VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME, new TopicConfiguration());
		
		when(mockS3Config.getVirusScannerConfig()).thenReturn(virusScannerConfig);
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		when(mockS3Client.getBucketNotificationConfiguration(any(String.class))).thenReturn(bucketConfiguration);

		String expectedBucket = stack + "-lambda-bucket";
		String expectedKey = "artifacts/virus-scanner/lambda-name.zip";
		
		// Call under test
		builder.buildAllBuckets();
		
		ArgumentCaptor<BucketNotificationConfiguration> argCaptor = ArgumentCaptor.forClass(BucketNotificationConfiguration.class);
		
		verify(mockS3Client).setBucketNotificationConfiguration(eq("bucket"), argCaptor.capture());
		
		BucketNotificationConfiguration configuration = argCaptor.getValue();
		
		// Make sure the config was removed
		assertTrue(configuration.getConfigurations().isEmpty());

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
		
		AmazonS3Exception exception = new AmazonS3Exception("Nope");
		exception.setStatusCode(404);
		
		when(mockS3Client.getPublicAccessBlock(any())).thenThrow(exception);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).getPublicAccessBlock(new GetPublicAccessBlockRequest().withBucketName(expectedBucketName));
		verify(mockS3Client).setPublicAccessBlock(new SetPublicAccessBlockRequest().withBucketName(expectedBucketName)
			.withPublicAccessBlockConfiguration(new PublicAccessBlockConfiguration()
				.withBlockPublicAcls(true)
				.withBlockPublicPolicy(true)
				.withIgnorePublicAcls(true)
				.withRestrictPublicBuckets(true)
			)
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
		when(mockS3Client.getPublicAccessBlock(any())).thenReturn(new GetPublicAccessBlockResult()
			.withPublicAccessBlockConfiguration(new PublicAccessBlockConfiguration()
				.withBlockPublicAcls(false)
				.withBlockPublicPolicy(false)
				.withIgnorePublicAcls(false)
				.withRestrictPublicBuckets(false)
			)
		);

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).getPublicAccessBlock(new GetPublicAccessBlockRequest().withBucketName(expectedBucketName));
		verify(mockS3Client, never()).setPublicAccessBlock(any());

	}
	
	private Rule allBucketRule(String ruleName) {
		return new Rule().withId(ruleName).withFilter(new LifecycleFilter(null)).withStatus(BucketLifecycleConfiguration.ENABLED);
	}
}
