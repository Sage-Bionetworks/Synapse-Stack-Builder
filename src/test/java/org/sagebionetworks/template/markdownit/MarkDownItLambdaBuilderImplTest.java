package org.sagebionetworks.template.markdownit;

import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import java.io.File;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_ARTIFACT_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_LAMBDA_MARKDOWNIT_ARTIFACT_URL;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

@ExtendWith(MockitoExtension.class)
public class MarkDownItLambdaBuilderImplTest {

    @Mock
    RepoConfiguration mockConfig;
    @Mock
    ArtifactDownload mockDownloader;

    @Mock
    CloudFormationClient mockCloudFormationClient;

    @Mock
    StackTagsProvider mockTagsProvider;

    @Mock
    AmazonS3 mockS3Client;

    @Mock
    VelocityEngine mockVelocityEngine;

    @Mock
    File mockFile;

    @Mock
    Template mockTemplate;

    @Captor
    private ArgumentCaptor<VelocityContext> velocityContextCaptor;

    private String stack;

    @BeforeEach
    public void before() {
        stack = "dev";
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
        when(mockConfig.getProperty(PROPERTY_KEY_LAMBDA_ARTIFACT_BUCKET)).thenReturn("lambda.sagebase.org");
        when(mockConfig.getProperty(PROPERTY_KEY_LAMBDA_MARKDOWNIT_ARTIFACT_URL)).thenReturn("https://sagebionetworks.jfrog.io/lambda/org/sagebase/markdownit/markdownit.zip");
    }

    @Test
    public void testBuildMarkDownItLambda() throws Exception {

        MarkDownItLambdaBuilder builder = new MarkDownItLambdaBuilderImpl(
                mockConfig,
                mockDownloader,
                mockCloudFormationClient,
                mockTagsProvider,
                mockS3Client,
                mockVelocityEngine);


        when(mockDownloader.downloadFile(any())).thenReturn(mockFile);
        when(mockVelocityEngine.getTemplate(any())).thenReturn(mockTemplate);

        doAnswer(invocation -> {
            ((StringWriter) invocation.getArgument(1)).append("{}");
            return null;
        }).when(mockTemplate).merge(any(), any());

        when(mockTagsProvider.getStackTags()).thenReturn(Collections.emptyList());

        Stack markdownItLambdaStack = new Stack();

        when(mockCloudFormationClient.describeStack(any())).thenReturn(Optional.of(markdownItLambdaStack));

        String expectedBucket = "lambda.sagebase.org";
        String expectedKey = "artifacts/markdown-it/markdownit.zip";

        // call under test
        builder.buildMarkDownItLambda();

        verify(mockDownloader).downloadFile("https://sagebionetworks.jfrog.io/lambda/org/sagebase/markdownit/markdownit.zip");
        verify(mockS3Client).putObject(expectedBucket, expectedKey, mockFile);

        verify(mockFile).delete();

        verify(mockTemplate, times(1)).merge(velocityContextCaptor.capture(), any());
        VelocityContext context = velocityContextCaptor.getValue();
        assertEquals(expectedBucket, context.get("lambdaArtifactBucket"));
        assertEquals(expectedKey, context.get("lambdaArtifactKey"));

        ArgumentCaptor<CreateOrUpdateStackRequest> argCaptorCreateOrUpdateStack = ArgumentCaptor.forClass(CreateOrUpdateStackRequest.class);
        ArgumentCaptor<String> argCaptorWaitForStack = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> argCaptorDescribeStack = ArgumentCaptor.forClass(String.class);

        verify(mockCloudFormationClient, times(1)).createOrUpdateStack(argCaptorCreateOrUpdateStack.capture());
        verify(mockCloudFormationClient, times(1)).waitForStackToComplete(argCaptorWaitForStack.capture());
        verify(mockCloudFormationClient, times(1)).describeStack(argCaptorDescribeStack.capture());

        CreateOrUpdateStackRequest request = argCaptorCreateOrUpdateStack.getValue();
        assertEquals("dev-markdown-it-function", request.getStackName());
        assertTrue(request.getTags().isEmpty());
        assertEquals(1, request.getCapabilities().length);
        assertEquals(CAPABILITY_NAMED_IAM, request.getCapabilities()[0]);
        assertEquals("{}", request.getTemplateBody());

        assertEquals("dev-markdown-it-function", argCaptorWaitForStack.getValue());

        assertEquals("dev-markdown-it-function", argCaptorDescribeStack.getValue());

    }



}
