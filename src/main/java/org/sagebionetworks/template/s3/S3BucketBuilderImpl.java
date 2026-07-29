package org.sagebionetworks.template.s3;

import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_STACK_NAME_FORMAT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_VIRUS_SCANNER_ARTIFACT_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.NotificationConfiguration;
import com.amazonaws.services.s3.model.S3Event;
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
import com.google.inject.Inject;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortIncompleteMultipartUpload;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3.model.LifecycleExpiration;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.PutBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionByDefault;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionConfiguration;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionRule;
import software.amazon.awssdk.services.s3.model.Transition;
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
	static final String INVENTORY_FORMAT = "Parquet";
	static final List<String> INVENTORY_FIELDS = Arrays.asList(
			"Size", "LastModifiedDate", "ETag", "IsMultipartUploaded", "StorageClass", "IntelligentTieringAccessTier", "EncryptionStatus", "ObjectOwner"
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
	
	private AmazonS3 s3Client;
	private S3Client s3ClientV2;
	private StsClient stsClient;
	private LambdaClient lambdaClient;
	private RepoConfiguration config;
	private S3Config s3Config;
	private VelocityEngine velocity;
	private CloudFormationClientWrapper cloudFormationClientWrapper;
	private StackTagsProvider tagsProvider;
	private ArtifactDownload downloader;

	@Inject
	public S3BucketBuilderImpl(AmazonS3 s3Client, S3Client s3ClientV2, StsClient stsClient, LambdaClient lambdaClient, RepoConfiguration config, S3Config s3Config, VelocityEngine velocity, CloudFormationClientWrapper cloudFormationClientWrapper, StackTagsProvider tagsProvider, ArtifactDownload downloader) {
		this.s3Client = s3Client;
		this.s3ClientV2 = s3ClientV2;
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
				configureBucketNotification(bucket, VIRUS_SCANNER_NOTIFICATION_CONFIG_NAME, virusScannerTopicArn, Collections.singleton(S3Event.ObjectCreatedByCompleteMultipartUpload.toString()));
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
		
		File artifact = downloader.downloadFile(lambdaSourceArtifactUrl);
		
		try {
			s3ClientV2.putObject(PutObjectRequest.builder().bucket(lambdaArtifactBucket).key(lambdaArtifactKey).build(), RequestBody.fromFile(artifact));
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

		try {
			s3ClientV2.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
		} catch (BucketAlreadyOwnedByYouException e) {
			// The bucket already exists and is owned by us, nothing to do.
		}
	}

	private void configurePublicAccessBlock(String bucketName) {

		PublicAccessBlockConfiguration config = null;

		try {
			GetPublicAccessBlockResponse result = s3ClientV2.getPublicAccessBlock(GetPublicAccessBlockRequest.builder().bucket(bucketName).build());

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

		s3ClientV2.putPublicAccessBlock(PutPublicAccessBlockRequest.builder()
			.bucket(bucketName)
			.publicAccessBlockConfiguration(config)
			.build()
		);

		LOG.info("Public access block configured for bucket {}: {}", bucketName, config);
	}

	private void configureEncryption(String bucketName) {
		try {
			// If server side encryption is not currently set this call with throw a 404
			s3ClientV2.getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(bucketName).build());
		} catch (S3Exception e) {
			if(e.statusCode() == 404) {
				// The bucket is not currently encrypted so configure it for encryption.
				LOG.info("Setting server side encryption for bucket: {}.", bucketName);

				s3ClientV2.putBucketEncryption(PutBucketEncryptionRequest.builder().bucket(bucketName)
						.serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration.builder()
								.rules(ServerSideEncryptionRule.builder().applyServerSideEncryptionByDefault(
										ServerSideEncryptionByDefault.builder().sseAlgorithm(ServerSideEncryption.AES256).build()).build())
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
			s3Client.getBucketInventoryConfiguration(bucketName, INVENTORY_ID);
		} catch (AmazonServiceException e) {
			if (e.getStatusCode() == 404) {
				configurationExists = false;
			} else {
				throw e;
			}
		}
		
		if (enabled) {
			LOG.info("Configuring inventory configuration for bucket {}.", bucketName);
			InventoryConfiguration config = new InventoryConfiguration()
					.withId(INVENTORY_ID)
					.withDestination(
							new InventoryDestination()
								.withS3BucketDestination(
										new InventoryS3BucketDestination()
											.withBucketArn("arn:aws:s3:::" + TemplateUtils.replaceStackVariable(inventoryConfig.getBucket(), stack))
											.withAccountId(accountId)
											.withPrefix(inventoryConfig.getPrefix())
											.withFormat(INVENTORY_FORMAT)
								)
					)
					.withOptionalFields(INVENTORY_FIELDS)
					.withSchedule(new InventorySchedule().withFrequency(InventoryFrequency.Weekly))
					.withEnabled(true)
					.withIncludedObjectVersions(InventoryIncludedObjectVersions.All);
			
			s3Client.setBucketInventoryConfiguration(bucketName, config);			
		} else if (configurationExists) {
			LOG.info("Removing inventory configuration for bucket {}.", bucketName);
			s3Client.deleteBucketInventoryConfiguration(bucketName, INVENTORY_ID);
		}
		
	}
	
	private void configureBucketLifeCycle(S3BucketDescriptor bucket) {

		// v2 throws when no life cycle configuration exists, treat as an empty set of rules
		List<LifecycleRule> existingRules = getBucketLifecycleRules(bucket.getName());

		// Keyed by rule id, preserving the existing order and allowing lookup/replacement
		LinkedHashMap<String, LifecycleRule> rules = new LinkedHashMap<>();

		for (LifecycleRule rule : existingRules) {
			rules.put(rule.id(), rule);
		}

		boolean update = false;

		if (bucket.getRetentionDays() != null) {
			if (addOrUpdateRule(rules, bucket.getName(), RULE_ID_RETENTION, bucket, this::createRetentionRule, this::isRetentionRuleUpToDate)) {
				update = true;
			}
		} else {
			if (rules.remove(RULE_ID_RETENTION) != null) {
				LOG.info("The {} rule was found on bucket {}, removing.", RULE_ID_RETENTION, bucket.getName());
				update = true;
			}
		}

		if (bucket.getStorageClassTransitions() != null) {
			for (S3BucketClassTransition transition : bucket.getStorageClassTransitions()) {
				String transitionRuleName = transition.getStorageClass().name() + RULE_ID_CLASS_TRANSITION;

				if (addOrUpdateRule(rules, bucket.getName(), transitionRuleName, transition, this::createClassTransitionRule, this::isClassTransitionRuleUpToDate)) {
					update = true;
				}
			}
		}

		// Always checks for a default multipart upload cleanup rule
		if (addOrUpdateRule(rules, bucket.getName(), RULE_ID_ABORT_MULTIPART_UPLOADS, bucket, this::createAbortMultipartRule, this::isAbortMultipartRuleUpToDate)) {
			update = true;
		}

		if (!rules.isEmpty() && update) {

			LOG.info("Updating bucket {} lifecycle, rules: ", bucket.getName());

			for (String ruleId : rules.keySet()) {
				LOG.info("	{}", ruleId);
			}

			s3ClientV2.putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest.builder()
					.bucket(bucket.getName())
					.lifecycleConfiguration(BucketLifecycleConfiguration.builder().rules(new ArrayList<>(rules.values())).build())
					.build());
		}

	}

	private List<LifecycleRule> getBucketLifecycleRules(String bucketName) {
		try {
			GetBucketLifecycleConfigurationResponse response = s3ClientV2.getBucketLifecycleConfiguration(
					GetBucketLifecycleConfigurationRequest.builder().bucket(bucketName).build());

			if (response == null || response.rules() == null) {
				return new ArrayList<>();
			}

			return new ArrayList<>(response.rules());
		} catch (S3Exception e) {
			// v2 throws a 404 (NoSuchLifecycleConfiguration) when no configuration exists
			if (e.statusCode() == 404) {
				return new ArrayList<>();
			}
			throw e;
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
	
	private LifecycleRule createAbortMultipartRule(String ruleName, S3BucketDescriptor bucket) {
		return baseRule(ruleName)
				.abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().daysAfterInitiation(ABORT_MULTIPART_UPLOAD_DAYS).build())
				.build();
	}

	private boolean isAbortMultipartRuleUpToDate(LifecycleRule rule, S3BucketDescriptor bucket) {
		return rule.filter() != null
				&& rule.abortIncompleteMultipartUpload() != null
				&& Integer.valueOf(ABORT_MULTIPART_UPLOAD_DAYS).equals(rule.abortIncompleteMultipartUpload().daysAfterInitiation());
	}

	private LifecycleRule createRetentionRule(String ruleName, S3BucketDescriptor bucket) {
		return baseRule(ruleName)
				.expiration(LifecycleExpiration.builder().days(bucket.getRetentionDays()).build())
				.build();
	}

	private boolean isRetentionRuleUpToDate(LifecycleRule rule, S3BucketDescriptor bucket) {
		return rule.filter() != null
				&& rule.expiration() != null
				&& bucket.getRetentionDays().equals(rule.expiration().days());
	}

	private LifecycleRule createClassTransitionRule(String ruleName, S3BucketClassTransition transition) {
		return baseRule(ruleName)
				.transitions(Transition.builder()
						.storageClass(transition.getStorageClass().toString())
						.days(transition.getDays())
						.build())
				.build();
	}

	private boolean isClassTransitionRuleUpToDate(LifecycleRule rule, S3BucketClassTransition transition) {
		if (rule.filter() == null || rule.transitions() == null || rule.transitions().isEmpty()) {
			return false;
		}

		Transition existingTransition = rule.transitions().get(0);

		return transition.getStorageClass().toString().equals(existingTransition.storageClassAsString())
				&& transition.getDays().equals(existingTransition.days());
	}

	private static LifecycleRule.Builder baseRule(String ruleName) {
		return LifecycleRule.builder()
				.id(ruleName)
				.status(ExpirationStatus.ENABLED)
				.filter(allBucketLifecycleFilter());
	}

	private static LifecycleRuleFilter allBucketLifecycleFilter() {
		// An empty filter applies the rule to all the objects in the bucket
		return LifecycleRuleFilter.builder().build();
	}

	private static <T> boolean addOrUpdateRule(LinkedHashMap<String, LifecycleRule> rules, String bucket, String ruleName, T definition, BiFunction<String, T, LifecycleRule> ruleCreator, BiPredicate<LifecycleRule, T> ruleUpToDate) {
		LifecycleRule existingRule = rules.get(ruleName);

		if (existingRule != null) {
			boolean upToDate = ruleUpToDate.test(existingRule, definition);

			LOG.info("The {} rule was found on bucket {} and was {}", ruleName, bucket, upToDate ? "up to date." : "outdated, will update.");

			if (upToDate) {
				return false;
			}

			// Re-putting an existing key preserves the rule position in the insertion order
			rules.put(ruleName, ruleCreator.apply(ruleName, definition));

			return true;
		} else {
			rules.put(ruleName, ruleCreator.apply(ruleName, definition));

			LOG.info("The {} rule was not found on bucket {}, will be added.", ruleName, bucket);

			return true;
		}
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
			LOG.info("Updating {} bucket notification configuration {} (Topic ARN: {}).", bucketName, configName, topicArn);
			s3Client.setBucketNotificationConfiguration(bucketName, bucketConfig);
		} else {
			LOG.info("The {} bucket notification configuration {} was up to date (Topic ARN: {}).", bucketName, configName, topicArn);
		}
	}

	private void removeBucketNotification(String bucketName, String configName) {
		BucketNotificationConfiguration bucketConfig = s3Client.getBucketNotificationConfiguration(bucketName);
		
		if (bucketConfig == null || bucketConfig.getConfigurations() == null || bucketConfig.getConfigurations().isEmpty()) {
			return;
		}
		
		NotificationConfiguration notificationConfig = bucketConfig.getConfigurationByName(configName);
		
		if (notificationConfig == null) {
			return;
		}
		
		bucketConfig.removeConfiguration(configName);
		
		LOG.info("Removing {} bucket notification configuration {}.", bucketName, configName);
		
		s3Client.setBucketNotificationConfiguration(bucketName, bucketConfig);		
	}
}
