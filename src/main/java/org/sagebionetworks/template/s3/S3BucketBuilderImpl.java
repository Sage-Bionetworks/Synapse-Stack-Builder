package org.sagebionetworks.template.s3;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_INVENTORY_BUCKET_POLICY_TEMPLATE;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.inventory.InventoryConfiguration;
import com.amazonaws.services.s3.model.inventory.InventoryDestination;
import com.amazonaws.services.s3.model.inventory.InventoryFrequency;
import com.amazonaws.services.s3.model.inventory.InventoryIncludedObjectVersions;
import com.amazonaws.services.s3.model.inventory.InventoryS3BucketDestination;
import com.amazonaws.services.s3.model.inventory.InventorySchedule;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.google.inject.Inject;

public class S3BucketBuilderImpl implements S3BucketBuilder {

	private static final Logger logger = LogManager.getLogger(S3BucketBuilderImpl.class);

	static final String INVENTORY_ID = "defaultInventory";
	static final String INVENTORY_FORMAT = "Parquet";
	static final String INVENTORY_PREFIX = "inventory";
	static final List<String> INVENTORY_FIELDS = Arrays.asList(
			"Size", "LastModifiedDate", "ETag", "IsMultipartUploaded"
	);
	static final String RETENTION_RULE_ID = "retentionRule";
	
	private AmazonS3 s3Client;
	private AWSSecurityTokenService stsClient;
	private RepoConfiguration config;
	private S3Config s3Config;
	private VelocityEngine velocity;
	
	@Inject
	public S3BucketBuilderImpl(AmazonS3 s3Client, AWSSecurityTokenService stsClient, RepoConfiguration config, S3Config s3Config, VelocityEngine velocity) {
		this.s3Client = s3Client;
		this.stsClient = stsClient;
		this.config = config;
		this.s3Config = s3Config;
		this.velocity = velocity;
	}

	@Override
	public void buildAllBuckets() {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		
		String accountId = stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getAccount();
		
		String inventoryBucket = TemplateUtils.replaceStackVariable(s3Config.getInventoryBucket(), stack);
		List<String> inventoriedBuckets = new ArrayList<>();
		
		// Configure all buckets first
		for (S3BucketDescriptor bucket : s3Config.getBuckets()) {
			String bucketName = TemplateUtils.replaceStackVariable(bucket.getName(), stack);
			
			createBucket(bucketName);
			configureEncryption(bucketName);	
			configureInventory(bucketName, accountId, inventoryBucket, bucket.isInventoryEnabled());
			configureBucketLifeCycle(bucketName, bucket.getRetentionDays());
			
			if (bucket.isInventoryEnabled()) {
				inventoriedBuckets.add(bucketName);
			}
			
		}
		
		// Makes sure the bucket policy on the inventory is correct
		configureInventoryBucketPolicy(stack, accountId, inventoryBucket, inventoriedBuckets);
	}
	
	private void createBucket(String bucketName) {
		logger.info("Creating bucket: {}.", bucketName);
		
		// This is idempotent
		s3Client.createBucket(bucketName);
	}
	
