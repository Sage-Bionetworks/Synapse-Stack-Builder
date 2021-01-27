package org.sagebionetworks.template.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.StringWriter;
import java.util.Arrays;

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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.GetBucketInventoryConfigurationResult;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.inventory.InventoryConfiguration;
import com.amazonaws.services.s3.model.inventory.InventoryFrequency;
import com.amazonaws.services.s3.model.inventory.InventoryS3BucketDestination;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;

@ExtendWith(MockitoExtension.class)
public class S3BucketBuilderImplTest {

	@Mock
	private RepoConfiguration mockConfig;

	@Mock
	private S3Config mockS3Config;

	@Mock
	private AmazonS3 mockS3Client;

	@Mock
	private AWSSecurityTokenService mockStsClient;

	@Mock
	private VelocityEngine mockVelocity;

	@InjectMocks
	private S3BucketBuilderImpl builder;

	@Mock
	private GetCallerIdentityResult mockGetCallerIdentityResult;
	
	@Mock
	private Template mockTemplate;

	@Captor
	private ArgumentCaptor<SetBucketEncryptionRequest> encryptionRequestCaptor;
	
	@Captor
	private ArgumentCaptor<InventoryConfiguration> inventoryConfigurationCaptor;
	
	@Captor
	private ArgumentCaptor<BucketLifecycleConfiguration> bucketLifeCycleConfigurationCaptor;
	
	@Captor
	private ArgumentCaptor<VelocityContext> velocityContextCaptor;

	private String stack;
	private String accountId;

	@BeforeEach
	public void before() {
		stack = "dev";
		accountId = "12345";
		
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(mockStsClient.getCallerIdentity(any())).thenReturn(mockGetCallerIdentityResult);
		when(mockGetCallerIdentityResult.getAccount()).thenReturn(accountId);
	}

