package org.sagebionetworks.template.s3;

import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_INVENTORY_BUCKET_POLICY_TEMPLATE;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortIncompleteMultipartUpload;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Transition;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.NotificationConfiguration;
import com.amazonaws.services.s3.model.S3Event;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.TopicConfiguration;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringAccessTier;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringConfiguration;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringFilter;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringStatus;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringTagPredicate;
import com.amazonaws.services.s3.model.intelligenttiering.Tiering;
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
	
	static final String INT_ARCHIVE_ID = "intArchiveAccessConfiguration";
	
	static final String CF_OUTPUT_VIRUS_TRIGGER_TOPIC = "ScanTriggerSNSTopic";
	
	static final String CF_PROPERTY_BUCKETS = "buckets";
	static final String CF_PROPERTY_LAMBDA_BUCKET = "lambdaBucket";
	static final String CF_PROPERTY_LAMBDA_KEY = "lambdaKey";
	static final String CF_PROPERTY_NOTIFICATION_EMAIL = "notificationEmail";
	
	static final String VIRUS_SCANNER_STACK_NAME = "${stack}-synapse-virus-scanner";
	static final String VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME = "virusScannerNotificationConfiguration";
	
	private AmazonS3 s3Client;
	private AWSSecurityTokenService stsClient;
	private RepoConfiguration config;
	private S3Config s3Config;
	private VelocityEngine velocity;
	private CloudFormationClient cloudFormationClient;
	private StackTagsProvider tagsProvider;
	private ArtifactDownload downloader;
	
	@Inject
	public S3BucketBuilderImpl(AmazonS3 s3Client, AWSSecurityTokenService stsClient, RepoConfiguration config, S3Config s3Config, VelocityEngine velocity, CloudFormationClient cloudFormationClient, StackTagsProvider tagsProvider, ArtifactDownload downloader) {
		this.s3Client = s3Client;
		this.stsClient = stsClient;
		this.config = config;
		this.s3Config = s3Config;
		this.velocity = velocity;
		this.cloudFormationClient = cloudFormationClient;
		this.tagsProvider = tagsProvider;
		this.downloader = downloader;
	}

	@Override
	public void buildAllBuckets() {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		
		String accountId = stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getAccount();
		
		String inventoryBucket = TemplateUtils.replaceStackVariable(s3Config.getInventoryBucket(), stack);
		
		List<String> inventoriedBuckets = new ArrayList<>();
				
		List<String> virusScanEnabledBuckets = new ArrayList<>();
		
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
			configureIntelligentTieringArchive(bucket);
			configureBucketNotifications(bucket, stack);
			
			if (bucket.isInventoryEnabled()) {
				inventoriedBuckets.add(bucket.getName());
			}
			
			if (bucket.isVirusScanEnabled()) {
				virusScanEnabledBuckets.add(bucket.getName());
			}
			
		}
		
		// Makes sure the bucket policy on the inventory is correct
		configureInventoryBucketPolicy(stack, accountId, inventoryBucket, inventoriedBuckets);
		
		buildVirusScannerStack(stack, s3Config.getVirusScannerConfig(), virusScanEnabledBuckets).ifPresent( virusScannerStack -> {
			// Once the virus scanner stack is built we need to setup the each bucket notification, this cannot be done in the cloud formation
			// template due to a known circular dependency
			String virusScannerTopicArn = virusScannerStack.getOutputs().stream()
				.filter( output -> output.getOutputKey().equals(CF_OUTPUT_VIRUS_TRIGGER_TOPIC))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Could not find SNS topic output from virus scanner template"))
				.getOutputValue();
			
			virusScanEnabledBuckets.forEach( bucket -> {
				configureBucketNotification(bucket, VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME, virusScannerTopicArn, Collections.singleton(S3Event.ObjectCreatedByCompleteMultipartUpload.toString()));
			});
		});
		
		
	}
	
	private Optional<Stack>buildVirusScannerStack(String stack, S3VirusScannerConfig config, List<String> buckets) {
		
		if (config == null) {
			return Optional.empty();
		}
		
		if (buckets.isEmpty()) {
			return Optional.empty();
		}
		
		String lambdaArtifactBucket = TemplateUtils.replaceStackVariable(config.getLambdaArtifactBucket(), stack);
		String lambdaArtifactKey = TemplateUtils.replaceStackVariable(config.getLambdaArtifactKey(), stack);
		
		File artifact = downloader.downloadFile(config.getLambdaArtifactSourceUrl());
		
		try {
			s3Client.putObject(lambdaArtifactBucket, lambdaArtifactKey, artifact);
		} finally {
			artifact.delete();
		}
		
		VelocityContext context = new VelocityContext();
		
		context.put(Constants.STACK, stack);
		context.put(CF_PROPERTY_BUCKETS, buckets);
		context.put(CF_PROPERTY_NOTIFICATION_EMAIL, config.getNotificationEmail());
		context.put(CF_PROPERTY_LAMBDA_BUCKET, lambdaArtifactBucket);
		context.put(CF_PROPERTY_LAMBDA_KEY, lambdaArtifactKey);
		
		// Merge the context with the template
		Template template = velocity.getTemplate(Constants.TEMPLATE_S3_VIRUS_SCANNER);
		
		StringWriter stringWriter = new StringWriter();
		
		template.merge(context, stringWriter);
		
		String resultJSON = new JSONObject(stringWriter.toString()).toString(5);
		
		String stackName = TemplateUtils.replaceStackVariable(VIRUS_SCANNER_STACK_NAME, stack);
		
		cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest()
				.withStackName(stackName)
				.withTemplateBody(resultJSON)
				.withTags(tagsProvider.getStackTags())
				.withCapabilities(CAPABILITY_NAMED_IAM));
		
		try {
			cloudFormationClient.waitForStackToComplete(stackName);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		return Optional.of(cloudFormationClient.describeStack(stackName));
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
			if (addOrUpdateRule(rules, bucket.getName(), RULE_ID_RETENTION, bucket, this::createRetentionRule, this::updateRetentionRule)) {
				update = true;
			}
		}

		if (bucket.getStorageClassTransitions() != null) {
			for (S3BucketClassTransition transition : bucket.getStorageClassTransitions()) {
				String transitionRuleName = transition.getStorageClass().name() + RULE_ID_CLASS_TRANSITION;

				if (addOrUpdateRule(rules, bucket.getName(), transitionRuleName, transition, this::createClassTransitionRule, this::updateClassTransitionRule)) {
					update = true;
				}
			}
		}
		
		// Always checks for a default multipart upload cleanup rule
		if (addOrUpdateRule(rules, bucket.getName(), RULE_ID_ABORT_MULTIPART_UPLOADS, bucket, this::createAbortMultipartRule, this::updateAbortMultipartRule)) {
			update = true;
		}
		
		if (!rules.isEmpty() && update) {
			config.setRules(rules);
			
			LOG.info("Updating bucket {} lifecycle, rules: ", bucket.getName());
			
			for (Rule rule : rules) {
				LOG.info("	{}", rule.getId());
			}
			
			s3Client.setBucketLifecycleConfiguration(bucket.getName(), config);
		}
		
	}
	
	void configureIntelligentTieringArchive(S3BucketDescriptor bucket) {
		
		if (bucket.getIntArchiveConfiguration() == null) {
			return;
		}
				
		IntelligentTieringConfiguration intConfig;
		
		try {
			intConfig = s3Client.getBucketIntelligentTieringConfiguration(bucket.getName(), INT_ARCHIVE_ID).getIntelligentTieringConfiguration();
		} catch (AmazonS3Exception e) {
			if (404 == e.getStatusCode() && "NoSuchConfiguration".equals(e.getErrorCode())) {
				intConfig = null;
			} else {
				throw e;
			}
		}
		
		if (intConfig != null) {
			LOG.warn("The {} intelligent tiering configuration already exists for bucket {}, will not update.", INT_ARCHIVE_ID, bucket.getName());
			return;
		}
		
		intConfig = createIntArchiveConfiguration(bucket.getIntArchiveConfiguration());
		
		LOG.info("Setting {} intelligent tiering configuration on bucket {}.", INT_ARCHIVE_ID, bucket.getName());
		
		s3Client.setBucketIntelligentTieringConfiguration(bucket.getName(), intConfig);
		
	}
	
	private IntelligentTieringConfiguration createIntArchiveConfiguration(S3IntArchiveConfiguration config) {
		IntelligentTieringConfiguration intConfig = new IntelligentTieringConfiguration().withId(INT_ARCHIVE_ID);
		intConfig.withStatus(IntelligentTieringStatus.Enabled);

		List<Tiering> tiers = new ArrayList<>();
		
		if (config.getArchiveAccessDays() != null) {
			tiers.add(new Tiering().withIntelligentTieringAccessTier(IntelligentTieringAccessTier.ARCHIVE_ACCESS).withDays(config.getArchiveAccessDays()));
		}
		
		if (config.getDeepArchiveAccessDays() != null) {
			tiers.add(new Tiering().withIntelligentTieringAccessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS).withDays(config.getDeepArchiveAccessDays()));
		}
		
		intConfig.withTierings(tiers);
		
		IntelligentTieringFilter filter = new IntelligentTieringFilter();

		if (config.getTagFilter() != null) {
			filter.withPredicate(new IntelligentTieringTagPredicate(new Tag(config.getTagFilter().getName(), config.getTagFilter().getValue())));
		}
		
		intConfig.setFilter(filter);
		
		return intConfig;
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
		
		boolean updateLifecycle = false;
		
		if (rule.isPresent()) {
			Rule existingRule = rule.get().withPrefix(null);
			
			updateLifecycle = ruleUpdate.apply(existingRule, definition);
			
			LOG.info("The {} rule was found on bucket {} and was {}", ruleName, bucket, updateLifecycle ? "outdated, will update." : "up to date.");
		} else {
			Rule newRule = ruleCreator.apply(definition).withId(ruleName).withStatus(BucketLifecycleConfiguration.ENABLED).withPrefix(null);
			
			rules.add(newRule);
			
			LOG.info("The {} rule was not found on bucket {}, will be added.", ruleName, bucket);
			
			updateLifecycle = true;
		}
		
		return updateLifecycle;
		
	}
	
	private static Optional<Rule> findRule(String ruleName, List<Rule> rules) {
		return rules.stream().filter(rule -> rule.getId().equals(ruleName)).findFirst();
	}
	
	private void configureBucketNotifications(S3BucketDescriptor bucket, String stack) {
		if (bucket.getNotificationsConfiguration() == null) {
			return;
		}
		
		S3NotificationsConfiguration config = bucket.getNotificationsConfiguration();
		
		String globalStackName = String.format(GLOBAL_RESOURCES_STACK_NAME_FORMAT, stack);
		
		String topicArn = cloudFormationClient.getOutput(globalStackName, config.getTopic());
		
		String configName = config.getTopic() + "Configuration";
		
		configureBucketNotification(bucket.getName(), configName, topicArn, config.getEvents());
		
	}
	
	private void configureBucketNotification(String bucketName, String configName, String topicArn, Set<String> events) {
		
		BucketNotificationConfiguration bucketConfig = s3Client.getBucketNotificationConfiguration(bucketName);
		
		boolean update = false;
		
		if (bucketConfig == null || bucketConfig.getConfigurations() == null || bucketConfig.getConfigurations().isEmpty()) {
			bucketConfig = new BucketNotificationConfiguration();
			update = true;
		}
		
		NotificationConfiguration notificationConfig = bucketConfig.getConfigurationByName(configName);
		
		if (notificationConfig == null) {
			notificationConfig = new TopicConfiguration(topicArn, events.toArray(new String[events.size()]));
			bucketConfig.addConfiguration(configName, notificationConfig);
			update = true;
		}
		
		if (notificationConfig instanceof TopicConfiguration) {
			TopicConfiguration topicConfig = (TopicConfiguration) notificationConfig;
			
			if (!topicConfig.getTopicARN().equals(topicArn)) {
				topicConfig.setTopicARN(topicArn);
				update = true;
			}
			
			if (!topicConfig.getEvents().equals(events)) {
				topicConfig.setEvents(events);
				update = true;
			}
		} else {
			throw new IllegalStateException("The notification configuration " + configName + " was found but was not a TopicConfiguration");
		}
		
		if (update) {
			LOG.info("Updating {} bucket notification configuration {}.", bucketName, configName);
			s3Client.setBucketNotificationConfiguration(bucketName, bucketConfig);
		} else {
			LOG.info("The {} bucket notification configuration {} was up to date.", bucketName, configName);
		}
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
