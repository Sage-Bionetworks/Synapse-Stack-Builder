package org.sagebionetworks.template.s3;

import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_VIRUS_SCANNER_ARTIFACT_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.StringWriter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import com.google.inject.Inject;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

public class S3BucketBuilderImpl implements S3BucketBuilder {

	private static final Logger LOG = LogManager.getLogger(S3BucketBuilderImpl.class);

	static final String INVENTORY_ID = "defaultInventory";
	static final List<InventoryOptionalField> INVENTORY_FIELDS = Arrays.asList(
			InventoryOptionalField.SIZE,
			InventoryOptionalField.LAST_MODIFIED_DATE,
			InventoryOptionalField.E_TAG,
			InventoryOptionalField.IS_MULTIPART_UPLOADED,
			InventoryOptionalField.STORAGE_CLASS,
			InventoryOptionalField.INTELLIGENT_TIERING_ACCESS_TIER,
			InventoryOptionalField.ENCRYPTION_STATUS,
			InventoryOptionalField.OBJECT_OWNER
	);
	
	static final String RULE_ID_RETENTION = "retentionRule";
	static final String RULE_ID_CLASS_TRANSITION = "ClassTransitionRule";
	static final String RULE_ID_ABORT_MULTIPART_UPLOADS = "abortMultipartUploadsRule";
	static final int ABORT_MULTIPART_UPLOAD_DAYS = 60;
	
	static final String INT_ARCHIVE_ID = "intArchiveAccessConfiguration";
	
	static final String CF_OUTPUT_VIRUS_TRIGGER_TOPIC = "ScanTriggerSNSTopic";
	static final String CF_OUTPUT_VIRUS_UPDATER_LAMBDA = "VirusScanDefinitionUpdaterLambda";
	
	static final String CF_PROPERTY_BUCKETS = "buckets";
	static final String CF_PROPERTY_LAMBDA_BUCKET = "lambdaBucket";
	static final String CF_PROPERTY_LAMBDA_KEY = "lambdaKey";
	static final String CF_PROPERTY_NOTIFICATION_EMAIL = "notificationEmail";
	
	static final String VIRUS_SCANNER_STACK_NAME = "${stack}-synapse-virus-scanner";
	static final String VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME = "virusScannerNotificationConfiguration";
	static final String VIRUS_SCANNER_KEY_TEMPLATE = "artifacts/virus-scanner/%s";
	static final String BUCKET_POLICY_STACK_NAME = "${stack}-synapse-bucket-policies";
	

	private static String getStackOutput(Stack stack, String key) {
		return stack.outputs().stream()
		.filter( output -> output.outputKey().equals(key))
		.findFirst()
		.orElseThrow(() -> new IllegalStateException("Could not find " + key + " output from stack " + stack.stackName()))
		.outputValue();
	}
	
	private S3Client s3Client;
	private StsClient stsClient;
	private LambdaClient lambdaClient;
	private RepoConfiguration config;
	private S3Config s3Config;
	private VelocityEngine velocity;
	private CloudFormationClientWrapper cloudFormationClientWrapper;
	private StackTagsProvider tagsProvider;
	private ArtifactDownload downloader;
	
	@Inject
	public S3BucketBuilderImpl(S3Client s3Client, StsClient stsClient, LambdaClient lambdaClient, RepoConfiguration config, S3Config s3Config, VelocityEngine velocity, CloudFormationClientWrapper cloudFormationClientWrapper, StackTagsProvider tagsProvider, ArtifactDownload downloader) {
		this.s3Client = s3Client;
		this.stsClient = stsClient;
		this.lambdaClient = lambdaClient;
		this.config = config;
		this.s3Config = s3Config;
		this.velocity = velocity;
		this.cloudFormationClientWrapper = cloudFormationClientWrapper;
		this.tagsProvider = tagsProvider;
		this.downloader = downloader;
	}