	@Test
	public void testBuildAllBuckets() {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");

		String expectedBucketName = stack + ".bucket";
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));

		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);

		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).getBucketLifecycleConfiguration(anyString());
		verify(mockS3Client, never()).setBucketLifecycleConfiguration(any(), any());
		verify(mockS3Client, never()).setBucketPolicy(any(), any());

	}

	@Test
	public void testBuildAllBucketsNeedsEncypted() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));

		AmazonServiceException notFound = new AmazonServiceException("NotFound");
		notFound.setStatusCode(404);

		doThrow(notFound).when(mockS3Client).getBucketEncryption(anyString());

		String expectedBucketName = stack + ".bucket";

		// call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).setBucketEncryption(encryptionRequestCaptor.capture());

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
		verify(mockS3Client, never()).getBucketLifecycleConfiguration(anyString());
		verify(mockS3Client, never()).setBucketLifecycleConfiguration(any(), any());
		verify(mockS3Client, never()).setBucketPolicy(any(), any());

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
	public void testBuildAllBucketsWithInventory() {

		S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
		inventoryBucket.setName("${stack}.inventory");
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setInventoryEnabled(true);
		
		String expectedInventoryBucketName = stack + ".inventory";
		String expectedBucketName = stack + ".bucket";
		
		when(mockS3Config.getInventoryBucket()).thenReturn(inventoryBucket.getName());
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(inventoryBucket, bucket));
		
		AmazonServiceException notFound = new AmazonServiceException("NotFound");
		notFound.setStatusCode(404);

		// No inventory configuration set
		doThrow(notFound).when(mockS3Client).getBucketInventoryConfiguration(anyString(), anyString());
		
		when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);
		
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				StringWriter writer = (StringWriter) invocation.getArgument(1);
				writer.append("fakeJsonPolicy");
				return null;
			}
		}).when(mockTemplate).merge(any(), any());
		
		
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
		assertEquals(S3BucketBuilderImpl.INVENTORY_PREFIX, destination.getPrefix());
		assertEquals(accountId, destination.getAccountId());
		assertEquals(S3BucketBuilderImpl.INVENTORY_FORMAT, destination.getFormat());
		
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).getBucketLifecycleConfiguration(anyString());
		verify(mockS3Client, never()).setBucketLifecycleConfiguration(any(), any());
		
		verify(mockTemplate).merge(velocityContextCaptor.capture(), any());
		
		VelocityContext context = velocityContextCaptor.getValue();
		
		assertEquals(stack, context.get("stack"));
		assertEquals(accountId, context.get("accountId"));
		assertEquals(expectedInventoryBucketName, context.get("inventoryBucket"));
		assertEquals(Arrays.asList(expectedBucketName), context.get("sourceBuckets"));
		
		verify(mockS3Client).setBucketPolicy(expectedInventoryBucketName, "fakeJsonPolicy");

	}
	
	@Test
	public void testBuildAllBucketsWithInventoryAndExisting() {

		S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
		inventoryBucket.setName("${stack}.inventory");
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setInventoryEnabled(true);
		
		String expectedInventoryBucketName = stack + ".inventory";
		String expectedBucketName = stack + ".bucket";
		
		when(mockS3Config.getInventoryBucket()).thenReturn(inventoryBucket.getName());
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(inventoryBucket, bucket));
		
		// Mimics an existing configuration that is enabled
		when(mockS3Client.getBucketInventoryConfiguration(anyString(), anyString())).thenReturn(
				new GetBucketInventoryConfigurationResult().withInventoryConfiguration(
						new InventoryConfiguration()
						.withEnabled(true))
		);
		
		when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedInventoryBucketName);
		verify(mockS3Client).createBucket(expectedBucketName);
		
		verify(mockS3Client).getBucketEncryption(expectedInventoryBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		
		verify(mockS3Client).getBucketInventoryConfiguration(expectedBucketName, S3BucketBuilderImpl.INVENTORY_ID);
		
		verify(mockS3Client, never()).setBucketEncryption(any());

		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		verify(mockS3Client, never()).deleteBucketInventoryConfiguration(expectedBucketName, S3BucketBuilderImpl.INVENTORY_ID);


	}
	
	@Test
	public void testBuildAllBucketsWithDisabledInventoryAndNonExisting() {

		S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
		inventoryBucket.setName("${stack}.inventory");
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setInventoryEnabled(false);
		
		String expectedInventoryBucketName = stack + ".inventory";
		String expectedBucketName = stack + ".bucket";
		
		when(mockS3Config.getInventoryBucket()).thenReturn(inventoryBucket.getName());
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(inventoryBucket, bucket));
		
		AmazonServiceException notFound = new AmazonServiceException("NotFound");
		notFound.setStatusCode(404);

		// No inventory configuration set
		doThrow(notFound).when(mockS3Client).getBucketInventoryConfiguration(anyString(), anyString());
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedInventoryBucketName);
		verify(mockS3Client).createBucket(expectedBucketName);
		
		verify(mockS3Client).getBucketEncryption(expectedInventoryBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		
		verify(mockS3Client).getBucketInventoryConfiguration(expectedBucketName, S3BucketBuilderImpl.INVENTORY_ID);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		// No bucket was setup to have the inventory, delete the bucket policy
		verify(mockS3Client).deleteBucketPolicy(expectedInventoryBucketName);

	}
	
	@Test
	public void testBuildAllBucketsWithDisabledInventoryAndExisting() {

		S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
		inventoryBucket.setName("${stack}.inventory");
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setInventoryEnabled(false);
		
		String expectedInventoryBucketName = stack + ".inventory";
		String expectedBucketName = stack + ".bucket";
		
		when(mockS3Config.getInventoryBucket()).thenReturn(inventoryBucket.getName());
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(inventoryBucket, bucket));
		
		// Mimics an existing configuration that is enabled
		when(mockS3Client.getBucketInventoryConfiguration(anyString(), anyString())).thenReturn(
				new GetBucketInventoryConfigurationResult().withInventoryConfiguration(
						new InventoryConfiguration()
						.withEnabled(true))
		);
		
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
		// No bucket was setup to have the inventory, delete the bucket policy
		verify(mockS3Client).deleteBucketPolicy(expectedInventoryBucketName);

	}
	
	@Test
	public void testBuildAllBucketsWithRetentionDays() {

		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setRetentionDays(30);
		
		String expectedBucketName = stack + ".bucket";
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client).setBucketLifecycleConfiguration(eq(expectedBucketName), bucketLifeCycleConfigurationCaptor.capture());
		
		BucketLifecycleConfiguration config = bucketLifeCycleConfigurationCaptor.getValue();
		
		assertEquals(1, config.getRules().size());

		Rule rule = config.getRules().get(0);
		
		assertEquals(S3BucketBuilderImpl.RETENTION_RULE_ID, rule.getId());
		assertEquals(bucket.getRetentionDays(), rule.getExpirationInDays());
		assertEquals(BucketLifecycleConfiguration.ENABLED, rule.getStatus());

	}
	
	@Test
	public void testBuildAllBucketsWithRetentionDaysAndExistingLifeCycle() {

		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("${stack}.bucket");
		bucket.setRetentionDays(30);
		
		String expectedBucketName = stack + ".bucket";
		
		when(mockS3Config.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		// Mimics an existing life cycle set on the bucket already
		when(mockS3Client.getBucketLifecycleConfiguration(anyString())).thenReturn(new BucketLifecycleConfiguration());
		
		// Call under test
		builder.buildAllBuckets();

		verify(mockS3Client).createBucket(expectedBucketName);
		verify(mockS3Client).getBucketEncryption(expectedBucketName);
		verify(mockS3Client).getBucketLifecycleConfiguration(expectedBucketName);
		
		verify(mockS3Client, never()).setBucketEncryption(any());
		verify(mockS3Client, never()).setBucketInventoryConfiguration(any(), any());
		
		verify(mockS3Client, never()).setBucketLifecycleConfiguration(any(), any());
	}
}
