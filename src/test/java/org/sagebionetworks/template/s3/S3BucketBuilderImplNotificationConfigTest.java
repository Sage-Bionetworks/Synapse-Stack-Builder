package org.sagebionetworks.template.s3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.io.File;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
public class S3BucketBuilderImplNotificationConfigTest {

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

        when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(
             GetBucketNotificationConfigurationResponse.builder().build()
        );
        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        // Call under test
        builder.buildAllBuckets();

        verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
        verify(mockS3Client).getBucketNotificationConfiguration(argThat((GetBucketNotificationConfigurationRequest request) ->
                expectedBucketName.equals(request.bucket())
        ));

        ArgumentCaptor<PutBucketNotificationConfigurationRequest> putBucketNotificationConfigurationRequestArgumentCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
        verify(mockS3Client).putBucketNotificationConfiguration(putBucketNotificationConfigurationRequestArgumentCaptor.capture());
        PutBucketNotificationConfigurationRequest actualPutBucketNotificationConfigurationRequest = putBucketNotificationConfigurationRequestArgumentCaptor.getValue();
        assertNotNull(actualPutBucketNotificationConfigurationRequest);
        NotificationConfiguration bucketConfig = actualPutBucketNotificationConfigurationRequest.notificationConfiguration();
        assertEquals(1, bucketConfig.topicConfigurations().size());
        TopicConfiguration snsConfig = bucketConfig.topicConfigurations().get(0);
        assertEquals(expectedTopicArn, snsConfig.topicArn());
        assertEquals(events.stream().map(Event::fromValue).collect(Collectors.toSet()), new HashSet<>(snsConfig.events()));

        verify(mockTemplate).merge(velocityContextCaptor.capture(), any());
        VelocityContext context = velocityContextCaptor.getValue();
        assertEquals(context.get(Constants.STACK), stack);

        String expectedStackName = stack + "-synapse-bucket-policies";

        CreateOrUpdateStackRequest createOrUpdateStackRequest = new CreateOrUpdateStackRequest()
                .withStackName(expectedStackName)
                .withTemplateBody("{}")
                        .withTags(Collections.emptyList());
        verify(mockCloudFormationClientWrapper).createOrUpdateStack(createOrUpdateStackRequest);

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