	private void configureEncryption(String bucketName) {
		try {
			// If server side encryption is not currently set this call with throw a 404
			s3Client.getBucketEncryption(bucketName);
		} catch (AmazonServiceException e) {
			if(e.getStatusCode() == 404) {
				// The bucket is not currently encrypted so configure it for encryption.
				logger.info("Setting server side encryption for bucket: {}.", bucketName);
				
				s3Client.setBucketEncryption(new SetBucketEncryptionRequest().withBucketName(bucketName)
						.withServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration()
								.withRules(new ServerSideEncryptionRule().withApplyServerSideEncryptionByDefault(
										new ServerSideEncryptionByDefault().withSSEAlgorithm(SSEAlgorithm.AES256)))));
			} else {
				throw e;
			}
		} 
	}
	
	private void configureInventory(String bucketName, String accountId, String inventoryBucket, boolean enabled) {
		if (inventoryBucket == null) {
			return;
		}
		
		try {
			s3Client.getBucketInventoryConfiguration(bucketName, INVENTORY_ID);
		} catch (AmazonServiceException e) {
			if (e.getStatusCode() == 404) {
				// If the inventory was disabled and does not exists, we do not add a configuration
				if (enabled) {
					setInventoryConfiguration(bucketName, accountId, inventoryBucket);
				}
				return;
			} else {
				throw e;
			}
		}
		
		if (enabled) {
			logger.warn("An inventory configuration for bucket {} exists already, will not update.", bucketName);
		} else {
			logger.info("Removing inventory configuration for bucket {}.", bucketName);
			s3Client.deleteBucketInventoryConfiguration(bucketName, INVENTORY_ID);
		}
		
	}
	
	private void setInventoryConfiguration(String bucketName, String accountId, String inventoryBucket) {
		logger.info("Configuring inventory for bucket: {}.", bucketName);
		
		InventoryConfiguration config = new InventoryConfiguration()
				.withId(INVENTORY_ID)
				.withDestination(
						new InventoryDestination()
							.withS3BucketDestination(
									new InventoryS3BucketDestination()
										.withBucketArn("arn:aws:s3:::" + inventoryBucket)
										.withAccountId(accountId)
										.withPrefix(INVENTORY_PREFIX)
										.withFormat(INVENTORY_FORMAT)
							)
				)
				.withOptionalFields(INVENTORY_FIELDS)
				.withSchedule(new InventorySchedule().withFrequency(InventoryFrequency.Weekly))
				.withEnabled(true)
				.withIncludedObjectVersions(InventoryIncludedObjectVersions.All);
		
		s3Client.setBucketInventoryConfiguration(bucketName, config);
	}
	
	private void configureBucketLifeCycle(String bucketName, Integer retentionDays) {
		if (retentionDays == null) {
			return;
		}
		
		// Returns null if no life cycle configuration was found
		BucketLifecycleConfiguration config = s3Client.getBucketLifecycleConfiguration(bucketName);
		
		if (config != null) {
			logger.warn("A bucket lifecycle configuration for bucket {} already exists, will not update.", bucketName);
			return;
		}
		
		config = new BucketLifecycleConfiguration()
				.withRules(new BucketLifecycleConfiguration.Rule()
						.withId(RETENTION_RULE_ID)
						.withExpirationInDays(retentionDays)
						.withStatus(BucketLifecycleConfiguration.ENABLED));
		
		logger.info("Configuring bucket {} lifecycle with {} days of retention.", bucketName, retentionDays);
		
		s3Client.setBucketLifecycleConfiguration(bucketName, config);
		
	}
	
	private void configureInventoryBucketPolicy(String stack, String accountId, String inventoryBucket, List<String> sourceBuckets) {
		if (inventoryBucket == null) {
			logger.warn("An inventory bucket was not specified.");
			return;
		} 
		
		if (sourceBuckets == null || sourceBuckets.isEmpty()) {
			logger.warn("No bucket had the inventory enabled, removing bucket policy.");
			s3Client.deleteBucketPolicy(inventoryBucket);
			return;
		}
		
		updateInventoryBucketPolicy(stack, accountId, inventoryBucket, sourceBuckets);
		
	}
	
	private void updateInventoryBucketPolicy(String stack, String accountId, String inventoryBucket, List<String> sourceBuckets) {
		VelocityContext context = new VelocityContext();
		
		context.put("stack", stack);
		context.put("accountId", accountId);
		context.put("inventoryBucket", inventoryBucket);
		context.put("sourceBuckets", sourceBuckets);
		
		Template policyTemplate = velocity.getTemplate(TEMPLATE_INVENTORY_BUCKET_POLICY_TEMPLATE, StandardCharsets.UTF_8.name());
		
		StringWriter stringWriter = new StringWriter();
		
		policyTemplate.merge(context, stringWriter);
		
		String jsonPolicy = stringWriter.toString();
		
		logger.info("Updating inventory bucket {} policy.", inventoryBucket);
		
		s3Client.setBucketPolicy(inventoryBucket, jsonPolicy);
	}

}
