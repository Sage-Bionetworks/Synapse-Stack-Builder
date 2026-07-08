package org.sagebionetworks.template.markdownit;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Stack;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.template.Constants.*;
import static org.sagebionetworks.template.markdownit.MarkDownItLambdaBuilderImpl.markdownitArtifactKeyFromVersion;

@ExtendWith(MockitoExtension.class)
public class MarkDownItLambdaBuilderImplTest {

    @Mock
    RepoConfiguration mockConfig;
    @Mock
    ArtifactDownload mockDownloader;

    @Mock
    CloudFormationClientWrapper mockCloudFormationClientWrapper;

    @Mock
    StackTagsProvider mockTagsProvider;

    @Mock
    AmazonS3 mockS3Client;

    VelocityEngine velocityEngine;

    @Mock
    File mockFile;


    private String stack;

    @BeforeEach
    public void before() {
        stack = "dev";
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        when(mockConfig.getProperty(PROPERTY_KEY_LAMBDA_MARKDOWNIT_VERSION)).thenReturn("v0.0.1");
        when(mockConfig.getProperty(PROPERTY_KEY_LAMBDA_MARKDOWN_IT_SUBDOMAIN)).thenReturn("md2html");
        when(mockConfig.getProperty(PROPERTY_KEY_LAMBDA_MARKDOWNIT_CERTIFICATE_ARN)).thenReturn("arn:123456789012:cert");
    }

    @Test
    public void testBuildMarkDownItLambda() throws Exception {

        velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

        MarkDownItLambdaBuilder builder = new MarkDownItLambdaBuilderImpl(
                mockConfig,
                mockDownloader,
                mockCloudFormationClientWrapper,
                mockTagsProvider,
                mockS3Client,
                velocityEngine);


        when(mockDownloader.downloadFile(any())).thenReturn(mockFile);

        when(mockTagsProvider.getStackTags(mockConfig)).thenReturn(Collections.emptyList());

        Stack markdownItLambdaStack = Stack.builder().build();

        when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(markdownItLambdaStack));

        String expectedBucket = "dev.artifacts.sagebase.org";
        String expectedKey = "markdown-it-v0.0.1.zip";

        // call under test
        builder.buildMarkDownItLambda();

        verify(mockDownloader).downloadFile("https://github.com/Sage-Bionetworks/synapse-markdown-it-lambda/releases/download/v0.0.1/markdown-it-v0.0.1.zip");
        verify(mockS3Client).putObject(expectedBucket, expectedKey, mockFile);

        verify(mockFile).delete();

        ArgumentCaptor<CreateOrUpdateStackRequest> argCaptorCreateOrUpdateStack = ArgumentCaptor.forClass(CreateOrUpdateStackRequest.class);
        ArgumentCaptor<String> argCaptorWaitForStack = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> argCaptorDescribeStack = ArgumentCaptor.forClass(String.class);

        verify(mockCloudFormationClientWrapper, times(1)).createOrUpdateStack(argCaptorCreateOrUpdateStack.capture());
        verify(mockCloudFormationClientWrapper, times(1)).waitForStackToComplete(argCaptorWaitForStack.capture());
        verify(mockCloudFormationClientWrapper, times(1)).describeStack(argCaptorDescribeStack.capture());

        CreateOrUpdateStackRequest request = argCaptorCreateOrUpdateStack.getValue();
        assertEquals("dev-markdown-it-function", request.getStackName());
        assertTrue(request.getTags().isEmpty());
        assertEquals(1, request.getCapabilities().length);
        assertEquals(Capability.CAPABILITY_NAMED_IAM, request.getCapabilities()[0]);
        assertNotNull(request.getTemplateBody());

        JSONObject templateJson = new JSONObject(request.getTemplateBody());
        System.out.println(request.getTemplateBody());
        JSONObject resources = templateJson.getJSONObject("Resources");
        assertTrue(resources.has("mdlambdaServiceRole"));
        assertTrue(resources.has("mdlambda"));
        assertTrue(resources.has("MarkdownItApi"));
        assertTrue(resources.has("MarkdownItApiPostMethod"));
        assertTrue(resources.has("MarkdownItApiPermission"));
        assertTrue(resources.has("MarkdownItApiDeployment"));
        assertTrue(resources.has("MarkdownItApiStage"));
        assertTrue(resources.has("CustomDomain"));
        assertTrue(resources.has("BasePathMapping"));

        assertEquals("dev-markdown-it-function", argCaptorWaitForStack.getValue());
        assertEquals("dev-markdown-it-function", argCaptorDescribeStack.getValue());

    }

}