        TopicConfiguration existingConfig = TopicConfiguration.builder().build();
        GetBucketNotificationConfigurationResponse expectedGetBucketNotificationConfigurationResponse = GetBucketNotificationConfigurationResponse.builder().build();
        when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class)))
                .thenReturn(expectedGetBucketNotificationConfigurationResponse);

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);

        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);
        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(
                GetBucketLifecycleConfigurationResponse.builder().build()
        );

        // Call under test
        builder.buildAllBuckets();

        verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
        verify(mockS3Client).getBucketNotificationConfiguration(argThat((GetBucketNotificationConfigurationRequest request) ->
                expectedBucketName.equals(request.bucket())
        ));

        ArgumentCaptor<PutBucketNotificationConfigurationRequest> putBucketNotificationConfigurationRequestArgumentCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
        verify(mockS3Client).putBucketNotificationConfiguration(putBucketNotificationConfigurationRequestArgumentCaptor.capture());
        PutBucketNotificationConfigurationRequest actualPutBucketNotificationConfigurationRequest = putBucketNotificationConfigurationRequestArgumentCaptor.getValue();
        assertNotNull(actualPutBucketNotificationConfigurationRequest);
        NotificationConfiguration bucketConfig = actualPutBucketNotificationConfigurationRequest.notificationConfiguration();
        assertEquals(1, bucketConfig.topicConfigurations().size());
        TopicConfiguration snsConfig = bucketConfig.topicConfigurations().get(0);
        assertEquals(expectedTopicArn, snsConfig.topicArn());
        assertEquals(events.stream().map(Event::fromValue).collect(Collectors.toSet()), new HashSet<>(snsConfig.events()));

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

        NotificationConfiguration existingConfig = NotificationConfiguration.builder()
                .topicConfigurations(List.of(
                        TopicConfiguration.builder()
                                .topicArn("otherArn")
                                .events(EnumSet.of(Event.S3_OBJECT_CREATED))
                                .build()
                        ))
                .build();

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);

        GetBucketNotificationConfigurationResponse expectedGetBucketNotificationConfigurationResponse = GetBucketNotificationConfigurationResponse.builder().topicConfigurations(existingConfig.topicConfigurations()).build();
        when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(expectedGetBucketNotificationConfigurationResponse);

        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        Stack bucketPolicyStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(bucketPolicyStack));
        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(
                GetBucketLifecycleConfigurationResponse.builder().build()
        );

        // Call under test
        builder.buildAllBuckets();

        verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
        verify(mockS3Client).getBucketNotificationConfiguration(argThat((GetBucketNotificationConfigurationRequest request) ->
                expectedBucketName.equals(request.bucket()))
        );

        ArgumentCaptor<PutBucketNotificationConfigurationRequest> putBucketNotificationConfigurationRequestArgumentCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
        verify(mockS3Client).putBucketNotificationConfiguration(putBucketNotificationConfigurationRequestArgumentCaptor.capture());
        PutBucketNotificationConfigurationRequest actualPutBucketNotificationConfigurationRequest = putBucketNotificationConfigurationRequestArgumentCaptor.getValue();
        assertNotNull(actualPutBucketNotificationConfigurationRequest);
        NotificationConfiguration bucketConfig = actualPutBucketNotificationConfigurationRequest.notificationConfiguration();
        assertEquals(2, bucketConfig.topicConfigurations().size());
        TopicConfiguration snsConfig = bucketConfig.topicConfigurations().get(1); // TODO: OK?
        assertEquals(expectedTopicArn, snsConfig.topicArn());
        assertEquals(events.stream().map(Event::fromValue).collect(Collectors.toSet()), new HashSet<>(snsConfig.events()));

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

        NotificationConfiguration existingConfig = NotificationConfiguration.builder()
                .topicConfigurations(List.of(TopicConfiguration.builder()
                        .id(expectedConfigName) // OK?
                        .topicArn("otherArn")
                        .events(events.stream().map(Event::fromValue).collect(Collectors.toSet()))
                        .build()))
                .build();

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
        GetBucketNotificationConfigurationResponse expectedGetBucketNotificationConfigurationResponse = GetBucketNotificationConfigurationResponse.builder().topicConfigurations(existingConfig.topicConfigurations()).build();
        when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(expectedGetBucketNotificationConfigurationResponse);

        when(mockVelocity.getTemplate(any(String.class))).thenReturn(mockTemplate);
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

        verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
        verify(mockS3Client).getBucketNotificationConfiguration(argThat((GetBucketNotificationConfigurationRequest request) ->
                expectedBucketName.equals(request.bucket()))
        );

        ArgumentCaptor<PutBucketNotificationConfigurationRequest> putBucketNotificationConfigurationRequestArgumentCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
        verify(mockS3Client).putBucketNotificationConfiguration(putBucketNotificationConfigurationRequestArgumentCaptor.capture());
        PutBucketNotificationConfigurationRequest actualPutBucketNotificationConfigurationRequest = putBucketNotificationConfigurationRequestArgumentCaptor.getValue();
        assertNotNull(actualPutBucketNotificationConfigurationRequest);
        NotificationConfiguration bucketConfig = actualPutBucketNotificationConfigurationRequest.notificationConfiguration();
        assertEquals(1, bucketConfig.topicConfigurations().size());
        TopicConfiguration snsConfig = bucketConfig.topicConfigurations().get(0);
        assertEquals(expectedTopicArn, snsConfig.topicArn());
        assertEquals(events.stream().map(Event::fromValue).collect(Collectors.toSet()), new HashSet<>(snsConfig.events()));

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

        NotificationConfiguration existingConfig = NotificationConfiguration.builder()
                .topicConfigurations(List.of(TopicConfiguration.builder()
                        .id(expectedConfigName)
                        .topicArn(expectedTopicArn)
                        .events(EnumSet.of(Event.S3_OBJECT_RESTORE_POST))
                        .build()))
                .build();

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
        GetBucketNotificationConfigurationResponse expectedGetBucketNotificationConfigurationResponse = GetBucketNotificationConfigurationResponse.builder().topicConfigurations(existingConfig.topicConfigurations()).build();
        when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(expectedGetBucketNotificationConfigurationResponse);
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

        verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
        verify(mockS3Client).getBucketNotificationConfiguration(argThat((GetBucketNotificationConfigurationRequest request) ->
                expectedBucketName.equals(request.bucket()))
        );

        ArgumentCaptor<PutBucketNotificationConfigurationRequest> putBucketNotificationConfigurationRequestArgumentCaptor = ArgumentCaptor.forClass(PutBucketNotificationConfigurationRequest.class);
        verify(mockS3Client).putBucketNotificationConfiguration(putBucketNotificationConfigurationRequestArgumentCaptor.capture());
        PutBucketNotificationConfigurationRequest actualPutBucketNotificationConfigurationRequest = putBucketNotificationConfigurationRequestArgumentCaptor.getValue();
        assertNotNull(actualPutBucketNotificationConfigurationRequest);
        NotificationConfiguration bucketConfig = actualPutBucketNotificationConfigurationRequest.notificationConfiguration();
        assertEquals(1, bucketConfig.topicConfigurations().size());
        TopicConfiguration snsConfig = bucketConfig.topicConfigurations().get(0);
        assertEquals(expectedTopicArn, snsConfig.topicArn());
        assertEquals(events.stream().map(Event::fromValue).collect(Collectors.toSet()), new HashSet<>(snsConfig.events()));

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

        NotificationConfiguration existingConfig = NotificationConfiguration.builder()
                .topicConfigurations(List.of(TopicConfiguration.builder()
                        .id(expectedConfigName)
                        .topicArn("topicArn")
                        .events(events.stream().map(Event::fromValue).collect(Collectors.toSet()))
                        .build()))
                .build();

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
        GetBucketNotificationConfigurationResponse expectedGetBucketNotificationConfigurationResponse = GetBucketNotificationConfigurationResponse.builder().topicConfigurations(existingConfig.topicConfigurations()).build();
        when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(expectedGetBucketNotificationConfigurationResponse);

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

        verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
        verify(mockS3Client).getBucketNotificationConfiguration(argThat((GetBucketNotificationConfigurationRequest request) ->
                expectedBucketName.equals(request.bucket()))
        );
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

    @Disabled("This case should not happen anymore since there is one collection per notification type")
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

        NotificationConfiguration existingConfig = NotificationConfiguration.builder()
                .queueConfigurations(List.of(QueueConfiguration.builder()
                        .queueArn("queueArn")
                        .events(events.stream().map(Event::fromValue).collect(Collectors.toSet()))
                        .build()))
                .build();