	@Override
	public void buildAllBuckets() {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		
		String accountId = stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account();
		
		List<String> virusScanEnabledBuckets = new ArrayList<>();
		List<String> virusScanDisabledBuckets = new ArrayList<>();
		
		// Configure all buckets first
		for (S3BucketDescriptor bucket : s3Config.getBuckets()) {
			
			bucket.setName(TemplateUtils.replaceStackVariable(bucket.getName(), stack));
			
			if (bucket.isDevOnly() && stack.equalsIgnoreCase(Constants.PROD_STACK_NAME)) {
				LOG.warn("The bucket {} is deployed only on non-prod stacks.", bucket.getName());
				continue;
			}
			
			createBucket(bucket.getName());
			configurePublicAccessBlock(bucket.getName());
			configureEncryption(bucket.getName());	
			configureInventory(stack, bucket.getName(), accountId, s3Config.getInventoryConfig(), bucket.isInventoryEnabled());
			configureBucketLifeCycle(bucket);
			configureIntelligentTieringArchive(bucket);
			configureBucketNotifications(bucket, stack);
			
			if (bucket.isVirusScanEnabled()) {
				virusScanEnabledBuckets.add(bucket.getName());
			} else {
				virusScanDisabledBuckets.add(bucket.getName());
			}
			
		}

		buildVirusScannerStack(stack, s3Config.getVirusScannerConfig(), virusScanEnabledBuckets).ifPresent( virusScannerStack -> {
			// Once the virus scanner stack is built we need to setup for each bucket a notification configuration to
			// send upload events to the topic the lambda is triggered by, this cannot be done in the cloud formation
			// template due to a known circular dependency (See https://github.com/aws-cloudformation/cloudformation-coverage-roadmap/issues/79).
			// Note that the proposed solution (e.g. read hack) by AWS (https://aws.amazon.com/premiumsupport/knowledge-center/cloudformation-s3-notification-lambda/)
			// involves using a custom resource setup by yet another lambda when the stack is created taking in input the bucket to setup the notification for, since we want to enable
			// this on multiple buckets using the API is a much simpler solution.
			String virusScannerTopicArn = getStackOutput(virusScannerStack, CF_OUTPUT_VIRUS_TRIGGER_TOPIC);
			
			virusScanEnabledBuckets.forEach( bucket -> {
				configureBucketNotification(bucket, VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME, virusScannerTopicArn, Collections.singleton(Event.S3_OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD.toString()));
			});

			// Makes sure to remove the existing bucket configurations
			virusScanDisabledBuckets.forEach( bucket -> {
				removeBucketNotification(bucket, VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME);
			});
			
			// We also need to trigger the lambda that updates the clamav definitions to setup them up so that the scanner can download them
			String virusScannerUpdatedLambda = getStackOutput(virusScannerStack, CF_OUTPUT_VIRUS_UPDATER_LAMBDA);

			lambdaClient.invoke(InvokeRequest.builder()
					.functionName(virusScannerUpdatedLambda)
					.invocationType(InvocationType.EVENT)
					.build()
			);
		});

		buildS3BucketPolicyStack(stack);
	}

