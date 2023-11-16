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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.io.FilenameUtils;
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
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortIncompleteMultipartUpload;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Transition;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.Grant;
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
		return stack.getOutputs().stream()
		.filter( output -> output.getOutputKey().equals(key))
		.findFirst()
		.orElseThrow(() -> new IllegalStateException("Could not find " + key + " output from stack " + stack.getStackName()))
		.getOutputValue();
	}
	
	private AmazonS3 s3Client;
	private AWSSecurityTokenService stsClient;
	private AWSLambda lambdaClient;
	private RepoConfiguration config;
	private S3Config s3Config;
	private VelocityEngine velocity;
	private CloudFormationClient cloudFormationClient;
	private StackTagsProvider tagsProvider;
	private ArtifactDownload downloader;
	
	@Inject
	public S3BucketBuilderImpl(AmazonS3 s3Client, AWSSecurityTokenService stsClient, AWSLambda lambdaClient, RepoConfiguration config, S3Config s3Config, VelocityEngine velocity, CloudFormationClient cloudFormationClient, StackTagsProvider tagsProvider, ArtifactDownload downloader) {
		this.s3Client = s3Client;
		this.stsClient = stsClient;
		this.lambdaClient = lambdaClient;
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
			configureEncryption(bucket.getName());	
			configureInventory(stack, bucket.getName(), accountId, s3Config.getInventoryConfig(), bucket.isInventoryEnabled());
			configureBucketLifeCycle(bucket);
			configureIntelligentTieringArchive(bucket);
			configureBucketNotifications(bucket, stack);
			configureBucketAcl(bucket.getName(), bucket.getAdditionalAclGrants());
			
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
			
			lambdaClient.invoke(new InvokeRequest()
				.withFunctionName(virusScannerUpdatedLambda)
				.withInvocationType(InvocationType.Event)
			);
		});

		buildS3BucketPolicyStack(stack);
	}

	private Optional<Stack> buildS3BucketPolicyStack(String stack) {
		VelocityContext context = new VelocityContext();

		context.put(Constants.STACK, stack);

		// Merge the context with the template
		Template template = velocity.getTemplate(Constants.TEMPLATE_S3_BUCKET_POLICY);

		StringWriter stringWriter = new StringWriter();

		template.merge(context, stringWriter);

		String resultJSON = stringWriter.toString();

		LOG.info(resultJSON);

		resultJSON = new JSONObject(resultJSON).toString(5);

		String stackName = TemplateUtils.replaceStackVariable(BUCKET_POLICY_STACK_NAME, stack);

		cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest()
				.withStackName(stackName)
				.withTemplateBody(resultJSON)
				.withTags(tagsProvider.getStackTags()));

		try {
			cloudFormationClient.waitForStackToComplete(stackName);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return Optional.of(cloudFormationClient.describeStack(stackName).orElseThrow(()->new IllegalStateException("Stack does not exist: "+stackName)));
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
		
		String resultJSON = stringWriter.toString();
		
		LOG.info(resultJSON);
		
		resultJSON = new JSONObject(resultJSON).toString(5);
		
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
		
		return Optional.of(cloudFormationClient.describeStack(stackName).orElseThrow(()->new IllegalStateException("Stack does not exist: "+stackName)));
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
	
	private void configureBucketAcl(String bucketName, List<S3BucketAclGrant> additionalGrants) {
		if (additionalGrants == null || additionalGrants.isEmpty()) {
			return;
		}
		
		AccessControlList bucketAcl = s3Client.getBucketAcl(bucketName);
		
		List<Grant> toAdd = new ArrayList<>();
		
		additionalGrants.forEach( additionalGrant -> {
			// We need to adapt our custom grant to the one used by the s3 model
			Grant s3GrantAdapter = new Grant(new CanonicalGrantee(additionalGrant.getCanonicalGrantee()), additionalGrant.getPermission());
			
			if (!bucketAcl.getGrantsAsList().contains(s3GrantAdapter)) {
				toAdd.add(s3GrantAdapter);
			}
		});
		
		if (!toAdd.isEmpty()) {
			LOG.info("Updating {} bucket ACL with the following additional grants:", bucketName);
			toAdd.forEach( grant -> {
				LOG.info(" -> " + grant.getGrantee().getIdentifier() + ": " + grant.getPermission());
			});
			bucketAcl.getGrantsAsList().addAll(toAdd);
			s3Client.setBucketAcl(bucketName, bucketAcl);
		} else {
			LOG.info("The {} bucket ACL is already up to date:", bucketName);
			bucketAcl.getGrantsAsList().forEach( grant -> {
				LOG.info(" -> " + grant.getGrantee().getIdentifier() + ": " + grant.getPermission());
			});
		}
	}
}
