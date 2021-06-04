package org.sagebionetworks.template.s3;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_INVENTORY_BUCKET_POLICY_TEMPLATE;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortIncompleteMultipartUpload;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Transition;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.inventory.InventoryConfiguration;
import com.amazonaws.services.s3.model.inventory.InventoryDestination;
import com.amazonaws.services.s3.model.inventory.InventoryFrequency;
import com.amazonaws.services.s3.model.inventory.InventoryIncludedObjectVersions;
import com.amazonaws.services.s3.model.inventory.InventoryS3BucketDestination;
import com.amazonaws.services.s3.model.inventory.InventorySchedule;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.google.inject.Inject;

public class S3BucketBuilderImpl implements S3BucketBuilder {

	private static final Logger LOG = LogManager.getLogger(S3BucketBuilderImpl.class);

	static final String INVENTORY_ID = "defaultInventory";
	static final String INVENTORY_FORMAT = "Parquet";
	static final String INVENTORY_PREFIX = "inventory";
	static final List<String> INVENTORY_FIELDS = Arrays.asList(
			"Size", "LastModifiedDate", "ETag", "IsMultipartUploaded"
	);
	
	static final String RULE_ID_RETENTION = "retentionRule";
	static final String RULE_ID_CLASS_TRANSITION = "ClassTransitionRule";
	static final String RULE_ID_ABORT_MULTIPART_UPLOADS = "abortMultipartUploadsRule";
	static final int ABORT_MULTIPART_UPLOAD_DAYS = 60;
	
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
			
			bucket.setName(TemplateUtils.replaceStackVariable(bucket.getName(), stack));
			
			if (bucket.isDevOnly() && stack.equalsIgnoreCase(Constants.PROD_STACK_NAME)) {
				LOG.warn("The bucket {} is deployed only on non-prod stacks.", bucket.getName());
				continue;
			}
			
			createBucket(bucket.getName());
			configureEncryption(bucket.getName());	
			configureInventory(bucket.getName(), accountId, inventoryBucket, bucket.isInventoryEnabled());
			configureBucketLifeCycle(bucket);
			