	private Optional<Stack> buildS3BucketPolicyStack(String stack) {
		VelocityContext context = new VelocityContext();

		context.put(Constants.STACK, stack);
		context.put(CF_PROPERTY_BUCKETS, s3Config.getBuckets().stream()
			.filter(bucket -> !bucket.isDevOnly() || !stack.equalsIgnoreCase(Constants.PROD_STACK_NAME))
			.collect(Collectors.toList())
		);

		// Merge the context with the template
		Template template = velocity.getTemplate(Constants.TEMPLATE_S3_BUCKET_POLICY);

		StringWriter stringWriter = new StringWriter();

		template.merge(context, stringWriter);

		String resultJSON = stringWriter.toString();

		LOG.info(resultJSON);

		resultJSON = new JSONObject(resultJSON).toString(5);
		
		String stackName = TemplateUtils.replaceStackVariable(BUCKET_POLICY_STACK_NAME, stack);

		cloudFormationClientWrapper.createOrUpdateStack(new CreateOrUpdateStackRequest()
				.withStackName(stackName)
				.withTemplateBody(resultJSON)
				.withTags(tagsProvider.getStackTags(config)));

		try {
			cloudFormationClientWrapper.waitForStackToComplete(stackName);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return Optional.of(cloudFormationClientWrapper.describeStack(stackName).orElseThrow(()->new IllegalStateException("Stack does not exist: "+stackName)));
	}
	
	private Optional<Stack>buildVirusScannerStack(String stack, S3VirusScannerConfig config, List<String> buckets) {
		
		if (config == null) {
			return Optional.empty();
		}
		
		String lambdaSourceArtifactUrl = this.config.getProperty(PROPERTY_KEY_LAMBDA_VIRUS_SCANNER_ARTIFACT_URL);
		String lambdaArtifactBucket = TemplateUtils.replaceStackVariable(config.getLambdaArtifactBucket(), stack);
		String lambdaArtifactKey = String.format(VIRUS_SCANNER_KEY_TEMPLATE, FilenameUtils.getName(lambdaSourceArtifactUrl));
		
		byte[] content = downloader.downloadAsBytes(lambdaSourceArtifactUrl);

		s3Client.putObject(
				PutObjectRequest.builder()
						.bucket(lambdaArtifactBucket)
						.key(lambdaArtifactKey)
						.build(),
				RequestBody.fromBytes(content));

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
		
		String resultJSON = stringWriter.toString();
		
		LOG.info(resultJSON);
		
		resultJSON = new JSONObject(resultJSON).toString(5);
		
		String stackName = TemplateUtils.replaceStackVariable(VIRUS_SCANNER_STACK_NAME, stack);
		
		cloudFormationClientWrapper.createOrUpdateStack(new CreateOrUpdateStackRequest()
				.withStackName(stackName)
				.withTemplateBody(resultJSON)
				.withTags(tagsProvider.getStackTags(this.config))
				.withCapabilities(Capability.CAPABILITY_NAMED_IAM));
		
		try {
			cloudFormationClientWrapper.waitForStackToComplete(stackName);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		return Optional.of(cloudFormationClientWrapper.describeStack(stackName).orElseThrow(()->new IllegalStateException("Stack does not exist: "+stackName)));
	}
		
	private void createBucket(String bucketName) {
		LOG.info("Creating bucket: {}.", bucketName);
		
		// This is idempotent
		s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
	}
	
	private void configurePublicAccessBlock(String bucketName) {
		
		PublicAccessBlockConfiguration config = null;
		
		try {
			GetPublicAccessBlockResponse result = s3Client.getPublicAccessBlock(GetPublicAccessBlockRequest.builder()
				.bucket(bucketName)
				.build());
			
			if (result != null) {
				config = result.publicAccessBlockConfiguration();
			}
			
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				LOG.info("No public access block configuration found for bucket {}.", bucketName);
			} else {
				throw e;
			}
		}
		
		if (config != null) {
			LOG.info("Public access block configuration already exists for bucket {}, will not update.", bucketName);
			return;
		}
		
		config = PublicAccessBlockConfiguration.builder()
			.blockPublicAcls(true)
			.ignorePublicAcls(true)
			.blockPublicPolicy(true)
			.restrictPublicBuckets(true)
			.build();
		
		s3Client.putPublicAccessBlock(PutPublicAccessBlockRequest.builder()
			.bucket(bucketName)
			.publicAccessBlockConfiguration(config)
			.build());
		
		LOG.info("Public access block configured for bucket {}: {}", bucketName, config);
	}
	
	private void configureEncryption(String bucketName) {
		try {
			// If server side encryption is not currently set this call with throw a 404
			s3Client.getBucketEncryption(GetBucketEncryptionRequest.builder()
				.bucket(bucketName)
				.build());
		} catch (S3Exception e) {
			if(e.statusCode() == 404) {
				// The bucket is not currently encrypted so configure it for encryption.
				LOG.info("Setting server side encryption for bucket: {}.", bucketName);
				
				s3Client.putBucketEncryption(PutBucketEncryptionRequest.builder()
					.bucket(bucketName)
					.serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration.builder()
						.rules(ServerSideEncryptionRule.builder()
							.applyServerSideEncryptionByDefault(ServerSideEncryptionByDefault.builder()
								.sseAlgorithm(ServerSideEncryption.AES256)
								.build())
							.build())
						.build())
					.build());
			} else {
				throw e;
			}
		} 
	}
	