//        when(mockVelocity.getTemplate(any())).thenReturn(mockTemplate);
//        doAnswer(invocation -> {
//            ((StringWriter) invocation.getArgument(1)).append("{}");
//            return null;
//        }).when(mockTemplate).merge(any(), any());

        when(mockS3Config.getBuckets()).thenReturn(List.of(bucket));
        when(mockCloudFormationClientWrapper.getOutput(any(), any())).thenReturn(expectedTopicArn);
        GetBucketNotificationConfigurationResponse expectedGetBucketNotificationConfigurationResponse = GetBucketNotificationConfigurationResponse.builder().topicConfigurations(existingConfig.topicConfigurations()).build();
        when(mockS3Client.getBucketNotificationConfiguration(any(GetBucketNotificationConfigurationRequest.class))).thenReturn(expectedGetBucketNotificationConfigurationResponse);

        GetBucketLifecycleConfigurationResponse expectedGetBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().build();
        when(mockS3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(expectedGetBucketLifecycleConfigurationResponse);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            // Call under test
            builder.buildAllBuckets();
        });

        assertEquals("The notification configuration " + expectedConfigName + " was found but was not a TopicConfiguration", ex.getMessage());

        verify(mockCloudFormationClientWrapper).getOutput(expectedGlobalStackName, topic);
        verify(mockS3Client).getBucketNotificationConfiguration(argThat((GetBucketNotificationConfigurationRequest request) ->
                expectedBucketName.equals(request.bucket()))
        );
        verify(mockS3Client, never()).putBucketNotificationConfiguration(any(PutBucketNotificationConfigurationRequest.class));
    }

}
