package org.sagebionetworks.template.markdownit;

import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
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
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.utils.ArtifactDownload;

import java.io.File;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.CAPABILITY_NAMED_IAM;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
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
    VelocityEngine velocityEngine;

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

        velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

        MarkDownItLambdaBuilder builder = new MarkDownItLambdaBuilderImpl(
                mockConfig,
                mockDownloader,
                mockCloudFormationClient,
                mockTagsProvider,
                mockS3Client,
                velocityEngine);


        when(mockDownloader.downloadFile(any())).thenReturn(mockFile);

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
        assertNotNull(request.getTemplateBody());

        JSONObject templateJson = new JSONObject(request.getTemplateBody());
        System.out.println(request.getTemplateBody());
        JSONObject resources = templateJson.getJSONObject("Resources");
        assertTrue(resources.has("mdlambdaServiceRole"));
        assertTrue(resources.has("mdlambda"));
        assertTrue(resources.has("mdlambdaFunctionUrl"));

        assertEquals("dev-markdown-it-function", argCaptorWaitForStack.getValue());
        assertEquals("dev-markdown-it-function", argCaptorDescribeStack.getValue());

    }

}