	private void configureInventory(String stack, String bucketName, String accountId, S3InventoryConfig inventoryConfig, boolean enabled) {
		if (inventoryConfig == null) {
			return;
		}
		
		boolean configurationExists = true;
		
		try {
			s3Client.getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest.builder()
				.bucket(bucketName)
				.id(INVENTORY_ID)
				.build());
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				configurationExists = false;
			} else {
				throw e;
			}
		}
		
		if (enabled) {
			LOG.info("Configuring inventory configuration for bucket {}.", bucketName);
			InventoryConfiguration config = InventoryConfiguration.builder()
					.id(INVENTORY_ID)
					.destination(InventoryDestination.builder()
						.s3BucketDestination(InventoryS3BucketDestination.builder()
							.bucket("arn:aws:s3:::" + TemplateUtils.replaceStackVariable(inventoryConfig.getBucket(), stack))
							.accountId(accountId)
							.prefix(inventoryConfig.getPrefix())
							.format(InventoryFormat.PARQUET)
							.build())
						.build())
					.optionalFields(INVENTORY_FIELDS)
					.schedule(InventorySchedule.builder()
						.frequency(InventoryFrequency.WEEKLY)
						.build())
					.isEnabled(true)
					.includedObjectVersions(InventoryIncludedObjectVersions.ALL)
					.build();
			
			s3Client.putBucketInventoryConfiguration(PutBucketInventoryConfigurationRequest.builder()
				.bucket(bucketName)
				.id(INVENTORY_ID)
				.inventoryConfiguration(config)
				.build());			
		} else if (configurationExists) {
			LOG.info("Removing inventory configuration for bucket {}.", bucketName);
			s3Client.deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest.builder()
				.bucket(bucketName)
				.id(INVENTORY_ID)
				.build());
		}
		
	}
	
	private void configureBucketLifeCycle(S3BucketDescriptor bucket) {
		
		boolean configurationExists = true;
		GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse = null;

		try {
			getBucketLifecycleConfigurationResponse = s3Client.getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder()
					.bucket(bucket.getName())
					.build());
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				configurationExists = false;
			} else {
				throw e;
			}
		}

		List<LifecycleRule> rules = !configurationExists ? new ArrayList<>() : new ArrayList<>(getBucketLifecycleConfigurationResponse.rules());
		
		boolean update = false;

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
			BucketLifecycleConfiguration lifecycleConfig = BucketLifecycleConfiguration.builder()
				.rules(rules)
				.build();
			
			LOG.info("Updating bucket {} lifecycle, rules: ", bucket.getName());
			
			for (LifecycleRule rule : rules) {
				LOG.info("	{}", rule.id());
			}
			
			s3Client.putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest.builder()
				.bucket(bucket.getName())
				.lifecycleConfiguration(lifecycleConfig)
				.build());
		}
		
	}
	
	void configureIntelligentTieringArchive(S3BucketDescriptor bucket) {
		
		if (bucket.getIntArchiveConfiguration() == null) {
			return;
		}
				
		IntelligentTieringConfiguration intConfig;
		
		try {
			intConfig = s3Client.getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationRequest.builder()
				.bucket(bucket.getName())
				.id(INT_ARCHIVE_ID)
				.build()).intelligentTieringConfiguration();
		} catch (S3Exception e) {
			if (404 == e.statusCode() && "NoSuchConfiguration".equals(e.awsErrorDetails().errorCode())) {
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
		
		s3Client.putBucketIntelligentTieringConfiguration(PutBucketIntelligentTieringConfigurationRequest.builder()
			.bucket(bucket.getName())
			.id(INT_ARCHIVE_ID)
			.intelligentTieringConfiguration(intConfig)
			.build());
		
	}
	
	private IntelligentTieringConfiguration createIntArchiveConfiguration(S3IntArchiveConfiguration config) {
		IntelligentTieringConfiguration.Builder intConfigBuilder = IntelligentTieringConfiguration.builder()
			.id(INT_ARCHIVE_ID)
			.status(IntelligentTieringStatus.ENABLED);

		List<Tiering> tiers = new ArrayList<>();
		
		if (config.getArchiveAccessDays() != null) {
			tiers.add(Tiering.builder()
				.accessTier(IntelligentTieringAccessTier.ARCHIVE_ACCESS)
				.days(config.getArchiveAccessDays())
				.build());
		}
		
		if (config.getDeepArchiveAccessDays() != null) {
			tiers.add(Tiering.builder()
				.accessTier(IntelligentTieringAccessTier.DEEP_ARCHIVE_ACCESS)
				.days(config.getDeepArchiveAccessDays())
				.build());
		}
		
		intConfigBuilder.tierings(tiers);
		
		IntelligentTieringFilter.Builder filterBuilder = IntelligentTieringFilter.builder();

		if (config.getTagFilter() != null) {
			filterBuilder.tag(Tag.builder()
				.key(config.getTagFilter().getName())
				.value(config.getTagFilter().getValue())
				.build());
		}
		
		intConfigBuilder.filter(filterBuilder.build());
		
		return intConfigBuilder.build();
	}
	
	private LifecycleRule createAbortMultipartRule(S3BucketDescriptor bucket) {
		return LifecycleRule.builder()
				.abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder()
					.daysAfterInitiation(ABORT_MULTIPART_UPLOAD_DAYS)
					.build())
				.filter(allBucketLifecycletFilter())
				.build();
	}
	
	private boolean updateAbortMultipartRule(LifecycleRule.Builder ruleBuilder, S3BucketDescriptor bucket) {
		LifecycleRule rule = ruleBuilder.build();
		if (rule.abortIncompleteMultipartUpload() == null || ABORT_MULTIPART_UPLOAD_DAYS != rule.abortIncompleteMultipartUpload().daysAfterInitiation() || rule.filter() == null) {
			ruleBuilder.abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder()
				.daysAfterInitiation(ABORT_MULTIPART_UPLOAD_DAYS)
				.build())
				.filter(allBucketLifecycletFilter());
			return true;
		} else {
			return false;
		}
	}
	
	private LifecycleRule createRetentionRule(S3BucketDescriptor bucket) {
		return LifecycleRule.builder()
				.expiration(LifecycleExpiration.builder()
					.days(bucket.getRetentionDays())
					.build())
				.filter(allBucketLifecycletFilter())
				.build();
	}
	
	private boolean updateRetentionRule(LifecycleRule.Builder ruleBuilder, S3BucketDescriptor bucket) {
		LifecycleRule rule = ruleBuilder.build();
		if (rule.expiration() == null || !bucket.getRetentionDays().equals(rule.expiration().days()) || rule.filter() == null) {
			ruleBuilder.expiration(LifecycleExpiration.builder()
				.days(bucket.getRetentionDays())
				.build())
				.filter(allBucketLifecycletFilter());
			return true;
		} else {
			return false;
		}
	}
	
	private LifecycleRule createClassTransitionRule(S3BucketClassTransition transition) {
		return LifecycleRule.builder()
			.transitions(Transition.builder()
				.storageClass(software.amazon.awssdk.services.s3.model.TransitionStorageClass.fromValue(transition.getStorageClass().toString()))
				.days(transition.getDays())
				.build())
			.filter(allBucketLifecycletFilter())
			.build();
	}
	
	private boolean updateClassTransitionRule(LifecycleRule.Builder ruleBuilder, S3BucketClassTransition transition) {
		LifecycleRule rule = ruleBuilder.build();
		Transition existingTransition = null;
		
		if (rule.transitions() != null && !rule.transitions().isEmpty()) {
			existingTransition = rule.transitions().get(0);
		} else {
			existingTransition = Transition.builder()
				.storageClass(software.amazon.awssdk.services.s3.model.TransitionStorageClass.fromValue(transition.getStorageClass().toString()))
				.days(transition.getDays())
				.build();
		}
		
		if (!transition.getStorageClass().toString().equals(existingTransition.storageClass().toString()) || !transition.getDays().equals(existingTransition.days()) || rule.filter() == null) {
			ruleBuilder.transitions(Transition.builder()
				.storageClass(software.amazon.awssdk.services.s3.model.TransitionStorageClass.fromValue(transition.getStorageClass().toString()))
				.days(transition.getDays())
				.build())
				.filter(allBucketLifecycletFilter());
			return true;
		} else {
			return false;
		}
	}
	
	private static LifecycleRuleFilter allBucketLifecycletFilter() {
		return LifecycleRuleFilter.builder().build();
	}
	
	private static <T> boolean addOrUpdateRule(List<LifecycleRule> rules, String bucket, String ruleName, T definition, Function<T, LifecycleRule> ruleCreator, BiFunction<LifecycleRule.Builder, T, Boolean> ruleUpdate) {
		Optional<LifecycleRule> rule = findRule(ruleName, rules);
		
		boolean updateLifecycle = false;
		
		if (rule.isPresent()) {
			LifecycleRule.Builder existingRuleBuilder = rule.get().toBuilder();
			
			updateLifecycle = ruleUpdate.apply(existingRuleBuilder, definition);
			
			if (updateLifecycle) {
				// Replace the existing rule with the updated one
				rules.remove(rule.get());
				rules.add(existingRuleBuilder.build());
			}
			
			LOG.info("The {} rule was found on bucket {} and was {}", ruleName, bucket, updateLifecycle ? "outdated, will update." : "up to date.");
		} else {
			LifecycleRule newRule = ruleCreator.apply(definition).toBuilder()
				.id(ruleName)
				.status(software.amazon.awssdk.services.s3.model.ExpirationStatus.ENABLED)
				.build();
			
			rules.add(newRule);
			
			LOG.info("The {} rule was not found on bucket {}, will be added.", ruleName, bucket);
			
			updateLifecycle = true;
		}
		
		return updateLifecycle;
		
	}
	
	private static Optional<LifecycleRule> findRule(String ruleName, List<LifecycleRule> rules) {
		return rules.stream().filter(rule -> rule.id().equals(ruleName)).findFirst();
	}
	
	private void configureBucketNotifications(S3BucketDescriptor bucket, String stack) {
		if (bucket.getNotificationsConfiguration() == null) {
			return;
		}
		
		S3NotificationsConfiguration config = bucket.getNotificationsConfiguration();
		
		String globalStackName = String.format(GLOBAL_RESOURCES_STACK_NAME_FORMAT, stack);
		
		String topicArn = cloudFormationClientWrapper.getOutput(globalStackName, config.getTopic());
		
		String configName = config.getTopic() + "Configuration";
		
		configureBucketNotification(bucket.getName(), configName, topicArn, config.getEvents());
		
	}
	
	private void configureBucketNotification(String bucketName, String configName, String topicArn, Set<String> events) {
		
		GetBucketNotificationConfigurationResponse response = s3Client.getBucketNotificationConfiguration(
			GetBucketNotificationConfigurationRequest.builder()
				.bucket(bucketName)
				.build());

		List<TopicConfiguration> topicConfigurations = response.topicConfigurations();

		boolean update = false;
		
		if (topicConfigurations == null || topicConfigurations.isEmpty()) {
			update = true;
		}
		
		// Find the config with matching name (Config = bucketConfig.getConfigurationByName() in v1)
		// BucketNotificationConfiguration used to be map<String, NotificationConfiguration>
		TopicConfiguration existingTopicConfig = topicConfigurations.stream()
			.filter(tc -> tc.id() != null && tc.id().equals(configName))
			.findFirst()
			.orElse(null);
		
		List<TopicConfiguration> topicConfigs = new ArrayList<>(topicConfigurations);

		if (existingTopicConfig == null) {  // No topic with configName
			TopicConfiguration newTopicConfig = TopicConfiguration.builder()
				.id(configName)
				.topicArn(topicArn)
				.events(events.stream().map(Event::fromValue).collect(Collectors.toSet()))
				.build();
			topicConfigs.add(newTopicConfig);
			update = true;
		} else {
			HashSet<Event> existingTopicEvents = new HashSet<>(existingTopicConfig.events());
			HashSet<Event> eventList = (HashSet<Event>) events.stream().map(Event::fromValue).collect(Collectors.toSet());
			
			if (!existingTopicConfig.topicArn().equals(topicArn) || !existingTopicEvents.equals(eventList)) {
				Iterator<TopicConfiguration> iterator = topicConfigs.iterator();
				while (iterator.hasNext()) {
					TopicConfiguration config = iterator.next();
					if (config.id().equals(existingTopicConfig.id())) {
						iterator.remove();
						topicConfigs.add(existingTopicConfig.toBuilder()
								.topicArn(topicArn)
								.events(eventList)
								.build());
						update = true;
						break;
					}
				}
			}
		}
		
		if (update) {
			LOG.info("Updating {} bucket notification configuration {} (Topic ARN: {}).", bucketName, configName, topicArn);
			s3Client.putBucketNotificationConfiguration(PutBucketNotificationConfigurationRequest.builder()
				.bucket(bucketName)
				.notificationConfiguration(NotificationConfiguration.builder()
					.topicConfigurations(topicConfigs)
					.build())
				.build());
		} else {
			LOG.info("The {} bucket notification configuration {} was up to date (Topic ARN: {}).", bucketName, configName, topicArn);
		}
	}

	private void removeBucketNotification(String bucketName, String configName) {
		GetBucketNotificationConfigurationResponse response = s3Client.getBucketNotificationConfiguration(
			GetBucketNotificationConfigurationRequest.builder()
				.bucket(bucketName)
				.build());
		
		if (response == null || response.topicConfigurations() == null || response.topicConfigurations().isEmpty()) {
			return;
		}
		
		TopicConfiguration existingTopicConfig = response.topicConfigurations().stream()
			.filter(tc -> tc.id() != null && tc.id().equals(configName))
			.findFirst()
			.orElse(null);
		
		if (existingTopicConfig == null) {
			return;
		}
		
		List<TopicConfiguration> topicConfigs = new ArrayList<>(response.topicConfigurations());
		topicConfigs.remove(existingTopicConfig);
		
		LOG.info("Removing {} bucket notification configuration {}.", bucketName, configName);
		
		s3Client.putBucketNotificationConfiguration(PutBucketNotificationConfigurationRequest.builder()
			.bucket(bucketName)
			.notificationConfiguration(NotificationConfiguration.builder()
				.topicConfigurations(topicConfigs)
				.build())
			.build());		
	}
}