			if (bucket.isInventoryEnabled()) {
				inventoriedBuckets.add(bucket.getName());
			}
			
		}
		
		// Makes sure the bucket policy on the inventory is correct
		configureInventoryBucketPolicy(stack, accountId, inventoryBucket, inventoriedBuckets);
	}
	
	private void createBucket(String bucketName) {
		LOG.info("Creating bucket: {}.", bucketName);
		
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
				LOG.info("Setting server side encryption for bucket: {}.", bucketName);
				
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
			LOG.warn("An inventory configuration for bucket {} exists already, will not update.", bucketName);
		} else {
			LOG.info("Removing inventory configuration for bucket {}.", bucketName);
			s3Client.deleteBucketInventoryConfiguration(bucketName, INVENTORY_ID);
		}
		
	}
	
	private void setInventoryConfiguration(String bucketName, String accountId, String inventoryBucket) {
		LOG.info("Configuring inventory for bucket: {}.", bucketName);
		
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
	
	private void configureBucketLifeCycle(S3BucketDescriptor bucket) {
		
		// Returns null if no life cycle configuration was found
		BucketLifecycleConfiguration config = s3Client.getBucketLifecycleConfiguration(bucket.getName());
		
		if (config == null) {
			config = new BucketLifecycleConfiguration();
		}
		
		boolean update = false;
		
		List<Rule> rules = config.getRules() == null ? new ArrayList<>() : new ArrayList<>(config.getRules());

		if (bucket.getRetentionDays() != null) {
			update = addOrUpdateRule(rules, bucket.getName(), RULE_ID_RETENTION, bucket, this::createRetentionRule, this::updateRetentionRule);
		}

		if (bucket.getStorageClassTransitions() != null) {
			for (S3BucketClassTransition transition : bucket.getStorageClassTransitions()) {
				String transitionRuleName = transition.getStorageClass().name() + RULE_ID_CLASS_TRANSITION;

				update |= addOrUpdateRule(rules, bucket.getName(), transitionRuleName, transition, this::createClassTransitionRule, this::updateClassTransitionRule);
			}
		}
		
		// Always checks for a default multipart upload cleanup rule
		update |= addOrUpdateRule(rules, bucket.getName(), RULE_ID_ABORT_MULTIPART_UPLOADS, bucket, this::createAbortMultipartRule, this::updateAbortMultipartRule);
		
		if (!rules.isEmpty() && update) {
			config.setRules(rules);
			
			LOG.info("Updating bucket {} lifecycle, rules: ", bucket.getName());
			
			for (Rule rule : rules) {
				LOG.info("	{}", rule.getId());
			}
			
			s3Client.setBucketLifecycleConfiguration(bucket.getName(), config);
		}
		
	}
	
	private Rule createAbortMultipartRule(S3BucketDescriptor bucket) {
		return new Rule()
				.withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(ABORT_MULTIPART_UPLOAD_DAYS))
				.withFilter(allBucketLifecycletFilter());
	}
	
	private boolean updateAbortMultipartRule(Rule rule, S3BucketDescriptor bucket) {
		if (rule.getAbortIncompleteMultipartUpload() == null || ABORT_MULTIPART_UPLOAD_DAYS != rule.getAbortIncompleteMultipartUpload().getDaysAfterInitiation() || rule.getFilter() == null) {
			rule.withAbortIncompleteMultipartUpload(new AbortIncompleteMultipartUpload().withDaysAfterInitiation(ABORT_MULTIPART_UPLOAD_DAYS)).withFilter(allBucketLifecycletFilter());
			return true;
		} else {
			return false;
		}
	}
	
	private Rule createRetentionRule(S3BucketDescriptor bucket) {
		return new Rule().withExpirationInDays(bucket.getRetentionDays()).withFilter(allBucketLifecycletFilter());
	}
	
	private boolean updateRetentionRule(Rule rule, S3BucketDescriptor bucket) {
		if (!bucket.getRetentionDays().equals(rule.getExpirationInDays()) || rule.getFilter() == null) {
			rule.withExpirationInDays(bucket.getRetentionDays())
				.withFilter(allBucketLifecycletFilter());
			return true;
		} else {
			return false;
		}
	}
	
	private Rule createClassTransitionRule(S3BucketClassTransition transition) {
		return new Rule()
			.addTransition(new Transition().withStorageClass(transition.getStorageClass()).withDays(transition.getDays()))
			.withFilter(allBucketLifecycletFilter());
	}
	
	private boolean updateClassTransitionRule(Rule rule, S3BucketClassTransition transition) {
		Transition existingTransition = null;
		
		if (rule.getTransitions() != null && !rule.getTransitions().isEmpty()) {
			existingTransition = rule.getTransitions().get(0);
		} else {
			existingTransition = new Transition();
			rule.addTransition(existingTransition);
		}
		
		if (!transition.getStorageClass().toString().equals(existingTransition.getStorageClassAsString()) || !transition.getDays().equals(existingTransition.getDays()) || rule.getFilter() == null) {
			existingTransition.withStorageClass(transition.getStorageClass()).withDays(transition.getDays());
			rule.withFilter(allBucketLifecycletFilter());
			return true;
		} else {
			return false;
		}
	}
	
	private static LifecycleFilter allBucketLifecycletFilter() {
		return new LifecycleFilter(null);
	}
	
	private static <T> boolean addOrUpdateRule(List<Rule> rules, String bucket, String ruleName, T definition, Function<T, Rule> ruleCreator, BiFunction<Rule, T, Boolean> ruleUpdate) {
		Optional<Rule> rule = findRule(ruleName, rules);
		
		if (rule.isPresent()) {
			LOG.info("The {} rule was found on bucket {}", ruleName, bucket);
			Rule existingRule = rule.get().withPrefix(null);
			return ruleUpdate.apply(existingRule, definition);
		} else {
			Rule newRule = ruleCreator.apply(definition).withId(ruleName).withStatus(BucketLifecycleConfiguration.ENABLED).withPrefix(null);
			rules.add(newRule);
			return true;
		}
		
	}
	
	private static Optional<Rule> findRule(String ruleName, List<Rule> rules) {
		return rules.stream().filter(rule -> rule.getId().equals(ruleName)).findFirst();
	}
	
	private void configureInventoryBucketPolicy(String stack, String accountId, String inventoryBucket, List<String> sourceBuckets) {
		if (inventoryBucket == null) {
			LOG.warn("An inventory bucket was not specified.");
			return;
		} 
		
		if (sourceBuckets == null || sourceBuckets.isEmpty()) {
			LOG.warn("No bucket had the inventory enabled, removing bucket policy.");
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
		
		LOG.info("Updating inventory bucket {} policy.", inventoryBucket);
		
		s3Client.setBucketPolicy(inventoryBucket, jsonPolicy);
	}

}
